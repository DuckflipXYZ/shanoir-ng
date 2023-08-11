package org.shanoir.uploader.action;

import java.io.File;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.shanoir.ng.exchange.imports.subject.IdentifierCalculator;
import org.shanoir.ng.importer.model.Patient;
import org.shanoir.ng.importer.model.Study;
import org.shanoir.uploader.ShUpOnloadConfig;
import org.shanoir.uploader.dicom.DicomTreeNode;
import org.shanoir.uploader.dicom.IDicomServerClient;
import org.shanoir.uploader.dicom.SerieTreeNode;
import org.shanoir.uploader.dicom.query.PatientTreeNode;
import org.shanoir.uploader.dicom.query.StudyTreeNode;
import org.shanoir.uploader.gui.ImportFromCSVWindow;
import org.shanoir.uploader.model.CsvImport;
import org.shanoir.uploader.model.Subject;
import org.shanoir.uploader.model.rest.AcquisitionEquipment;
import org.shanoir.uploader.model.rest.Examination;
import org.shanoir.uploader.model.rest.IdList;
import org.shanoir.uploader.model.rest.ImagedObjectCategory;
import org.shanoir.uploader.model.rest.Sex;
import org.shanoir.uploader.model.rest.StudyCard;
import org.shanoir.uploader.model.rest.SubjectType;
import org.shanoir.uploader.model.rest.importer.ImportJob;
import org.shanoir.uploader.nominativeData.NominativeDataUploadJob;
import org.shanoir.uploader.nominativeData.NominativeDataUploadJobManager;
import org.shanoir.uploader.service.rest.ShanoirUploaderServiceClient;
import org.shanoir.uploader.upload.UploadJob;
import org.shanoir.uploader.upload.UploadJobManager;
import org.shanoir.uploader.upload.UploadState;
import org.shanoir.uploader.utils.ImportUtils;
import org.shanoir.uploader.utils.Util;

public class ImportFromCsvRunner extends SwingWorker<Void, Integer> {

	private static final String WILDCARD = "*";
	private static final String WILDCARD_REPLACE = "\\*";

	private static Logger logger = Logger.getLogger(ImportFromCsvRunner.class);

	private List<CsvImport> csvImports;
	private ResourceBundle resourceBundle;
	private ImportFromCSVWindow importFromCSVWindow;
	private IdentifierCalculator identifierCalculator;
	private IDicomServerClient dicomServerClient;
	private ShanoirUploaderServiceClient shanoirUploaderServiceClientNG;

