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

package org.shanoir.ng.subject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.shared.exception.ShanoirException;
import org.shanoir.ng.shared.security.rights.StudyUserRight;
import org.shanoir.ng.study.model.Study;
import org.shanoir.ng.study.model.StudyUser;
import org.shanoir.ng.study.repository.StudyRepository;
import org.shanoir.ng.study.repository.StudyUserRepository;
import org.shanoir.ng.subject.controler.SubjectApi;
import org.shanoir.ng.subject.model.Subject;
import org.shanoir.ng.subject.repository.SubjectRepository;
import org.shanoir.ng.subjectstudy.model.SubjectStudy;
import org.shanoir.ng.subjectstudy.repository.SubjectStudyRepository;
import org.shanoir.ng.utils.ModelsUtil;
import static org.shanoir.ng.utils.assertion.AssertUtils.assertAccessAuthorized;
import static org.shanoir.ng.utils.assertion.AssertUtils.assertAccessDenied;
import org.shanoir.ng.utils.usermock.WithMockKeycloakUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BindingResultUtils;

/**
 * User security service test.
 * 
 * @author jlouis
 * 
 */

@SpringBootTest
@ActiveProfiles("test")
public class SubjectApiSecurityTest {

	private static final long LOGGED_USER_ID = 2L;
	private static final String LOGGED_USER_USERNAME = "logged";
	private static final long ENTITY_ID = 1L;
	
	private Subject mockNew;
	private Subject mockExisting;
	private BindingResult mockBindingResult;
	
	@Autowired
	private SubjectApi api;
	
	@MockBean
	private SubjectRepository repository;

	@MockBean
	private StudyUserRepository studyUserRepository;
	
	@MockBean
	private StudyRepository studyRepository;
	
	@MockBean
	private SubjectStudyRepository subjectStudyRepository;
	
	@BeforeEach
	public void setup() {
		mockNew = ModelsUtil.createSubject();
		mockExisting = ModelsUtil.createSubject();
		mockExisting.setId(ENTITY_ID);
		mockBindingResult = BindingResultUtils.getBindingResult(new HashMap<String, String>(), "Subject");
	}
	
	@Test
	@WithAnonymousUser
	public void testAsAnonymous() throws ShanoirException, RestServiceException {
		assertAccessDenied(api::deleteSubject, ENTITY_ID);
		assertAccessDenied(api::findSubjects, true, true);
		assertAccessDenied(api::findAllSubjectsNames);
		assertAccessDenied(api::findSubjectsNames, List.of(ENTITY_ID));
		assertAccessDenied(api::findSubjectById, ENTITY_ID);
		assertAccessDenied(api::saveNewSubject, mockNew, null, mockBindingResult);
		assertAccessDenied(api::updateSubject, ENTITY_ID, mockExisting, mockBindingResult);
		assertAccessDenied(api::findSubjectsByStudyId, ENTITY_ID, "null");
		assertAccessDenied(api::findSubjectByIdentifier, "identifier");
	}
	
	@Test
	@WithMockKeycloakUser(id = LOGGED_USER_ID, username = LOGGED_USER_USERNAME, authorities = { "ROLE_USER" })
	public void testAsUser() throws ShanoirException, RestServiceException {
		testRead();
		testCreate();
		assertAccessDenied(api::updateSubject, ENTITY_ID, mockExisting, mockBindingResult);

		assertAccessDenied(api::deleteSubject, ENTITY_ID);
		
		Subject mock = buildSubjectMock(ENTITY_ID);
		addStudyToMock(mock, 1L, StudyUserRight.CAN_IMPORT);
		given(repository.findById(ENTITY_ID)).willReturn(Optional.of(mock));
		assertAccessDenied(api::deleteSubject, ENTITY_ID);
		
		addStudyToMock(mock, 2L, StudyUserRight.CAN_ADMINISTRATE);
		given(repository.findById(ENTITY_ID)).willReturn(Optional.of(mock));
		assertAccessDenied(api::deleteSubject, ENTITY_ID);
		
		mock = buildSubjectMock(ENTITY_ID);
		addStudyToMock(mock, 1L, StudyUserRight.CAN_ADMINISTRATE);
		given(repository.findById(ENTITY_ID)).willReturn(Optional.of(mock));
		assertAccessDenied(api::deleteSubject, ENTITY_ID);
	}
	
