import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-green-100 py-10">
      <div class="card w-full max-w-md">
        <div class="text-center mb-8">
          <div class="text-4xl mb-2">🌾</div>
          <h1 class="text-2xl font-bold text-gray-900">Create Account</h1>
          <p class="text-gray-500 text-sm mt-1">Join CropDeal today</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
            <input formControlName="name" type="text" class="input-field" placeholder="John Doe">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input formControlName="email" type="email" class="input-field" placeholder="you@example.com">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input formControlName="password" type="password" class="input-field" placeholder="Min 8 characters">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Phone</label>
            <input formControlName="phone" type="tel" class="input-field" placeholder="+91 9876543210">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Location</label>
            <input formControlName="location" type="text" class="input-field" placeholder="City, State">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">I am a</label>
            <select formControlName="role" class="input-field">
              <option value="FARMER">Farmer</option>
              <option value="DEALER">Dealer</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          @if (error) {
            <div class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">{{ error }}</div>
          }

          <button type="submit" class="btn-primary w-full" [disabled]="loading">
            {{ loading ? 'Creating account…' : 'Create Account' }}
          </button>
        </form>

        <p class="text-center text-sm text-gray-500 mt-6">
          Already have an account? <a routerLink="/auth/login" class="text-primary-600 font-medium hover:underline">Sign in</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent {
  form = this.fb.group({
    name:     ['', Validators.required],
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    phone:    [''],
    location: [''],
    role:     ['FARMER', Validators.required]
  });
  loading = false;
  error = '';

  constructor(private fb: FormBuilder, private auth: AuthService) {}

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.error = '';
    this.auth.register(this.form.getRawValue() as any).subscribe({
      next: () => this.auth.redirectByRole(),
      error: err => { this.error = err.error?.message ?? 'Registration failed'; this.loading = false; }
    });
  }
}
