import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'cd_token';
  private readonly USER_KEY  = 'cd_user';

  private _user = signal<User | null>(this.loadUser());
  readonly user   = this._user.asReadonly();
  readonly role   = computed(() => this._user()?.role ?? null);
  readonly isAuth = computed(() => !!this._user());

  constructor(private http: HttpClient, private router: Router) {}

  login(req: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/login`, req).pipe(
      tap(res => this.persist(res.data))
    );
  }

  register(req: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/register`, req).pipe(
      tap(res => this.persist(res.data))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this._user.set(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  redirectByRole(): void {
    const role = this.role();
    if (role === 'FARMER') this.router.navigate(['/farmer']);
    else if (role === 'DEALER') this.router.navigate(['/dealer']);
    else if (role === 'ADMIN') this.router.navigate(['/admin']);
    else this.router.navigate(['/auth/login']);
  }

  private persist(data: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, data.accessToken);
    // Backend returns flat fields — construct the User object from them
    const user: User = {
      id: data.userId ?? (data as any).user?.id,
      name: data.name ?? (data as any).user?.name,
      email: data.email ?? (data as any).user?.email,
      role: data.role ?? (data as any).user?.role,
      active: true
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this._user.set(user);
  }

  private loadUser(): User | null {
    try {
      const raw = localStorage.getItem(this.USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }
}

interface ApiResponse<T> { success: boolean; message: string; data: T; }