	@Test
	@WithMockKeycloakUser(id = LOGGED_USER_ID, username = LOGGED_USER_USERNAME, authorities = { "ROLE_EXPERT" })
	public void testAsExpert() throws ShanoirException, RestServiceException {
		testRead();
		testCreate();
		
		//assertAccessDenied(api::updateSubject, ENTITY_ID, mockExisting, mockBindingResult);

		assertAccessDenied(api::deleteSubject, ENTITY_ID);
		
		Subject mock = buildSubjectMock(ENTITY_ID);
		addStudyToMock(mock, 1L, StudyUserRight.CAN_IMPORT);
		given(repository.findById(ENTITY_ID)).willReturn(Optional.of(mock));
		assertAccessDenied(api::deleteSubject, ENTITY_ID);
		
		addStudyToMock(mock, 2L, StudyUserRight.CAN_ADMINISTRATE);
		given(repository.findById(ENTITY_ID)).willReturn(Optional.of(mock));
		assertAccessDenied(api::deleteSubject, ENTITY_ID);
		
		mock = buildSubjectMock(ENTITY_ID);
		addStudyToMock(mock, 1L, StudyUserRight.CAN_ADMINISTRATE);
		given(repository.findById(ENTITY_ID)).willReturn(Optional.of(mock));
		assertAccessAuthorized(api::deleteSubject, ENTITY_ID);
	}

	@Test
	@WithMockKeycloakUser(id = LOGGED_USER_ID, username = LOGGED_USER_USERNAME, authorities = { "ROLE_ADMIN" })
	public void testAsAdmin() throws ShanoirException, RestServiceException {
		assertAccessAuthorized(api::deleteSubject, ENTITY_ID);
		assertAccessAuthorized(api::findSubjects, true, true);
		assertAccessAuthorized(api::findAllSubjectsNames);
		assertAccessAuthorized(api::findSubjectsNames, List.of(ENTITY_ID));
		assertAccessAuthorized(api::findSubjectById, ENTITY_ID);
		assertAccessAuthorized(api::saveNewSubject, mockNew, null, mockBindingResult);
		assertAccessAuthorized(api::updateSubject, ENTITY_ID, mockExisting, mockBindingResult);
		assertAccessAuthorized(api::findSubjectsByStudyId, ENTITY_ID, "null");
		assertAccessAuthorized(api::findSubjectByIdentifier, "identifier");
	}
	
