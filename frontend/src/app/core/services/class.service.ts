import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ClassItem, ClassRequestPayload } from '../../shared/models/class.model';

@Injectable({
  providedIn: 'root'
})
export class ClassService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/classes`;

  getClasses(includeArchived = true): Observable<ApiResponse<ClassItem[]>> {
    const params = new HttpParams().set('includeArchived', String(includeArchived));
    return this.http.get<ApiResponse<ClassItem[]>>(this.apiUrl, { params });
  }

  getClassById(id: string): Observable<ApiResponse<ClassItem>> {
    return this.http.get<ApiResponse<ClassItem>>(`${this.apiUrl}/${id}`);
  }

  createClass(payload: ClassRequestPayload): Observable<ApiResponse<ClassItem>> {
    return this.http.post<ApiResponse<ClassItem>>(this.apiUrl, payload);
  }

  updateClass(id: string, payload: ClassRequestPayload): Observable<ApiResponse<ClassItem>> {
    return this.http.put<ApiResponse<ClassItem>>(`${this.apiUrl}/${id}`, payload);
  }

  softDeleteClass(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  restoreClass(id: string): Observable<ApiResponse<ClassItem>> {
    return this.http.patch<ApiResponse<ClassItem>>(`${this.apiUrl}/${id}/restore`, {});
  }
}
