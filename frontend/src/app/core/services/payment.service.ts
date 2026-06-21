import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@env/environment';
import { ApiResponse, Payment, PagedResponse } from '../models';

export interface VerifyPaymentPayload {
  orderId?: number;
  paymentId?: number;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private base = `${environment.apiUrl}/payments`;

  constructor(private http: HttpClient) {}

  getPaymentByOrderId(orderId: number): Observable<Payment> {
    return this.http.get<ApiResponse<Payment>>(`${this.base}/order/${orderId}`)
      .pipe(map(r => r.data));
  }

  verifyPayment(payload: VerifyPaymentPayload): Observable<Payment> {
    return this.http.post<ApiResponse<Payment>>(`${this.base}/verify`, payload)
      .pipe(map(r => r.data));
  }

  getMyPaymentsAsDealer(page = 0, size = 10): Observable<PagedResponse<Payment>> {
    return this.http.get<ApiResponse<PagedResponse<Payment>>>(`${this.base}/dealer`, {
      params: { page, size }
    }).pipe(map(r => r.data));
  }

  getMyPaymentsAsFarmer(page = 0, size = 10): Observable<PagedResponse<Payment>> {
    return this.http.get<ApiResponse<PagedResponse<Payment>>>(`${this.base}/farmer`, {
      params: { page, size }
    }).pipe(map(r => r.data));
  }

  getMyReceipts(page = 0, size = 10): Observable<PagedResponse<any>> {
    return this.http.get<ApiResponse<PagedResponse<any>>>(`${this.base}/receipts/my`, {
      params: { page, size }
    }).pipe(map(r => r.data));
  }

  // Alias used by invoices and receipts components
  getMyPayments(page = 0, size = 20): Observable<PagedResponse<Payment>> {
    return this.getMyPaymentsAsDealer(page, size);
  }
}
