import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { SystemParameterItem, UpdateSystemParameterRequest } from '../models/system-parameter.models';

@Injectable({
  providedIn: 'root'
})
export class SystemParameterService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/settings/parameters`;

  getParameters(group?: string, keyword?: string): Observable<ApiResponse<SystemParameterItem[]>> {
    let params = new HttpParams();
    if (group && group !== 'ALL') {
      params = params.set('group', group);
    }
    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    return this.http.get<ApiResponse<SystemParameterItem[]>>(this.apiUrl, { params });
  }

  getParameter(key: string): Observable<ApiResponse<SystemParameterItem>> {
    return this.http.get<ApiResponse<SystemParameterItem>>(`${this.apiUrl}/${key}`);
  }

  updateParameter(key: string, paramValue: string): Observable<ApiResponse<SystemParameterItem>> {
    const payload: UpdateSystemParameterRequest = { paramValue };
    return this.http.put<ApiResponse<SystemParameterItem>>(`${this.apiUrl}/${key}`, payload);
  }

  reloadCache(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/reload-cache`, {});
  }

  resetDefaults(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/reset-defaults`, {});
  }
}