	public ImportFromCsvRunner(List<CsvImport> csvImports, ResourceBundle ressourceBundle, ImportFromCSVWindow importFromCSVWindow, IDicomServerClient dicomServerClient, ShanoirUploaderServiceClient shanoirUploaderServiceClientNG) {
		this.csvImports = csvImports;
		this.resourceBundle = ressourceBundle;
		this.importFromCSVWindow = importFromCSVWindow;
		this.identifierCalculator = new IdentifierCalculator();
		this.dicomServerClient = dicomServerClient;
		this.shanoirUploaderServiceClientNG = shanoirUploaderServiceClientNG;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// Iterate over import to import them one by one
		Set<Long> idList = new HashSet<>();
		Map<String, ArrayList<StudyCard>> studyCardsByStudy = new HashMap<>();

		importFromCSVWindow.openButton.setEnabled(false);
		importFromCSVWindow.uploadButton.setEnabled(false);

		importFromCSVWindow.progressBar.setStringPainted(true);
		importFromCSVWindow.progressBar.setString("Preparing import...");
		importFromCSVWindow.progressBar.setVisible(true);

		// Get the list of studies, study card, center, equipments to check their existence
		for (CsvImport importTodo : this.csvImports) {
			idList.add(Long.valueOf(importTodo.getStudyId()));
			studyCardsByStudy.put(importTodo.getStudyId(), new ArrayList<StudyCard>());
		}
		List<org.shanoir.uploader.model.rest.Study> studies = new ArrayList<>();
		try {
			studies = shanoirUploaderServiceClientNG.findStudiesNamesAndCenters();

			IdList idealist = new IdList();
			idealist.setIdList(new ArrayList<>(idList));
			List<StudyCard> studyCards = shanoirUploaderServiceClientNG.findStudyCardsByStudyIds(idealist);
			if (studyCards == null) {
				throw new Exception(resourceBundle.getString("shanoir.uploader.import.csv.error.studycard"));
			}

			List<AcquisitionEquipment> acquisitionEquipments = shanoirUploaderServiceClientNG.findAcquisitionEquipments();
			if (acquisitionEquipments == null) {
				throw new Exception("Error while retrieving acquisition equipments");
			}

			// Iterate over study cards to get equipment + fill study => SC map
			for (StudyCard studyCard : studyCards) {
				Long acquisitionEquipmentIdSC = studyCard.getAcquisitionEquipmentId();
				for (Iterator<AcquisitionEquipment> acquisitionEquipmentsIt = acquisitionEquipments.iterator(); acquisitionEquipmentsIt.hasNext();) {
					AcquisitionEquipment acquisitionEquipment = (AcquisitionEquipment) acquisitionEquipmentsIt.next();
					if (acquisitionEquipment.getId().equals(acquisitionEquipmentIdSC)) {
						studyCard.setAcquisitionEquipment(acquisitionEquipment);
						studyCard.setCenterId(acquisitionEquipment.getCenter().getId());
						break;
					}
				}
				studyCardsByStudy.get(studyCard.getStudyId().toString()).add(studyCard);
			}
		} catch (Exception e1) {
			this.importFromCSVWindow.error.setText(resourceBundle.getString("shanoir.uploader.import.csv.error.studycard"));
			return null;
		}

		boolean success = true;
		int i = 1;

		for (CsvImport importTodo : this.csvImports) {
			importFromCSVWindow.progressBar.setString("Preparing import " + i + "/" + this.csvImports.size());
			importFromCSVWindow.progressBar.setValue(100*i/this.csvImports.size() + 1);

			org.shanoir.uploader.model.rest.Study study = studies.stream().filter(element -> element.getId().toString().equals(importTodo.getStudyId())).findFirst().get();
			success = importData(importTodo, studyCardsByStudy.get(importTodo.getStudyId()), study ) && success;
			i++;
		}

		if (success) {
			importFromCSVWindow.progressBar.setString("Success !");
			importFromCSVWindow.progressBar.setValue(100);

			// Open current import tab and close csv import panel
			((JTabbedPane) this.importFromCSVWindow.scrollPaneUpload.getParent().getParent()).setSelectedComponent(this.importFromCSVWindow.scrollPaneUpload.getParent());

			this.importFromCSVWindow.frame.setVisible(false);
			this.importFromCSVWindow.frame.dispose();
		} else {
			this.importFromCSVWindow.displayCsv(csvImports);
			importFromCSVWindow.openButton.setEnabled(true);
			importFromCSVWindow.uploadButton.setEnabled(false);
		}
		return null;
	}

