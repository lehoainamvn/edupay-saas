import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';

export interface CenterTeacherItem {
  id: string;
  fullName: string;
  phone: string;
  email?: string;
  status: 'ACTIVE' | 'SUSPENDED' | string;
  assignedClassesCount: number;
  createdAt: string;
  lastLoginAt?: string;
}

export interface CreateCenterTeacherPayload {
  phone: string;
  fullName: string;
  email?: string;
  password: string;
}

export interface UpdateCenterTeacherPayload {
  fullName: string;
  email?: string;
  status?: string;
}

@Injectable({
  providedIn: 'root',
})
export class CenterTeacherService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/center/teachers`;

  getTeachers(): Observable<ApiResponse<CenterTeacherItem[]>> {
    return this.http.get<ApiResponse<CenterTeacherItem[]>>(this.baseUrl);
  }

  getTeacherById(id: string): Observable<ApiResponse<CenterTeacherItem>> {
    return this.http.get<ApiResponse<CenterTeacherItem>>(`${this.baseUrl}/${id}`);
  }

  createTeacher(payload: CreateCenterTeacherPayload): Observable<ApiResponse<CenterTeacherItem>> {
    return this.http.post<ApiResponse<CenterTeacherItem>>(this.baseUrl, payload);
  }

  updateTeacher(id: string, payload: UpdateCenterTeacherPayload): Observable<ApiResponse<CenterTeacherItem>> {
    return this.http.put<ApiResponse<CenterTeacherItem>>(`${this.baseUrl}/${id}`, payload);
  }

  updateTeacherStatus(id: string, status: string): Observable<ApiResponse<CenterTeacherItem>> {
    return this.http.patch<ApiResponse<CenterTeacherItem>>(`${this.baseUrl}/${id}/status`, { status });
  }

  resetPassword(id: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/${id}/reset-password`, { newPassword });
  }

  deleteTeacher(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}

