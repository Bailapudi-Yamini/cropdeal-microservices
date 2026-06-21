import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@env/environment';
import { ApiResponse, Order, Negotiation, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private base = `${environment.apiUrl}/orders`;

  constructor(private http: HttpClient) {}

  getMyOrders(role: 'farmer' | 'dealer', page = 0, size = 10): Observable<PagedResponse<Order>> {
    return this.http.get<ApiResponse<PagedResponse<Order>>>(`${this.base}/${role}`, {
      params: { page, size }
    }).pipe(map(r => r.data));
  }

  getById(id: number): Observable<Order> {
    return this.http.get<ApiResponse<Order>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  placeOrder(cropListingId: number, quantity: number): Observable<Order> {
    return this.http.post<ApiResponse<Order>>(this.base, { cropListingId, quantity }).pipe(map(r => r.data));
  }

  confirmOrder(id: number): Observable<Order> {
    return this.http.post<ApiResponse<Order>>(`${this.base}/${id}/confirm`, {}).pipe(map(r => r.data));
  }

  negotiate(id: number, proposedPrice: number, message: string): Observable<Negotiation> {
    return this.http.post<ApiResponse<Negotiation>>(`${this.base}/${id}/negotiate`, { proposedPrice, message })
      .pipe(map(r => r.data));
  }

  getNegotiations(id: number): Observable<Negotiation[]> {
    return this.http.get<ApiResponse<Negotiation[]>>(`${this.base}/${id}/negotiations`)
      .pipe(map(r => r.data));
  }

  acceptOrder(id: number): Observable<Order> {
    return this.http.put<ApiResponse<Order>>(`${this.base}/${id}/accept`, {}).pipe(map(r => r.data));
  }

  rejectOrder(id: number): Observable<Order> {
    return this.http.put<ApiResponse<Order>>(`${this.base}/${id}/reject`, {}).pipe(map(r => r.data));
  }

  cancelOrder(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  getAllOrders(page = 0, size = 20): Observable<PagedResponse<Order>> {
    return this.http.get<ApiResponse<PagedResponse<Order>>>(`${this.base}/admin`, {
      params: { page, size }
    }).pipe(map(r => r.data));
  }
}
