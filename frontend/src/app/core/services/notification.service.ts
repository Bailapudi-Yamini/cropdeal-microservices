import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Subscription, interval, of } from 'rxjs';
import { switchMap, catchError, map, tap } from 'rxjs/operators';
import { environment } from '@env/environment';
import { ApiResponse, AppNotification, NotificationPage, NotificationType } from '../models';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private base = `${environment.apiUrl}/notifications`;

  private _notifications$ = new BehaviorSubject<AppNotification[]>([]);
  private _unreadCount$   = new BehaviorSubject<number>(0);
  private _pollSub?: Subscription;

  readonly notifications$ = this._notifications$.asObservable();
  readonly unreadCount$   = this._unreadCount$.asObservable();

  /** Navigation target per notification type and user role */
  private readonly navMap: Record<NotificationType, Record<string, string>> = {
    CROP_POSTED:        { DEALER: '/dealer/browse',  FARMER: '/farmer/crops',   ADMIN: '/admin/dashboard' },
    ORDER_PLACED:       { FARMER: '/farmer/orders',  DEALER: '/dealer/orders',  ADMIN: '/admin/dashboard' },
    NEGOTIATION_UPDATE: { FARMER: '/farmer/orders',  DEALER: '/dealer/orders',  ADMIN: '/admin/dashboard' },
    PAYMENT_SUCCESS:    { FARMER: '/farmer/orders',  DEALER: '/dealer/orders',  ADMIN: '/admin/dashboard' },
    PAYMENT_FAILED:     { DEALER: '/dealer/orders',  FARMER: '/farmer/orders',  ADMIN: '/admin/dashboard' },
  };

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private router: Router
  ) {}

  /** Call once after login — starts the 15-second polling loop */
  startPolling(): void {
    this.stopPolling();
    // Fetch immediately, then every 15 seconds
    this._pollSub = interval(15_000).pipe(
      switchMap(() => this.fetchPage()),
    ).subscribe();
    // First fetch right away
    this.fetchPage().subscribe();
  }

  stopPolling(): void {
    this._pollSub?.unsubscribe();
    this._pollSub = undefined;
  }

  private fetchPage() {
    return this.http
      .get<ApiResponse<NotificationPage>>(`${this.base}?page=0&size=10`)
      .pipe(
        tap(res => {
          this._notifications$.next(res.data.content);
          this._unreadCount$.next(res.data.unreadCount);
        }),
        catchError(() => of(null))
      );
  }

  markRead(id: number) {
    return this.http
      .patch<ApiResponse<void>>(`${this.base}/${id}/read`, {})
      .pipe(
        tap(() => {
          this._notifications$.next(
            this._notifications$.value.map(n => n.id === id ? { ...n, read: true } : n)
          );
          const current = this._unreadCount$.value;
          if (current > 0) this._unreadCount$.next(current - 1);
        })
      );
  }

  markAllRead() {
    return this.http
      .patch<ApiResponse<void>>(`${this.base}/read-all`, {})
      .pipe(
        tap(() => {
          this._notifications$.next(
            this._notifications$.value.map(n => ({ ...n, read: true }))
          );
          this._unreadCount$.next(0);
        })
      );
  }

  /** Mark as read then navigate to the relevant page */
  handleClick(notification: AppNotification): void {
    const role = this.auth.user()?.role ?? 'DEALER';
    const target = this.navMap[notification.type]?.[role] ?? '/';

    if (!notification.read) {
      this.markRead(notification.id).subscribe();
    }
    this.router.navigateByUrl(target);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }
}
