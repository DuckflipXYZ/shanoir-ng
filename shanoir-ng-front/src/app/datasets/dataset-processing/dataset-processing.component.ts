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

import { Component, ViewChild } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Option } from '../../shared/select/select.component';
import { EntityComponent } from '../../shared/components/entity/entity.component.abstract';
import { DatasetProcessingType } from '../../enum/dataset-processing-type.enum';
import { Dataset } from '../shared/dataset.model';
import { DatasetService } from '../shared/dataset.service';
import { DatasetProcessing } from '../../datasets/shared/dataset-processing.model';
import { DatasetProcessingService } from '../shared/dataset-processing.service';
import { StudyService } from '../../studies/shared/study.service';
import { Study } from '../../studies/shared/study.model';
import { Subject } from '../../subjects/shared/subject.model';
import { EntityService } from '../../shared/components/entity/entity.abstract.service';
import { BrowserPaging } from '../../shared/components/table/browser-paging.model';
import { FilterablePageable, Page } from '../../shared/components/table/pageable.model';
import { TableComponent } from '../../shared/components/table/table.component';
import { ColumnDefinition } from '../../shared/components/table/column.definition.type';
import { CarminDatasetProcessingService } from 'src/app/carmin/shared/carmin-dataset-processing.service';
import { CarminDatasetProcessing } from 'src/app/carmin/models/CarminDatasetProcessing';

@Component({
    selector: 'dataset-processing-detail',
    templateUrl: 'dataset-processing.component.html',
    styleUrls: ['dataset-processing.component.css']
})

export class DatasetProcessingComponent extends EntityComponent<DatasetProcessing> {

    @ViewChild('inputDatasetsTable', {static: false}) inputDatasetsTable: TableComponent;
    @ViewChild('outputDatasetsTable', {static: false}) outputDatasetsTable: TableComponent;

    public datasetProcessingTypes: Option<DatasetProcessingType>[] = DatasetProcessingType.options;
    public study: Study;
    public subject: Subject;
    public studyOptions: Option<Study>[] = [];
    public subjectOptions: Option<Subject>[] = [];
    public inputDatasetOptions: Option<Dataset>[] = [];
    private inputDatasetsPromise: Promise<any>;
    private outputDatasetsPromise: Promise<any>;
    private inputDatasetsBrowserPaging: BrowserPaging<Dataset>;
    private outputDatasetsBrowserPaging: BrowserPaging<Dataset>;
    private inputDatasetsToRemove: Dataset[] = [];
    private inputDatasetsToAdd: Dataset[] = [];
    private outputDatasetsToRemove: Dataset[] = [];
    public inputDatasetsColumnDefs: ColumnDefinition[];
    public outputDatasetsColumnDefs: ColumnDefinition[];
    public isCarminDatasetProcessingEntity: boolean = false;
    public carminDatasetProcessing: CarminDatasetProcessing;
    prefilledStudy: Study;
    prefilledSubject: Subject;

    constructor(
            private route: ActivatedRoute,
            private studyService: StudyService,
            private datasetService: DatasetService,
            private datasetProcessingService: DatasetProcessingService,
            private carminDatasetProcessingService: CarminDatasetProcessingService
            ) {

        super(route, 'dataset-processing');
    }
    
    ngOnInit(): void {
        super.ngOnInit();
        this.createColumnDefs();
        this.outputDatasetsPromise = Promise.resolve().then(() => {
            this.outputDatasetsBrowserPaging = new BrowserPaging([], this.outputDatasetsColumnDefs);
        });
        this.inputDatasetsPromise = Promise.resolve().then(() => {
            this.inputDatasetsBrowserPaging = new BrowserPaging([], this.inputDatasetsColumnDefs);
        });
    }

    get datasetProcessing(): DatasetProcessing { return this.entity; }
    set datasetProcessing(datasetProcessing: DatasetProcessing) { this.entity = datasetProcessing; }

    getService(): EntityService<DatasetProcessing> {
        return this.datasetProcessingService;
    }


