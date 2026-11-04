import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { PaymentSetting, PaymentSettingRequest } from '../models/payment-setting.models';

@Injectable({
  providedIn: 'root'
})
export class PaymentSettingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/settings/payment`;

  getPaymentSetting(): Observable<ApiResponse<PaymentSetting>> {
    return this.http.get<ApiResponse<PaymentSetting>>(this.apiUrl);
  }

  savePaymentSetting(payload: PaymentSettingRequest): Observable<ApiResponse<PaymentSetting>> {
    return this.http.put<ApiResponse<PaymentSetting>>(this.apiUrl, payload);
  }
}