	/**
	 * Loads data to shanoir NG
	 * @param csvImport the import
	 * @param studyCardsByStudy the list of study
	 * @param study2
	 * @return
	 */
	private boolean importData(CsvImport csvImport, List<StudyCard> studyCardsByStudy, org.shanoir.uploader.model.rest.Study study2) {

		// 1. Request PACS to check the presence of data
		logger.info("1 Request PACS");
		List<Patient> patients = null;
		try {
			if (!StringUtils.isEmpty(csvImport.getIpp())) {
				patients = dicomServerClient.queryDicomServer("", csvImport.getIpp(), "", "", null, null);
			}
			if (patients == null) {
				String name = csvImport.getName().toUpperCase();
				if (!StringUtils.isEmpty(csvImport.getSurname())) {
					name+="^";
					name+=csvImport.getSurname().toUpperCase();
				}
				patients = dicomServerClient.queryDicomServer(name, "", "", "", null, null);
			}
		} catch (Exception e) {
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.missing.data"));
			return false;
		}

		// 2. Select series
		logger.info("2 Select series");

		Set<SerieTreeNode> selectedSeries = new HashSet<>();
		PatientTreeNode pat = null;
		StudyTreeNode stud = null;
		if (patients == null || patients.isEmpty()) {
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.missing.data"));
			return false;
		}
		boolean foundPatient = false;
		String serialNumber = null;
		String modelName = null;
		
		Map<StudyTreeNode, Set<SerieTreeNode>> selectedSeriesByStudy = new HashMap<>();
		StudyTreeNode selectedStudy = null;

		Date minDate;
		Date selectedStudyDate = new Date();
		if (!StringUtils.isBlank(csvImport.getMinDateFilter())) {
			
			String[] acceptedFormats = {"yyyy","yyyy-MM-dd","yyyy-MM-dd-HH"};
			try {
				minDate = DateUtils.parseDate(csvImport.getMinDateFilter(), acceptedFormats);
				selectedStudyDate = minDate;
			} catch (Exception e) {
				csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.date.format"));
				return false;
			}
		} else {
			Calendar cal = Calendar.getInstance();
			cal.set(1000, 0, 1, 0, 0, 0);
			minDate = cal.getTime();
		}

//		for (Patient patient : patients) {
//			if (foundPatient) {
//				// Get only one patient => Once we've selected a serie with interesting data, do not iterate more
//				break;
//			}
//			List<Study> studies = patient.getStudies();
//			Date currentDate = new Date();
//			for (Study study : studies) {
//				// get study date time
//				String[] acceptedFormats = {
//						"yyyyMMddHHmmSS.FFFFFF",
//						"yyyyMMddHHmmSS.FFFFF",
//						"yyyyMMddHHmmSS.FFFF",
//						"yyyyMMddHHmmSS.FFF",
//						"yyyyMMddHHmmSS.FF",
//						"yyyyMMddHHmmSS.F",
//						"yyyyMMddHHmmSS",
//						"yyyyMMddHHmmS",
//						"yyyyMMddHHmm",
//						"yyyyMMddHHm",
//						"yyyyMMddHH"};
//				Date studyDate = new Date();
//				try {
//					studyDate = DateUtils.parseDate(study.getStudyDate(), acceptedFormats);
//				} catch (ParseException e) {
//					logger.error("could not consider this study:", e);
//					// Could not get date => skip the study
//					continue;
//				}
//				if (studyDate.after(currentDate) || studyDate.before(minDate)) {
//					// We take the first valid date, if we are after on valid date, don't check the data
//					continue;
//				}
//				if (!searchField(study.getDisplayString(), csvImport.getStudyFilter())) {
//					continue;
//				}
//				stud = study;
//				selectedSeriesByStudy.put(stud, new HashSet<>());
//				Collection<DicomTreeNode> series = study.getTreeNodes().values();
//				for (Iterator<DicomTreeNode> seriesIt = series.iterator(); seriesIt.hasNext();) {
//					// Filter on serie
//					SerieTreeNode serie = (SerieTreeNode) seriesIt.next();
//					if (searchField(serie.getDescription(), csvImport.getAcquisitionFilter())) {
//						selectedSeriesByStudy.get(stud).add(serie);
//						selectedStudy = stud;
//						serialNumber = serie.getMriInformation().getDeviceSerialNumber();
//						modelName = serie.getMriInformation().getManufacturersModelName();
//						foundPatient = true;
//						currentDate = studyDate;
//						selectedStudyDate = studyDate;
//					}
//				}
//			}
//		}
		if (selectedStudy == null) {
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.missing.data"));
			return false;
		}
		selectedSeries = selectedSeriesByStudy.get(selectedStudy);
		if (selectedSeries.isEmpty()) {
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.missing.data"));
			return false;
		}

		// 3. Check existence of study / study card
		logger.info("3 Check study card");

		StudyCard sc = null;
		for (StudyCard studyc : studyCardsByStudy) {
			if (serialNumber != null && serialNumber.equals(studyc.getAcquisitionEquipment().getSerialNumber())) {
				sc = studyc;
				break;
			}
			if (modelName != null && modelName.equals(studyc.getAcquisitionEquipment().getManufacturerModel().getName())) {
				sc = studyc;
				break;
			}
		}

		// No study card by default => get the one in the file (if existing of course)
		if (sc == null) {
			Optional<StudyCard> scOpt = studyCardsByStudy.stream().filter(element -> element.getName().equals(csvImport.getStudyCardName())).findFirst();
			if (!scOpt.isPresent()) {
				csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.studycard"));
				return false;
			} else {
				sc = scOpt.get();
			}
		}

		// 4. Create DicomDataTransferObject
		logger.info("4 Complete data");

		DicomDataTransferObject dicomData = null;
		String subjectIdentifier = "";

		try {
			dicomData = new DicomDataTransferObject(null, pat, stud);

			// Calculate identifier

			// #1609 we encounter problems when using same subject data accross multiple studies.
			// We add study id in front of identifier to correct this
			subjectIdentifier = stud.getId() +  this.identifierCalculator.calculateIdentifier(dicomData.getFirstName(), dicomData.getLastName(), dicomData.getBirthDate());

			dicomData.setSubjectIdentifier(subjectIdentifier);

			// Change birth date to first day of year
//			final String dicomBirthDate = pat.getDescriptionMap().get("birthDate");
//			if (!StringUtils.isEmpty(dicomBirthDate)) {
//				Date dicomBirthDateAsDate = Util.convertStringDicomDateToDate(dicomBirthDate);
//				Calendar cal = Calendar.getInstance();
//				cal.setTime(dicomBirthDateAsDate);
//				cal.set(Calendar.MONTH, Calendar.JANUARY);
//				cal.set(Calendar.DAY_OF_MONTH, 1);
//				dicomData.setBirthDate(cal.getTime());
//			}
		} catch (Exception e) {
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.missing.data"));
			return false;
		}

		File uploadFolder = ImportUtils.createUploadFolder(dicomServerClient.getWorkFolder(), dicomData);
		List<String> allFileNames = null;
		try {
			allFileNames = ImportUtils.downloadOrCopyFilesIntoUploadFolder(true, selectedSeries, uploadFolder, this.dicomServerClient, null);
		} catch (Exception e) {
			logger.error("Could not copy data from PACS !");
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.missing.data"));
			return false;
		}

		/**
		 * 5. Fill MRI information into serie from first DICOM file of each serie
		 * This has already been done for CD/DVD import, but not yet here for PACS
		 */
		logger.info("5 Fill MRI info");

		for (Iterator<SerieTreeNode> iterator = selectedSeries.iterator(); iterator.hasNext();) {
			SerieTreeNode serie = iterator.next();
			Util.processSerieMriInfo(uploadFolder, serie);
		}

		/**
		 * 6. Write the UploadJob and schedule upload
		 */
		logger.info("6 Write upload job");

		UploadJob uploadJob = new UploadJob();
		ImportUtils.initUploadJob(selectedSeries, dicomData, uploadJob);

		if (allFileNames == null) {
			uploadJob.setUploadState(UploadState.ERROR);
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.pacs.copy"));
			return false;
		}
		UploadJobManager uploadJobManager = new UploadJobManager(uploadFolder.getAbsolutePath());
		uploadJobManager.writeUploadJob(uploadJob);

		/**
		 * 7. Write the NominativeDataUploadJobManager for displaying the download state
		 */
		logger.info("7 Write upload job nominative");

		NominativeDataUploadJob dataJob = new NominativeDataUploadJob();
		ImportUtils.initDataUploadJob(selectedSeries, dicomData, dataJob);

		NominativeDataUploadJobManager uploadDataJobManager = new NominativeDataUploadJobManager(
				uploadFolder.getAbsolutePath());
		uploadDataJobManager.writeUploadDataJob(dataJob);
		ShUpOnloadConfig.getCurrentNominativeDataController().addNewNominativeData(uploadFolder, dataJob);

		logger.info(uploadFolder.getName() + ": finished: " + toString());

		Long centerId = sc.getCenterId();

		// 8.  Create subject if necessary
		Subject subjectFound = null;
		String subjectStudyIdentifier = null;
		try {
//			subjectFound = shanoirUploaderServiceClientNG.findSubjectBySubjectIdentifier(subjectIdentifier);
			if (!subjectFound.getName().equals(csvImport.getCommonName())) {
				// If the name does not match, change the subjectStudyIdentifier for this study
				subjectStudyIdentifier = csvImport.getCommonName();
			}
		} catch (Exception e) {
			//Do nothing, if it fails, we'll just create a new subject
		}
		Subject subject;
		if (subjectFound != null) {
			logger.info("8 Subject exists, just use it");
			subject = subjectFound;
//			ImportUtils.addSubjectStudy(study2, subject, SubjectType.PATIENT, true, subjectStudyIdentifier);
//			subject = shanoirUploaderServiceClientNG.createSubjectStudy(subject);
		} else {
			logger.info("8 Creating a new subject");
	
			subject = new Subject();
			subject.setName(csvImport.getCommonName());
//			if (!StringUtils.isEmpty(pat.getDescriptionMap().get("sex"))) {
//				subject.setSex(Sex.valueOf(pat.getDescriptionMap().get("sex")));
//			} else {
//				// Force feminine (girl power ?)
//				subject.setSex(Sex.F);
//			}
			subject.setIdentifier(subjectIdentifier);
	
//			subject.setImagedObjectCategory(ImagedObjectCategory.LIVING_HUMAN_BEING);
//	
//			subject.setBirthDate(dicomData.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
//	
//			ImportUtils.addSubjectStudy(study2, subject, SubjectType.PATIENT, true, subjectStudyIdentifier);
//	
//			// Get center ID from study card
//			subject = shanoirUploaderServiceClientNG.createSubject(subject, true, centerId);
		}
		if (subject == null) {
			uploadJob.setUploadState(UploadState.ERROR);
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.subject"));
			return false;
		}

		// 9. Create examination
		logger.info("9 Create exam");

		Examination examDTO = new Examination();
		examDTO.setCenterId(centerId);
		examDTO.setComment(csvImport.getComment());
		examDTO.setExaminationDate(selectedStudyDate);
		examDTO.setPreclinical(false);
		examDTO.setStudyId(Long.valueOf(csvImport.getStudyId()));
		examDTO.setSubjectId(subject.getId());
		Examination createdExam = shanoirUploaderServiceClientNG.createExamination(examDTO);

		if (createdExam == null) {
			uploadJob.setUploadState(UploadState.ERROR);
			csvImport.setErrorMessage(resourceBundle.getString("shanoir.uploader.import.csv.error.examination"));
			return false;
		}

		/**
		 * 10. Fill import-job.json to prepare the import
		 */
		logger.info("10 Import.json");

		ImportJob importJob = ImportUtils.prepareImportJob(uploadJob, subject.getName(), subject.getId(), createdExam.getId(), study2, sc);
		importJob.setFromCsv(true);
		Runnable runnable = new ImportFinishRunnable(uploadJob, uploadFolder, importJob, subject.getName());
		Thread thread = new Thread(runnable);
		thread.start();
		return true;
	}