    initView(): Promise<void> {
        return this.datasetProcessingService.get(this.id).then((entity)=> {
            // checking if the datasetProcessing is carmin type.
            this.carminDatasetProcessingService.getCarminDatasetProcessing(entity.id).subscribe(
                (carminDatasetProcessing: CarminDatasetProcessing) => {
                    this.setCarminDatasetProcessing(carminDatasetProcessing);
                }, (error) => {
                    // 404 : if it's not found then it's not carmin type !
                    this.resetCarminDatasetProcessing();
                }
            )

            this.datasetProcessing = entity;
            this.fetchOneStudy(this.datasetProcessing?.id).then(() => {
                this.study = this.studyOptions?.[0]?.value;
            });
        })
    }

    initEdit(): Promise<void> {
        this.fetchStudies();
        return this.datasetProcessingService.get(this.id).then(entity => {
            this.datasetProcessing = entity;
        });
    }

    initCreate(): Promise<void> {
        this.datasetProcessing = new DatasetProcessing();
        this.prefilledStudy = this.breadcrumbsService.currentStep.getPrefilledValue('study');
        this.prefilledSubject = this.breadcrumbsService.currentStep.getPrefilledValue('subject');
        return Promise.resolve().then(() => {
            if (!!this.prefilledStudy) {
                this.studyOptions = [new Option(this.prefilledStudy, this.prefilledStudy.name)];
                this.study = this.prefilledStudy;
                this.datasetProcessing.studyId = this.study?.id;
            } else {
                return this.fetchStudies();
            }
        }).then(() => {
            if (!!this.prefilledSubject) {
                this.subjectOptions = [new Option(this.prefilledSubject, this.prefilledSubject.name)];
                this.subject = this.prefilledSubject;
                return this.fetchDatasets();
            }
        })
    }

    onStudyChange() {
        this.datasetProcessing.studyId = this.study?.id;
        this.subjectOptions = [];
        this.subject = null;
        if (this.study) {
            this.fetchSubjects();
        }
    }

    onSubjectChange() {
        this.inputDatasetOptions = [];  
        if (this.datasetProcessing.inputDatasets?.length > 0 || this.datasetProcessing.outputDatasets?.length > 0) {
            this.confirmDialogService.confirm('Change Subject', 'Are you sure you want to change the subject for this processing ? Every dataset input and output will be removed from the current lists.')
            .then(response => {
                if (response) {
                    this.datasetProcessing.inputDatasets = [];
                    this.datasetProcessing.outputDatasets = [];
                    this.fetchDatasets();
                }
            });
        } else {
            this.fetchDatasets();
        }
    }

    setCarminDatasetProcessing(carminDatasetProcessing: CarminDatasetProcessing){
        this.isCarminDatasetProcessingEntity = true;
        this.carminDatasetProcessing = carminDatasetProcessing;
    }

    resetCarminDatasetProcessing(){
        this.isCarminDatasetProcessingEntity = false;
    }

    fetchStudies(): Promise<void> {
        return this.studyService.getAll().then(studies => {
            this.studyOptions = studies?.map(study => new Option<Study>(study, study.name));
        });
    }

    fetchOneStudy(studyId: number): Promise<void> {
        return this.studyService.get(studyId).then(study=> {
            this.studyOptions = [new Option<Study>(study, study.name)];
        });
    }

    fetchSubjects(): Promise<void> {
        if (!this.study?.id) return Promise.resolve();
        return this.studyService.findSubjectsByStudyId(this.study.id).then(subjects => {
            this.subjectOptions = subjects?.map(sub => {
                let subject: Subject = new Subject();
                subject.id = sub.id;
                subject.name = sub.name;
                subject.identifier = sub.identifier;
                return new Option<Subject>(subject, sub.name);
            });
        }).then();
    }

    fetchDatasets(): Promise<void> {
        if (!this.study?.id || !this.subject?.id) return Promise.resolve();
        return this.datasetService.getByStudyIdAndSubjectId(this.study.id, this.subject.id).then(datasets => {
            for (let dataset of datasets) {
                this.inputDatasetOptions.push(new Option<Dataset>(dataset, dataset.name));
            }
        });
    }

