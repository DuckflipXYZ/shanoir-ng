/**
 * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */

package org.shanoir.ng.importer.strategies.dataset;

import java.util.ArrayList;
import java.util.List;

import org.dcm4che3.data.Attributes;
import org.shanoir.ng.dataset.modality.MrDataset;
import org.shanoir.ng.dataset.model.CardinalityOfRelatedSubjects;
import org.shanoir.ng.dataset.model.DatasetExpression;
import org.shanoir.ng.dataset.model.DatasetMetadata;
import org.shanoir.ng.dataset.model.DatasetModalityType;
import org.shanoir.ng.dataset.model.ProcessedDatasetType;
import org.shanoir.ng.dicom.DicomProcessing;
import org.shanoir.ng.importer.dto.Dataset;
import org.shanoir.ng.importer.dto.DatasetsWrapper;
import org.shanoir.ng.importer.dto.EchoTime;
import org.shanoir.ng.importer.dto.ExpressionFormat;
import org.shanoir.ng.importer.dto.ImportJob;
import org.shanoir.ng.importer.dto.Serie;
import org.shanoir.ng.importer.strategies.datasetexpression.DatasetExpressionContext;
import org.shanoir.ng.shared.mapper.EchoTimeMapper;
import org.shanoir.ng.shared.mapper.FlipAngleMapper;
import org.shanoir.ng.shared.mapper.InversionTimeMapper;
import org.shanoir.ng.shared.mapper.RepetitionTimeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MrDatasetStrategy implements DatasetStrategy<MrDataset> {

	@Autowired
	DicomProcessing dicomProcessing;
	
	@Autowired
	DatasetExpressionContext datasetExpressionContext;
	
	@Autowired
	private EchoTimeMapper echoTimeMapper;
	
	@Autowired
	private RepetitionTimeMapper repetitionTimeMapper;

	@Autowired
	private InversionTimeMapper inversionTimeMapper;
	
	@Autowired
	private FlipAngleMapper flipAngleMapper;
	
	@Override
	public DatasetsWrapper<MrDataset> generateDatasetsForSerie(Attributes dicomAttributes, Serie serie,
			ImportJob importJob) {
		
		DatasetsWrapper<MrDataset> datasetWrapper = new DatasetsWrapper<>();
		/**
		 * retrieve number of dataset in current serie if Number of dataset > 1 then
		 * each dataset will be named with an int at the end of the name. else the is
		 * only one dataset => no need for extension.
		 */
		int datasetIndex;
		if (serie.getDatasets().size() > 1) {
			datasetIndex = 1;
		} else {
			datasetIndex = -1;
		}

		// TODO ATO : implement MrDAtasetAcquisitionHome.createMrDataset (issue by
		// createMrDatasetAcquisitionFromDicom()
		for (Dataset dataset : serie.getDatasets()) {
			// TODO ATO : implement line 350 - 372 MrDAtasetAcquisitionHome.createMrDataset
			MrDataset mrDataset = new MrDataset();
			mrDataset = generateSingleDataset(dicomAttributes, serie, dataset, datasetIndex, importJob);
			if (mrDataset.getFirstImageAcquisitionTime() != null) {
				if (datasetWrapper.getFirstImageAcquisitionTime() == null) {
					datasetWrapper.setFirstImageAcquisitionTime(mrDataset.getFirstImageAcquisitionTime());
				} else {
					if (datasetWrapper.getFirstImageAcquisitionTime().isAfter(mrDataset.getFirstImageAcquisitionTime())) {
						datasetWrapper.setFirstImageAcquisitionTime(mrDataset.getFirstImageAcquisitionTime());
					}
				}
			}
			if (mrDataset.getLastImageAcquisitionTime() != null) {
				if (datasetWrapper.getLastImageAcquisitionTime() == null) {
					datasetWrapper.setLastImageAcquisitionTime(mrDataset.getLastImageAcquisitionTime());
				} else {
					if (datasetWrapper.getLastImageAcquisitionTime().isAfter(mrDataset.getLastImageAcquisitionTime())) {
						datasetWrapper.setLastImageAcquisitionTime(mrDataset.getLastImageAcquisitionTime());
					}
				}
			}
			datasetWrapper.getDatasets().add(mrDataset);
			datasetIndex++;
		}

		return datasetWrapper;

	}

	/* (non-Javadoc)
	 * @see org.shanoir.ng.dataset.modality.DatasetStrategy#generateSingleMrDataset(org.dcm4che3.data.Attributes, org.shanoir.ng.importer.dto.Serie, org.shanoir.ng.importer.dto.Dataset, int, org.shanoir.ng.importer.dto.ImportJob)
	 */
	@Override
	public MrDataset generateSingleDataset(Attributes dicomAttributes, Serie serie, Dataset dataset, int datasetIndex,
			ImportJob importJob) {
		MrDataset mrDataset = new MrDataset();
		mrDataset.setCreationDate(serie.getSeriesDate());
		mrDataset.setDiffusionGradients(dataset.getDiffusionGradients());
		final String serieDescription = serie.getSeriesDescription();

		DatasetMetadata datasetMetadata = new DatasetMetadata();
		mrDataset.setOriginMetadata(datasetMetadata);
		// set the series description as the dataset comment & name
		if (serieDescription != null && !"".equals(serieDescription)) {
			mrDataset.getOriginMetadata().setName(computeDatasetName(serieDescription, datasetIndex));
			mrDataset.getOriginMetadata().setComment(serieDescription);
		}

		// Pre-select the type Reconstructed dataset
		mrDataset.getOriginMetadata().setProcessedDatasetType(ProcessedDatasetType.RECONSTRUCTEDDATASET);

		// Set the study and the subject
		mrDataset.setSubjectId(importJob.getPatients().get(0).getSubject().getId());
		mrDataset.setStudyId(importJob.getFrontStudyId());

		// Set the modality from dicom fields
		// TODO  :VERIFY NOT NEEDED ANY MORE ?
		 mrDataset.getOriginMetadata().setDatasetModalityType(DatasetModalityType.MR_DATASET);

		CardinalityOfRelatedSubjects refCardinalityOfRelatedSubjects = null;
		if (mrDataset.getSubjectId() != null) {
			refCardinalityOfRelatedSubjects = CardinalityOfRelatedSubjects.SINGLE_SUBJECT_DATASET;
		} else {
			refCardinalityOfRelatedSubjects = CardinalityOfRelatedSubjects.MULTIPLE_SUBJECTS_DATASET;
		}
		mrDataset.getOriginMetadata().setCardinalityOfRelatedSubjects(refCardinalityOfRelatedSubjects);
		
		if (dataset.getEchoTimes() != null) {
			List<EchoTime> listEchoTime = new ArrayList<>(dataset.getEchoTimes());
			mrDataset.getEchoTime().addAll(echoTimeMapper.EchoTimeDTOListToEchoTimeList(listEchoTime));
			for (org.shanoir.ng.shared.model.EchoTime et: mrDataset.getEchoTime()) {
				et.setMrDataset(mrDataset);
			}
		}
		
		if (dataset.getRepetitionTimes() != null) {
			List<Double> listRepetitionTime = new ArrayList<>(dataset.getRepetitionTimes());
			mrDataset.getRepetitionTime().addAll(repetitionTimeMapper.RepetitionTimeDTOListToRepetitionTimeList(listRepetitionTime));
			for ( org.shanoir.ng.shared.model.RepetitionTime rt: mrDataset.getRepetitionTime()) {
				rt.setMrDataset(mrDataset);
			}
		}
		
		if (dataset.getInversionTimes() != null) {
			List<Double> listInversionTime = new ArrayList<>(dataset.getInversionTimes());
			mrDataset.getInversionTime().addAll(inversionTimeMapper.InversionTimeDTOListToInversionTimeList(listInversionTime));
			for ( org.shanoir.ng.shared.model.InversionTime rt: mrDataset.getInversionTime()) {
				rt.setMrDataset(mrDataset);
			}
		}
		
		if (dataset.getFlipAngles() != null) {
			List<String> listFlipAngle = new ArrayList<>(dataset.getFlipAngles());
			mrDataset.getFlipAngle().addAll(flipAngleMapper.FlipAngleDTOListToFlipAngleList(listFlipAngle));
			for ( org.shanoir.ng.shared.model.FlipAngle rt: mrDataset.getFlipAngle()) {
				rt.setMrDataset(mrDataset);
			}
		}

		/**
		 *  The part below will generate automatically the datasetExpression according to :
		 *   -  type found in the importJob.serie.datasets.dataset.expressionFormat.type
		 * 
		 *  The DatasetExpressionFactory will return the proper object according to the expression format type and add it to the current mrDataset
		 * 
		 **/
		for (ExpressionFormat expressionFormat : dataset.getExpressionFormats()) {
			datasetExpressionContext.setDatasetExpressionStrategy(expressionFormat.getType());
			DatasetExpression datasetExpression = datasetExpressionContext.generateDatasetExpression(serie, importJob, expressionFormat);
			if (datasetExpression.getFirstImageAcquisitionTime() != null) {
				if (mrDataset.getFirstImageAcquisitionTime() == null) {
					mrDataset.setFirstImageAcquisitionTime(datasetExpression.getFirstImageAcquisitionTime());
				} else {
					if (mrDataset.getFirstImageAcquisitionTime().isAfter(datasetExpression.getFirstImageAcquisitionTime())) {
						mrDataset.setFirstImageAcquisitionTime(datasetExpression.getFirstImageAcquisitionTime());
					}
				}
			}
			if (datasetExpression.getLastImageAcquisitionTime() != null) {
				if (mrDataset.getLastImageAcquisitionTime() == null) {
					mrDataset.setLastImageAcquisitionTime(datasetExpression.getLastImageAcquisitionTime());
				} else {
					if (mrDataset.getLastImageAcquisitionTime().isAfter(datasetExpression.getLastImageAcquisitionTime())) {
						mrDataset.setLastImageAcquisitionTime(datasetExpression.getLastImageAcquisitionTime());
					}
				}
			}
			datasetExpression.setDataset(mrDataset);
			mrDataset.getDatasetExpressions().add(datasetExpression);
		}
		return mrDataset;
	}


	/* (non-Javadoc)
	 * @see org.shanoir.ng.dataset.modality.DatasetStrategy#computeDatasetName(java.lang.String, int)
	 */
	@Override
	public String computeDatasetName(String name, int index) {
		if (index == -1) {
			return name;
		} else {
			return name + " " + index;
		}
	}

}
