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

/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.shanoir.ng.solr.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.shanoir.ng.solr.model.ShanoirMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yyao
 * @author mkain
 *
 */
@Component
@SuppressWarnings("unchecked")
public class ShanoirMetadataRepositoryImpl implements ShanoirMetadataRepositoryCustom {

	private static final Logger LOG = LoggerFactory.getLogger(ShanoirMetadataRepositoryImpl.class);
	public static final String MR_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"mdm.mr_dataset_nature as datasetNature, " +
			"d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName, " +
			"su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, st.name as studyName, " +
			"e.study_id as studyId, " +
			"c.name as centerName, " +
			"c.id as centerId, mrp.slice_thickness as sliceThickness, " +
			"mrp.pixel_bandwidth as pixelBandwidth, " +
			"mrp.magnetic_field_strength as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset_acquisition da on da.id = d.dataset_acquisition_id"
			+ " LEFT JOIN mr_dataset_acquisition mda on mda.id = d.dataset_acquisition_id"
			+ " LEFT JOIN mr_protocol mrp on mrp.id = mda.mr_protocol_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, dataset_metadata dm, mr_dataset md"
			+ " LEFT JOIN mr_dataset_metadata mdm ON md.updated_mr_metadata_id = mdm.id"
			+ " WHERE d.updated_metadata_id = dm.id AND md.id = d.id";
	public static final String PET_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"null as datasetNature, " +
			"d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName, " +
			"su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, " +
			"st.name as studyName, e.study_id as studyId, c.name as centerName, " +
			"c.id as centerId, null as sliceThickness, " +
			"null as pixelBandwidth, " +
			"null as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset_acquisition da on da.id = d.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, pet_dataset pd, dataset_metadata dm"
			+ " WHERE d.updated_metadata_id = dm.id AND pd.id = d.id";
	public static final String CT_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"null as datasetNature, " +
			"d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName, " +
			"su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, st.name as studyName, " +
			"e.study_id as studyId, " +
			"c.name as centerName, c.id as centerId, " +
			"null as sliceThickness, " +
			"null as pixelBandwidth, " +
			"null as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset_acquisition da on da.id = d.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, ct_dataset cd, dataset_metadata dm"
			+ " WHERE d.updated_metadata_id = dm.id AND cd.id = d.id";
	public static final String GENERIC_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"null as datasetNature, " +
			"d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName, " +
			"su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, " +
			"st.name as studyName, " +
			"e.study_id as studyId, " +
			"c.name as centerName, " +
			"c.id as centerId, " +
			"null as sliceThickness, " +
			"null as pixelBandwidth, " +
			"null as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset_acquisition da on da.id = d.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, generic_dataset cd, dataset_metadata dm"
			+ " WHERE d.updated_metadata_id = dm.id AND cd.id = d.id";
	public static final String EEG_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"null as datasetNature, " +
			"d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName, " +
			"su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, st.name as studyName, " +
			"e.study_id as studyId, " +
			"c.name as centerName, c.id as centerId, " +
			"null as sliceThickness, " +
			"null as pixelBandwidth, " +
			"null as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset_acquisition da on da.id = d.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, eeg_dataset ed, dataset_metadata dm"
			+ " WHERE d.origin_metadata_id = dm.id AND ed.id = d.id";
	public static final String BIDS_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"null as datasetNature, " +
			"d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName, " +
			"su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, " +
			"st.name as studyName, " +
			"e.study_id as studyId, " +
			"c.name as centerName, " +
			"c.id as centerId, " +
			"null as sliceThickness, " +
			"null as pixelBandwidth, " +
			"null as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset_acquisition da on da.id = d.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, bids_dataset ed, dataset_metadata dm"
			+ " WHERE d.updated_metadata_id = dm.id AND ed.id = d.id";
	public static final String PROCESSED_QUERY = "SELECT d.id as datasetId"
			+ ", dm.name as datasetName"
			+ ", dm.dataset_modality_type as datasetType"
			+ ", null as datasetNature"
			+ ", d.creation_date as datasetCreationDate"
			+ ", e.id as examinationId"
			+ ", e.comment as examinationComment"
			+ ", e.examination_date as examinationDate"
			+ ", ae.name as acquisitionEquipmentName"
			+ ", su.name as subjectName"
			+ ", sust.subject_type as subjectType"
			+ ", su.id as subjectId"
			+ ", st.name as studyName"
			+ ", e.study_id as studyId"
			+ ", c.name as centerName"
			+ ", c.id as centerId"
			+ ", null as sliceThickness"
			+ ", null as pixelBandwidth"
			+ ", null as magneticFieldStrength"
			+ ", da.import_date as importDate"
			+ ", da.username as username"
			+ ", 1 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset dp ON dp.id ="
			+ " (SELECT dataset_id from input_of_dataset_processing WHERE processing_id = d.dataset_processing_id LIMIT 1)"
			+ " LEFT JOIN dataset_acquisition da on da.id = dp.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, dataset_metadata dm"
			+ " WHERE d.origin_metadata_id = dm.id"
			+ " AND d.dataset_processing_id is not null";
	public static final String MEASUREMENT_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"null as datasetNature, "
			+ "d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName,"
			+ "su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, " +
			"st.name as studyName, " +
			"e.study_id as studyId, " +
			"c.name as centerName, " +
			"c.id as centerId, "
			+ "null as sliceThickness, " +
			"null as pixelBandwidth, " +
			"null as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset refd ON refd.id = d.referenced_dataset_for_superimposition_id"
			+ " LEFT JOIN dataset_acquisition da on da.id = refd.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, measurement_dataset md, dataset_metadata dm"
			+ " WHERE d.updated_metadata_id = dm.id AND md.id = d.id";
	public static final String XA_QUERY = "SELECT d.id as datasetId, " +
			"dm.name as datasetName, " +
			"dm.dataset_modality_type as datasetType, " +
			"null as datasetNature, " +
			"d.creation_date as datasetCreationDate, " +
			"e.id as examinationId, " +
			"e.comment as examinationComment, " +
			"e.examination_date as examinationDate, " +
			"ae.name as acquisitionEquipmentName, " +
			"su.name as subjectName, " +
			"sust.subject_type as subjectType, " +
			"su.id as subjectId, " +
			"st.name as studyName, " +
			"e.study_id as studyId, " +
			"c.name as centerName, " +
			"c.id as centerId, " +
			"null as sliceThickness, " +
			"null as pixelBandwidth, " +
			"null as magneticFieldStrength, " +
			"da.import_date as importDate, " +
			"da.username as username, " +
			"0 as processed"
			+ " FROM dataset d"
			+ " LEFT JOIN dataset_acquisition da on da.id = d.dataset_acquisition_id"
			+ " LEFT JOIN examination e ON e.id = da.examination_id"
			+ " LEFT JOIN acquisition_equipment ae ON ae.id = da.acquisition_equipment_id"
			+ " LEFT JOIN study st ON st.id = e.study_id"
			+ " LEFT JOIN subject_study sust ON sust.subject_id = d.subject_id AND sust.study_id = e.study_id"
			+ " LEFT JOIN center c ON c.id = e.center_id"
			+ " LEFT JOIN subject su ON su.id = d.subject_id, xa_dataset cd, dataset_metadata dm"
			+ " WHERE d.updated_metadata_id = dm.id AND cd.id = d.id";
	public static final String RESULTSET_MAPPING = "SolrResult";