	private void testRead() throws ShanoirException {
		final String NAME = "data";
		
		// No rights
		Subject subjectMockNoRights = buildSubjectMock(1L);
		given(repository.findByName(NAME)).willReturn(subjectMockNoRights);
		given(repository.findById(1L)).willReturn(Optional.of(subjectMockNoRights));
		given(repository.findSubjectWithSubjectStudyById(1L)).willReturn(subjectMockNoRights);
		given(repository.findSubjectFromCenterCode("centerCode%")).willReturn(subjectMockNoRights);
		given(repository.findAllById(Arrays.asList(1L))).willReturn(Arrays.asList(subjectMockNoRights));
		assertAccessDenied(api::findSubjectById, ENTITY_ID);
		
		given(repository.findAll()).willReturn(Arrays.asList(subjectMockNoRights));
		assertAccessAuthorized(api::findSubjects,true, true);
		assertEquals(null, api.findSubjects(true, true).getBody());
		assertAccessAuthorized(api::findAllSubjectsNames);
		assertAccessAuthorized(api::findSubjectsNames, List.of(ENTITY_ID));

		//assertNotNull(api.findSubjectsNames().getBody());
		SubjectStudy subjectStudyMock = new SubjectStudy();
		subjectStudyMock.setStudy(buildStudyMock(1L));
		subjectStudyMock.setSubject(subjectMockNoRights);
		given(subjectStudyRepository.findByStudyId(subjectStudyMock.getStudy().getId())).willReturn(Arrays.asList(subjectStudyMock));
		given(subjectStudyRepository.findByStudyIdAndStudy_StudyUserList_UserId(subjectStudyMock.getStudy().getId(), LOGGED_USER_ID)).willReturn(Arrays.asList(subjectStudyMock));
		given(studyRepository.findStudyWithTagsById(1L)).willReturn(buildStudyMock(1L));
		assertAccessDenied(api::findSubjectsByStudyId, 1L, "null");
		
		// Wrong Rights
		Subject subjectMockWrongRights = buildSubjectMock(1L);
		addStudyToMock(subjectMockWrongRights, 100L, StudyUserRight.CAN_ADMINISTRATE, StudyUserRight.CAN_DOWNLOAD, StudyUserRight.CAN_IMPORT);
		given(repository.findByName(NAME)).willReturn(subjectMockWrongRights);
		given(repository.findById(1L)).willReturn(Optional.of(subjectMockWrongRights));
		given(repository.findSubjectWithSubjectStudyById(1L)).willReturn(subjectMockWrongRights);
		given(repository.findSubjectFromCenterCode("centerCode%")).willReturn(subjectMockWrongRights);
		given(repository.findAll()).willReturn(Arrays.asList(subjectMockWrongRights));
		assertAccessDenied(api::findSubjectById, ENTITY_ID);
		
		given(repository.findAll()).willReturn(Arrays.asList(subjectMockWrongRights));
		assertAccessAuthorized(api::findSubjects, true, true);
		assertEquals(null, api.findSubjects(true, true).getBody());
		assertAccessAuthorized(api::findAllSubjectsNames);
		assertAccessAuthorized(api::findSubjectsNames, List.of(ENTITY_ID));
		//assertEquals(null, api.findSubjectsNames().getBody());
		subjectStudyMock = new SubjectStudy();
		subjectStudyMock.setStudy(buildStudyMock(1L));
		subjectStudyMock.setSubject(subjectMockWrongRights);
		given(subjectStudyRepository.findByStudyId(subjectStudyMock.getStudy().getId())).willReturn(Arrays.asList(subjectStudyMock));
		given(subjectStudyRepository.findByStudyIdAndStudy_StudyUserList_UserId(subjectStudyMock.getStudy().getId(), LOGGED_USER_ID)).willReturn(Arrays.asList(subjectStudyMock));
		assertAccessDenied(api::findSubjectsByStudyId, 1L, null);
		
		// Right rights (!)
		Subject subjectMockRightRights = buildSubjectMock(1L);
		addStudyToMock(subjectMockRightRights, 100L, StudyUserRight.CAN_SEE_ALL);
		given(repository.findByName(NAME)).willReturn(subjectMockRightRights);
		given(repository.findById(1L)).willReturn(Optional.of(subjectMockRightRights));
		given(repository.findFirstByIdentifierAndSubjectStudyListStudyIdIn("identifier", List.of(ENTITY_ID))).willReturn(subjectMockRightRights);
		given(repository.findSubjectWithSubjectStudyById(1L)).willReturn(subjectMockRightRights);
		given(repository.findSubjectFromCenterCode("centerCode%")).willReturn(subjectMockRightRights);
		assertAccessAuthorized(api::findSubjectById, ENTITY_ID);
		assertAccessAuthorized(api::findSubjectByIdentifier, "identifier");
		
		given(repository.findAll()).willReturn(Arrays.asList(subjectMockRightRights));
		given(repository.findAllById(Arrays.asList(1L))).willReturn(Arrays.asList(subjectMockRightRights));
		assertAccessAuthorized(api::findSubjects, true, true);
		assertEquals(1, api.findSubjects(true, true).getBody().size());
		assertAccessAuthorized(api::findAllSubjectsNames);
		assertAccessAuthorized(api::findSubjectsNames, List.of(ENTITY_ID));
		//assertEquals(1, api.findSubjectsNames().getBody().size());
		subjectStudyMock = new SubjectStudy();
		subjectStudyMock.setStudy(buildStudyMock(1L));
		subjectStudyMock.setSubject(subjectMockRightRights);
		given(subjectStudyRepository.findByStudyId(subjectStudyMock.getStudy().getId())).willReturn(Arrays.asList(subjectStudyMock));
		given(subjectStudyRepository.findByStudyIdAndStudy_StudyUserList_UserId(subjectStudyMock.getStudy().getId(), LOGGED_USER_ID)).willReturn(Arrays.asList(subjectStudyMock));
		given(studyRepository.findById(1L)).willReturn(Optional.of(subjectStudyMock.getStudy()));
		assertAccessAuthorized(api::findSubjectsByStudyId, 1L, null);
		assertNotNull(api.findSubjectsByStudyId(1L,null).getBody());
		assertEquals(1, api.findSubjectsByStudyId(1L, null).getBody().size());
	}

