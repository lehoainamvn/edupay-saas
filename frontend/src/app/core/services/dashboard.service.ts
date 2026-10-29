import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';

export interface DashboardKpis {
  expectedTuition: number;
  actualTuition: number;
  unpaidTuition: number;
  attendanceRate: number;
  totalStudents: number;
  totalClasses: number;
  unpaidCount: number;
  paidCount: number;
  progressPercent: number;
  attendedCount: number;
  permittedLeaveCount: number;
  unpermittedLeaveCount: number;
}

export interface TodayClassItem {
  id: string;
  name: string;
  time: string;
  end: string;
  room: string;
  count: number;
  fee: number;
  feeType: string;
  state: 'upcoming' | 'later';
  text: string;
}

export interface RecentTransactionItem {
  id: string;
  name: string;
  className: string;
  time: string;
  amount: string;
  rawAmount: number;
  code: string;
  initials: string;
  status: string;
}

export interface DebtorItem {
  id: string;
  name: string;
  parent: string;
  amount: string;
  rawAmount: number;
  due: string;
  initials: string;
  danger?: boolean;
}

export interface BankSettingInfo {
  bankName: string;
  accountNumber: string;
  accountName: string;
  bankBin: string;
}

export interface DashboardSummary {
  kpis: DashboardKpis;
  todayClasses: TodayClassItem[];
  recentTransactions: RecentTransactionItem[];
  debtors: DebtorItem[];
  bankSettings?: BankSettingInfo;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/dashboard/summary`;

  getDashboardSummary(): Observable<ApiResponse<DashboardSummary>> {
    return this.http.get<ApiResponse<DashboardSummary>>(this.apiUrl);
  }
}