	public static final String SUBJECT_TAG_QUERY = "SELECT d.id AS dataset_id, tag.name AS tag" +
			" FROM dataset d" +
			" INNER JOIN subject_study substu ON d.subject_id = substu.subject_id" +
			" INNER JOIN subject_study_tag substutag ON substu.id = substutag.subject_study_id" +
			" INNER JOIN tag ON substutag.tags_id = tag.id";

	public static final String STUDY_TAG_QUERY = "SELECT d.id AS dataset_id, tag.name AS tag" +
			" FROM dataset d " +
			" INNER JOIN dataset_tag dstag ON d.id = dstag.dataset_id " +
			" INNER JOIN study_tag tag ON dstag.study_tag_id = tag.id";

	@PersistenceContext
	private EntityManager em;
	
	@Override
	public List<ShanoirMetadata> findAllAsSolrDoc() {
		return this.findSolr("");
	}

	@Override
	public ShanoirMetadata findOneSolrDoc(Long datasetId) {

		String clause = " AND d.id = " + datasetId;

		List<ShanoirMetadata> processedResult = this.findSolr(clause);
		if (!processedResult.isEmpty()) {
			return processedResult.get(0);
		}

		List<ShanoirMetadata> result = this.findSolr(clause);

		if(result.isEmpty()){
			return null;
		}
		
		if (result.size() > 1) {
			LOG.error("Solr query returned multiple result for dataset [{}]. Please check database consistency.", datasetId);
			return null;
		}
		
		return result.get(0);
	}

	@Override
	public List<ShanoirMetadata> findSolrDocs(List<Long> datasetIds) {

		if (CollectionUtils.isEmpty(datasetIds)) {
			return Collections.emptyList();
		}

		String ids = datasetIds.stream().map(Object::toString).collect(Collectors.joining(","));
		String clause = " AND d.id IN (" + ids + ")";

		return this.findSolr(clause);
	}

	private List<ShanoirMetadata> findSolr(String clause){

		List<ShanoirMetadata> result = new ArrayList<>();

		Query mrQuery = em.createNativeQuery(MR_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(mrQuery.getResultList());

		Query petQuery = em.createNativeQuery(PET_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(petQuery.getResultList());

		Query ctQuery = em.createNativeQuery(CT_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(ctQuery.getResultList());

		Query xaQuery = em.createNativeQuery(XA_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(xaQuery.getResultList());

		Query genericQuery = em.createNativeQuery(GENERIC_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(genericQuery.getResultList());

		Query eegQuery = em.createNativeQuery(EEG_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(eegQuery.getResultList());

		Query bidsQuery = em.createNativeQuery(BIDS_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(bidsQuery.getResultList());

		result.addAll(this.findSolrProcessed(clause));

		Query measurementQuery = em.createNativeQuery(MEASUREMENT_QUERY + clause, RESULTSET_MAPPING);
		result.addAll(measurementQuery.getResultList());

		return result;
	}

	private List<ShanoirMetadata> findSolrProcessed(String clause){
		Query processedQuery = em.createNativeQuery(PROCESSED_QUERY + clause, RESULTSET_MAPPING);
		return processedQuery.getResultList();
	}

	@Override
	public Map<Long, List<String>> findAllTags(List<Long> datasetIds){

		List<Object[]> result = new ArrayList<>();

		String clause = "";

		if(datasetIds != null && !datasetIds.isEmpty()){
			String ids = datasetIds.stream().map(Object::toString).collect(Collectors.joining(","));
			clause = " AND d.id IN (" + ids + ")";
		}

		Query subjectTagQuery = em.createNativeQuery(SUBJECT_TAG_QUERY + clause);
		result.addAll(subjectTagQuery.getResultList());

		Query studyTagQuery = em.createNativeQuery(STUDY_TAG_QUERY + clause);
		result.addAll(studyTagQuery.getResultList());

		Map<Long, List<String>> tags = new HashMap<>();

		for(Object[] row : result){
			Long id = (Long) row[0];
			tags.putIfAbsent(id, new ArrayList<>());
			tags.get(id).add((String) row[1]);
		}

		return tags;

	}
}