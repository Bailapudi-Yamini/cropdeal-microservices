import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-green-100">
      <div class="card w-full max-w-md">
        <div class="text-center mb-8">
          <div class="text-4xl mb-2">🌾</div>
          <h1 class="text-2xl font-bold text-gray-900">Welcome to CropDeal</h1>
          <p class="text-gray-500 text-sm mt-1">Sign in to your account</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input formControlName="email" type="email" class="input-field" placeholder="you@example.com">
            @if (form.get('email')?.invalid && form.get('email')?.touched) {
              <p class="text-red-500 text-xs mt-1">Valid email required</p>
            }
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input formControlName="password" type="password" class="input-field" placeholder="••••••••">
            @if (form.get('password')?.invalid && form.get('password')?.touched) {
              <p class="text-red-500 text-xs mt-1">Password required</p>
            }
          </div>

          @if (error) {
            <div class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">{{ error }}</div>
          }

          <button type="submit" class="btn-primary w-full" [disabled]="loading">
            {{ loading ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>

        <p class="text-center text-sm text-gray-500 mt-6">
          Don't have an account? <a routerLink="/auth/register" class="text-primary-600 font-medium hover:underline">Register</a>
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  loading = false;
  error = '';

  constructor(private fb: FormBuilder, private auth: AuthService) {}

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.error = '';
    this.auth.login(this.form.getRawValue() as any).subscribe({
      next: () => this.auth.redirectByRole(),
      error: err => { this.error = err.error?.message ?? 'Login failed'; this.loading = false; }
    });
  }
}