	private void testCreate() throws ShanoirException {
		List<Study> studiesMock;
		
		// Create subject without subject <-> study
		Subject newSubjectMock = buildSubjectMock(null);
		assertAccessDenied(api::saveNewSubject, newSubjectMock, null, mockBindingResult);
		
		// Create subject
		studiesMock = new ArrayList<>();
		studiesMock.add(buildStudyMock(9L));
		given(studyRepository.findAllById(Arrays.asList(9L))).willReturn(studiesMock);
		given(studyRepository.findById(9L)).willReturn(Optional.of(buildStudyMock(9L)));
		newSubjectMock = buildSubjectMock(null);
		addStudyToMock(newSubjectMock, 9L);
		assertAccessDenied(api::saveNewSubject, newSubjectMock, null, mockBindingResult);
		
		// Create subject linked to a study where I can admin, download, see all but not import.
		studiesMock = new ArrayList<>();
		studiesMock.add(buildStudyMock(10L, StudyUserRight.CAN_ADMINISTRATE, StudyUserRight.CAN_DOWNLOAD, StudyUserRight.CAN_SEE_ALL));
		given(studyRepository.findAllById(Arrays.asList(10L))).willReturn(studiesMock);
		given(studyRepository.findById(10L)).willReturn(Optional.of(buildStudyMock(10L, StudyUserRight.CAN_ADMINISTRATE, StudyUserRight.CAN_DOWNLOAD, StudyUserRight.CAN_SEE_ALL)));
		newSubjectMock = buildSubjectMock(null);
		addStudyToMock(newSubjectMock, 10L);
		assertAccessDenied(api::saveNewSubject, newSubjectMock, null, mockBindingResult);
		
		// Create subject linked to a study where I can import and also to a study where I can't.
		studiesMock = new ArrayList<>();
		studiesMock.add(buildStudyMock(11L, StudyUserRight.CAN_ADMINISTRATE, StudyUserRight.CAN_DOWNLOAD, StudyUserRight.CAN_SEE_ALL));
		studiesMock.add(buildStudyMock(12L, StudyUserRight.CAN_IMPORT));
		given(studyRepository.findAllById(Arrays.asList(new Long[] { 12L, 11L }))).willReturn(studiesMock);
		given(studyRepository.findAllById(Arrays.asList(new Long[] { 11L, 12L }))).willReturn(studiesMock);
		given(studyRepository.findById(11L)).willReturn(Optional.of(buildStudyMock(11L, StudyUserRight.CAN_ADMINISTRATE, StudyUserRight.CAN_DOWNLOAD, StudyUserRight.CAN_SEE_ALL)));
		given(studyRepository.findById(12L)).willReturn(Optional.of(buildStudyMock(12L, StudyUserRight.CAN_IMPORT)));
		newSubjectMock = buildSubjectMock(null);
		addStudyToMock(newSubjectMock, 11L);
		addStudyToMock(newSubjectMock, 12L);
		assertAccessDenied(api::saveNewSubject, newSubjectMock, null, mockBindingResult);
		
		// Create subject linked to a study where I can import
		studiesMock = new ArrayList<>();
		studiesMock.add(buildStudyMock(13L, StudyUserRight.CAN_IMPORT));
		given(studyRepository.findAllById(Arrays.asList(new Long[] { 13L }))).willReturn(studiesMock);
		given(studyRepository.findById(13L)).willReturn(Optional.of(buildStudyMock(13L, StudyUserRight.CAN_IMPORT)));
		given(studyUserRepository.findByStudy_Id(13L)).willReturn(buildStudyMock(13L, StudyUserRight.CAN_IMPORT).getStudyUserList());
		newSubjectMock = buildSubjectMock(null);
		addStudyToMock(newSubjectMock, 13L);
		assertAccessAuthorized(api::saveNewSubject, newSubjectMock, null, mockBindingResult);
	}
	
	private Study buildStudyMock(Long id, StudyUserRight... rights) {
		Study study = ModelsUtil.createStudy();
		study.setId(id);
		List<StudyUser> studyUserList = new ArrayList<>();
		for (StudyUserRight right : rights) {
			StudyUser studyUser = new StudyUser();
			studyUser.setUserId(LOGGED_USER_ID);
			studyUser.setUserName(LOGGED_USER_USERNAME);
			studyUser.setStudy(study);
			studyUser.setStudyUserRights(Arrays.asList(right));
			studyUserList.add(studyUser);
		}
		study.setStudyUserList(studyUserList);
		return study;
	}
	
	private Subject buildSubjectMock(Long id) {
		Subject subject = ModelsUtil.createSubject();
		subject.setId(id);
		return subject;
	}
	
	private void addStudyToMock(Subject mock, Long id, StudyUserRight... rights) {
		Study study = buildStudyMock(id, rights);
		
		SubjectStudy subjectStudy = new SubjectStudy();
		subjectStudy.setSubject(mock);
		subjectStudy.setStudy(study);
		
		if (study.getSubjectStudyList() == null) {
			study.setSubjectStudyList(new ArrayList<SubjectStudy>());
		}
		if (mock.getSubjectStudyList() == null) {
			mock.setSubjectStudyList(new ArrayList<SubjectStudy>());
		}
		study.getSubjectStudyList().add(subjectStudy);
		mock.getSubjectStudyList().add(subjectStudy);
	}

}