    buildForm(): FormGroup {
        let formGroup: FormGroup = this.formBuilder.group({
            'study': [{value: this.study?.id, disabled: !!this.prefilledStudy}, Validators.required],
            'subject': [{value: this.subject, disabled: (!!this.prefilledSubject || !this.study)}, Validators.required],
            'processingType': [this.datasetProcessing.datasetProcessingType, Validators.required],
            'processingDate': [this.datasetProcessing.processingDate, Validators.required],
            'inputDatasetList': [{value: this.datasetProcessing.inputDatasets, disabled: !this.subject}, [Validators.required, Validators.minLength(1)]],
            'outputDatasetList': [this.datasetProcessing.outputDatasets],
            'comment': [this.datasetProcessing.comment]
        });
        this.subscribtions.push(
            formGroup.get('study').valueChanges.subscribe(studyVal => {
                if (!!this.prefilledSubject || !studyVal) formGroup.get('subject').disable();
                else formGroup.get('subject').enable();    
            }), formGroup.get('subject').valueChanges.subscribe(subjectVal => {
                if (!subjectVal) formGroup.get('inputDatasetList').disable();
                else formGroup.get('inputDatasetList').enable();    
            })
        );
        return formGroup;
    }

    public async hasEditRight(): Promise<boolean> {
        return false;
    }

    getInputDatasetsPage(pageable: FilterablePageable): Promise<Page<Dataset>> {
        return new Promise((resolve) => {
            this.inputDatasetsPromise.then(() => {
                resolve(this.inputDatasetsBrowserPaging.getPage(pageable));
            });
        });
    }

    getOutputDatasetsPage(pageable: FilterablePageable): Promise<Page<Dataset>> {
        return new Promise((resolve) => {
            this.outputDatasetsPromise.then(() => {
                resolve(this.outputDatasetsBrowserPaging.getPage(pageable));
            });
        });
    }

    private createColumnDefs() {
        this.inputDatasetsColumnDefs = [
            {headerName: "ID", field: "id", type: "number"},
            {headerName: "Name", field: "name", route: (dataset : Dataset)=>{
                return `/dataset/details/${dataset.id}`
            }},
            {headerName: "Dataset type", field: "type"},
            {headerName: "Study", field: "study.name"},
            {headerName: "Subject", field: "subject.name"},
            {headerName: "Creation date", field: "creationDate", type: "date"}
        ];
        this.outputDatasetsColumnDefs = this.inputDatasetsColumnDefs;
    }

    public onAddInputDataset(selectedDataset: Dataset) {
        if (!selectedDataset) {
            return;
        }
        if (this.datasetProcessing.inputDatasets.filter(dataset => dataset.id == selectedDataset.id).length > 0){
            return;   
        }
        this.inputDatasetsToAdd.push(selectedDataset);
        this.datasetProcessing.inputDatasets.push(selectedDataset);
        this.inputDatasetsBrowserPaging.setItems(this.datasetProcessing.inputDatasets);
        this.inputDatasetsTable.refresh();
        this.form.markAsDirty();
        this.form.updateValueAndValidity();
    }

    removeInputDataset(item: Dataset) {
        const index: number = this.datasetProcessing.inputDatasets.indexOf(item);
        if (index !== -1) {
            this.datasetProcessing.inputDatasets.splice(index, 1);
        }
        this.inputDatasetsToRemove.push(item);
        this.inputDatasetsBrowserPaging.setItems(this.datasetProcessing.inputDatasets);
        this.inputDatasetsTable.refresh();
    }

    removeOutputDataset(item: Dataset) {
        const index: number = this.datasetProcessing.outputDatasets.indexOf(item);
        if (index !== -1) {
            this.datasetProcessing.outputDatasets.splice(index, 1);
        }
        this.outputDatasetsToRemove.push(item);
        this.outputDatasetsBrowserPaging.setItems(this.datasetProcessing.outputDatasets);
        this.outputDatasetsTable.refresh();
    }

    private isCarminDatasetProcessing(datasetProcessing: DatasetProcessing){
        if(datasetProcessing.comment == null || !datasetProcessing.comment.startsWith("workflow-")) return false;
        return true;
    }

}