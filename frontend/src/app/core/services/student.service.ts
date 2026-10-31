import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  StudentImportResult,
  StudentItem,
  StudentRequestPayload,
  StudentStatus
} from '../../shared/models/student.model';

@Injectable({
  providedIn: 'root'
})
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/students`;

  getStudents(filters?: {
    classId?: string;
    status?: StudentStatus;
    search?: string;
  }): Observable<ApiResponse<StudentItem[]>> {
    let params = new HttpParams();
    if (filters?.classId) params = params.set('classId', filters.classId);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.search) params = params.set('search', filters.search);
    return this.http.get<ApiResponse<StudentItem[]>>(this.apiUrl, { params });
  }

  getStudentById(id: string): Observable<ApiResponse<StudentItem>> {
    return this.http.get<ApiResponse<StudentItem>>(`${this.apiUrl}/${id}`);
  }

  createStudent(payload: StudentRequestPayload): Observable<ApiResponse<StudentItem>> {
    return this.http.post<ApiResponse<StudentItem>>(this.apiUrl, payload);
  }

  updateStudent(id: string, payload: StudentRequestPayload): Observable<ApiResponse<StudentItem>> {
    return this.http.put<ApiResponse<StudentItem>>(`${this.apiUrl}/${id}`, payload);
  }

  softDeleteStudent(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  updateStudentStatus(id: string, status: StudentStatus): Observable<ApiResponse<StudentItem>> {
    return this.http.patch<ApiResponse<StudentItem>>(`${this.apiUrl}/${id}/status`, { status });
  }

  importStudents(classId: string, file: File): Observable<ApiResponse<StudentImportResult>> {
    const formData = new FormData();
    formData.append('classId', classId);
    formData.append('file', file);
    return this.http.post<ApiResponse<StudentImportResult>>(`${this.apiUrl}/import`, formData);
  }

  downloadTemplateExcel(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/import/template`, { responseType: 'blob' });
  }
}
