import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  AttendanceBatchUpsertPayload,
  AttendanceResponseItem,
  AttendanceUpdatePayload,
  DailyAttendance,
  MonthlyAttendanceMatrix
} from '../../shared/models/attendance.model';

@Injectable({
  providedIn: 'root'
})
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/attendance`;

  /**
   * Lấy dữ liệu điểm danh theo ngày của một lớp học (UC-13).
   */
  getDailyAttendance(classId: string, date?: string): Observable<ApiResponse<DailyAttendance>> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<ApiResponse<DailyAttendance>>(`${this.apiUrl}/classes/${classId}/daily`, { params });
  }

  /**
   * Lưu hoặc cập nhật hàng loạt (Batch Upsert) điểm danh cho cả lớp trong 1 request (UC-13).
   */
  batchUpsertAttendance(payload: AttendanceBatchUpsertPayload): Observable<ApiResponse<DailyAttendance>> {
    return this.http.post<ApiResponse<DailyAttendance>>(`${this.apiUrl}/batch`, payload);
  }

  /**
   * Lấy ma trận lịch sử điểm danh và tổng hợp số buổi học theo tháng (UC-14, UC-15).
   */
  getMonthlyAttendanceMatrix(classId: string, month: string): Observable<ApiResponse<MonthlyAttendanceMatrix>> {
    const params = new HttpParams().set('month', month);
    return this.http.get<ApiResponse<MonthlyAttendanceMatrix>>(`${this.apiUrl}/classes/${classId}/monthly`, { params });
  }

  /**
   * Chỉnh sửa lẻ 1 bản ghi điểm danh cụ thể (UC-14).
   */
  updateAttendance(id: string, payload: AttendanceUpdatePayload): Observable<ApiResponse<AttendanceResponseItem>> {
    return this.http.put<ApiResponse<AttendanceResponseItem>>(`${this.apiUrl}/${id}`, payload);
  }

  /**
   * Hủy/Xóa toàn bộ buổi điểm danh của một ngày (dành cho điểm danh nhầm).
   */
  deleteDailyAttendance(classId: string, date: string): Observable<ApiResponse<void>> {
    const params = new HttpParams().set('date', date);
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/classes/${classId}/daily`, { params });
  }
}

