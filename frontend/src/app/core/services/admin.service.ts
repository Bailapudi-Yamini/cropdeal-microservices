import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@env/environment';
import { ApiResponse, PagedResponse, User } from '../models';

export interface DashboardStats {
  totalUsers: number;
  totalFarmers: number;
  totalDealers: number;
  totalOrders: number;
  totalRevenue: number;
  activeListings: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private base = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<ApiResponse<DashboardStats>>(`${this.base}/reports/dashboard`).pipe(map(r => r.data));
  }

  getUsers(page = 0, size = 20, role?: string): Observable<PagedResponse<User>> {
    let params: Record<string, string | number> = { page, size };
    if (role) params['role'] = role;
    return this.http.get<ApiResponse<PagedResponse<User>>>(`${this.base}/users`, { params }).pipe(map(r => r.data));
  }

  toggleUserStatus(userId: number): Observable<User> {
    return this.http.patch<ApiResponse<User>>(`${this.base}/users/${userId}/toggle`, {}).pipe(map(r => r.data));
  }

  exportOrdersExcel(): Observable<Blob> {
    return this.http.get(`${this.base}/reports/orders/export`, { responseType: 'blob' });
  }

  exportUsersExcel(): Observable<Blob> {
    return this.http.get(`${this.base}/reports/users/export`, { responseType: 'blob' });
  }
}