	/**
	 * This method allows to check if a filter with potentiel wildcard '*' is contained in the searched element
	 * @param searchedElement the string that is checked
	 * @param filter the filter we want to find
	 * @return true if the filter matches, false otherwise
	 */
	protected boolean searchField(String searchedElement, String filter) {
		if (StringUtils.isBlank(searchedElement)) {
			return false;
		}
		if (StringUtils.isBlank(filter) || filter.equals("*")) {
			return true;
		}
		String[] filters;
		boolean valid = true;
		// NB: It is possible to have AND ";" filters OR OR ";;" filters but not both at the same time for the moment.

		if (filter.contains(";;")) {
			valid = false;
			filters = filter.split(";;");
			for (String filterToApply : filters) {
				if (filterToApply.startsWith("!")) {
					valid =  valid || !filterWildCard(searchedElement, filterToApply.replaceAll("!", ""));
				} else {
					valid =  valid || filterWildCard(searchedElement, filterToApply.replaceAll("!", ""));
				}
			}
			return valid;
		} else if (filter.contains(";")) {
			filters = filter.split(";");
			for (String filterToApply : filters) {
				if (filterToApply.startsWith("!")) {
					valid =  valid && !filterWildCard(searchedElement, filterToApply.replaceAll("!", ""));
				} else {
					valid =  valid && filterWildCard(searchedElement, filterToApply.replaceAll("!", ""));
				}
			}
			return valid;
		} else {
			if (filter.startsWith("!")) {
				valid = !filterWildCard(searchedElement, filter.replaceAll("!", ""));
			} else {
				valid = filterWildCard(searchedElement, filter.replaceAll("!", ""));
			}
			return valid;
		}
	}

