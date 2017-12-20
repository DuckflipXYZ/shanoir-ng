import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import * as AppUtils from '../../utils/app.utils';
import { Examination } from './examination.model';
import { HandleErrorService } from '../../shared/utils/handle-error.service';
import { SubjectExamination } from '../shared/subject-examination.model';

@Injectable()
export class ExaminationService {
    constructor(private http: HttpClient, private handleErrorService: HandleErrorService) { }

    getExaminations(): Promise<Examination[]> {
        return this.http.get<Examination[]>(AppUtils.BACKEND_API_EXAMINATION_URL)
            .toPromise()
            .then(response => response)
            .catch((error) => {
                console.error('Error while getting examinations', error);
                return Promise.reject(error.message || error);
            });
    }

    delete(id: number): Promise<void> {
        return this.http.delete<void>(AppUtils.BACKEND_API_EXAMINATION_URL + '/' + id)
            .toPromise()
            .catch((error) => {
                console.error('Error delete examination', error);
                return Promise.reject(error.message || error);
            });
    }

    getExamination(id: number): Promise<Examination> {
        return this.http.get<Examination>(AppUtils.BACKEND_API_EXAMINATION_URL + '/' + id)
            .toPromise()
            .then(res => res)
            .catch((error) => {
                console.error('Error while getting examination', error);
                return Promise.reject(error.message || error);
            });
    }

    create(examination: Examination): Observable<Examination> {
        return this.http.post<Examination>(AppUtils.BACKEND_API_EXAMINATION_URL, JSON.stringify(examination))
            .map(response => response)
            .catch(this.handleErrorService.handleError);
    }

    update(id: number, examination: Examination): Observable<Examination> {
        return this.http.put<Examination>(AppUtils.BACKEND_API_EXAMINATION_URL + '/' + id, JSON.stringify(examination))
            .map(response => response)
            .catch(this.handleErrorService.handleError);
    }

    findExaminationsBySubjectId(subjectId: number): Promise<SubjectExamination[]> {
        return this.http.get<SubjectExamination[]>(AppUtils.BACKEND_API_EXAMINATION_ALL_BY_SUBJECT_URL + '/' + subjectId)
            .toPromise()
            .then(response => response)
            .catch((error) => {
                console.error('Error while getting examinations by subject id', error);
                return Promise.reject(error.message || error);
            });
    }
}