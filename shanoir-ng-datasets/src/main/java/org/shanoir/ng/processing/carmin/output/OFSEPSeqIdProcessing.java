package org.shanoir.ng.processing.carmin.output;

import org.apache.commons.io.IOUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.shanoir.ng.dataset.model.Dataset;
import org.shanoir.ng.dataset.model.DatasetMetadata;
import org.shanoir.ng.dataset.service.DatasetService;
import org.shanoir.ng.datasetacquisition.model.DatasetAcquisition;
import org.shanoir.ng.datasetacquisition.model.mr.MrDatasetAcquisition;
import org.shanoir.ng.datasetacquisition.model.mr.MrProtocolSCMetadata;
import org.shanoir.ng.datasetacquisition.service.DatasetAcquisitionService;
import org.shanoir.ng.download.WADODownloaderService;
import org.shanoir.ng.processing.carmin.model.CarminDatasetProcessing;
import org.shanoir.ng.property.model.DatasetProperty;
import org.shanoir.ng.property.service.DatasetPropertyService;
import org.shanoir.ng.shared.exception.EntityNotFoundException;
import org.shanoir.ng.shared.exception.PacsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OFSEPSeqIdProcessing extends OutputProcessing {

    private static final Logger LOG = LoggerFactory.getLogger(OFSEPSeqIdProcessing.class);

    public static final String PIPELINE_OUTPUT = "output.json";

    private static final String[] SERIE_PROPERTIES = {
            "coil",
            "type",
            "protocolValidityStatus",
            "name",
            "numberOfDirections"
    };

    private static final String[] VOLUME_PROPERTIES = {
            "acquisitionDate",
            "contrastAgent",
            "contrastAgentAlgo",
            "contrastAgentAlgoConfidence",
            "contrastAgentDICOM",
            "organ",
            "organAlgo",
            "organAlgoConfidence",
            "organDICOM",
            "type",
            "sequence",
            "extraType",
            "derivedSequence",
            "name",
            "contrast",
            "bValue",
            "sliceThickness",
            "spacings",
            "spacingBetweenSlices",
            "numberOfSlices",
            "dimension",
            "dimensions",
            "axis"

    };

    @Autowired
    private DatasetService datasetService;
    @Autowired
    private DatasetAcquisitionService datasetAcquisitionService;

    @Autowired
    private WADODownloaderService wadoDownloaderService;

    @Autowired
    private DatasetPropertyService datasetPropertyService;



    @Override
    public boolean canProcess(CarminDatasetProcessing processing) {
        return processing.getPipelineIdentifier().startsWith("ofsep_sequences_identification");
    }

    @Override
    public void manageTarGzResult(List<File> resultFiles, File parentFolder, CarminDatasetProcessing processing) throws OutputProcessingException {

        for(File file : resultFiles){
            if (file.getAbsolutePath().endsWith(PIPELINE_OUTPUT)) {

                if (file.length() == 0) {
                    LOG.warn("Result file [{}] is present but empty.", file.getAbsolutePath());
                    return;
                }

                try (InputStream is = new FileInputStream(file)) {
                    JSONObject json = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                    this.processSeries(json.getJSONArray("series"), processing);
                }catch (Exception e) {
                    throw new OutputProcessingException("An error occured while extracting result from result archive.", e);
                }

                return;
            }
        }

    }

    private void processSeries(JSONArray series, CarminDatasetProcessing processing) throws JSONException, PacsException, EntityNotFoundException {

        for (int i = 0; i < series.length(); i++) {

            JSONObject serie = series.getJSONObject(i);
            Long serieId = serie.getLong("id");

            List<Dataset> datasets = processing.getInputDatasets().stream()
                    .filter(ds -> ds.getDatasetAcquisition() != null
                            && ds.getDatasetAcquisition().getId().equals(serieId))
                    .collect(Collectors.toList());

            if(datasets.isEmpty()){
                LOG.error("No dataset found for serie/acquisition [" + serieId + "]");
                continue;
            }

            for(Dataset ds : datasets){

                JSONObject vol;
                vol = this.getMatchingVolume(ds, serie);

                if(vol == null){
                    LOG.error("No volume from serie [{}] could be match with dataset [{}].", serieId, ds.getId());
                    continue;
                }

                try {
                    this.updateDataset(serie, ds, vol);
                } catch (EntityNotFoundException e) {
                    LOG.error("Error while updating dataset [{}]", ds.getId(), e);
                    throw e;
                }

                List<DatasetProperty> properties = this.getDatasetPropertiesFromVolume(ds, vol, processing);
                datasetPropertyService.create(properties);

            }
        }
    }

    /**
     * Update dataset from pipeline output
     *
     * @param serie
     * @param ds
     * @param vol
     * @throws JSONException
     * @throws EntityNotFoundException
     */
    @Transactional
    public void updateDataset(JSONObject serie, Dataset ds, JSONObject vol) throws JSONException, EntityNotFoundException {

        if(ds.getDatasetAcquisition() instanceof MrDatasetAcquisition){
            MrDatasetAcquisition acq = (MrDatasetAcquisition) ds.getDatasetAcquisition();

            MrProtocolSCMetadata metadata = acq.getMrProtocol().getUpdatedMetadata() != null
                    ? acq.getMrProtocol().getUpdatedMetadata()
                    : new MrProtocolSCMetadata();

            metadata.setMrSequenceName(serie.getString("type"));
            acq.getMrProtocol().setUpdatedMetadata(metadata);
            datasetAcquisitionService.update(acq);
        }

        if(ds.getUpdatedMetadata() == null){
            ds.setUpdatedMetadata(ds.getOriginMetadata() != null ? ds.getOriginMetadata() : new DatasetMetadata());
        }
        ds.getUpdatedMetadata().setName(vol.getString("type"));
        datasetService.update(ds);
    }


    /**
     * Create dataset properties from pipeline output
     *
     * @param ds
     * @param volume
     * @return
     */
    private List<DatasetProperty> getDatasetPropertiesFromVolume(Dataset ds, JSONObject volume, CarminDatasetProcessing processing) throws JSONException {
        List<DatasetProperty> properties = new ArrayList<>();

        for(String name : SERIE_PROPERTIES){

            if(!volume.has(name)){
                continue;
            }

            DatasetProperty property = new DatasetProperty();
            property.setDataset(ds);
            property.setName("serie." + name);
            property.setValue(volume.getString(name));
            property.setProcessing(processing);
            properties.add(property);
        }

        for(String name : VOLUME_PROPERTIES){

            if(!volume.has(name)){
                continue;
            }

            DatasetProperty property = new DatasetProperty();
            property.setDataset(ds);
            property.setName("volume." + name);
            property.setValue(volume.getString(name));
            property.setProcessing(processing);
            properties.add(property);
        }

        return properties;

    }


    /**
     * Get JSON volume matching Shanoir dataset
     * Match is made by orientation DICOM property
     *
     * @param dataset
     * @return
     * @throws JSONException
     */
    public JSONObject getMatchingVolume(Dataset dataset, JSONObject serie) throws JSONException, PacsException {

        JSONArray volumes = serie.getJSONArray("volumes");

        Attributes attributes = wadoDownloaderService.getDicomAttributesForDataset(dataset);

        double[] dsOrientation = attributes.getDoubles(Tag.ImageOrientationPatient);

        for (int i = 0 ; i < volumes.length(); i++) {

            JSONObject volume = volumes.getJSONObject(i);
            JSONArray volOrientation = volume.getJSONArray("orientation");

            if(this.areOrientationsEquals(dsOrientation, volOrientation)){
                return volume;
            }

        }

        return null;

    }

    public boolean areOrientationsEquals(double[] dsOrientation, JSONArray volOrientation) throws JSONException {

        if(dsOrientation.length != volOrientation.length()){
            return false;
        }

        for (int i = 0 ; i < dsOrientation.length; i++) {
            if(dsOrientation[i] != volOrientation.getDouble(i)){
                return false;
            }
        }

        return true;

    }

}