	/*
	@Test
	public void testSearchField() {
		assertFalse(searchField("", ""));
		assertTrue(searchField("tested", ""));
		assertTrue(searchField("tested", "*"));
		assertTrue(searchField("tested", "*sted"));
		assertTrue(searchField("tested", "test*"));
		assertTrue(searchField("tested", "*est*"));
		assertFalse(searchField("tested", "*ast*"));
		assertTrue(searchField("tested", "*st*;*ed"));
		assertTrue(searchField("tested", "*st*;;*ed"));
		assertFalse(searchField("tested", "*sta*;*ed"));
		assertTrue(searchField("tested", "*sta*;;*ed"));
		assertFalse(searchField("tested", "*sta*;*tad*"));
		assertFalse(searchField("tested", "*sta*;;*tad*"));
	}
	*/

	/**
	 * Check if filterd elements contains or not the data sent in argument
	 * @param searchedElement
	 * @param filter
	 * @return
	 */
	private boolean filterWildCard(String searchedElement, String filter) {
		// Set all to uppercase
		searchedElement = searchedElement.toUpperCase();
		if(filter.endsWith(WILDCARD)) {
			if(filter.startsWith(WILDCARD)) {
				// *filter*
				return searchedElement.contains(filter.replaceAll(WILDCARD_REPLACE, "").toUpperCase());
			}
			// filter*
			return searchedElement.startsWith(filter.replaceAll(WILDCARD_REPLACE, "").toUpperCase());
		}
		if(filter.startsWith(WILDCARD)) {
			// *filter
			return searchedElement.endsWith(filter.replaceAll(WILDCARD_REPLACE, "").toUpperCase());
		}
		// filter
		return searchedElement.equalsIgnoreCase(filter);
	}
}
