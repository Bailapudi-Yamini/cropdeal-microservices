import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuth()) return true;
  return router.createUrlTree(['/auth/login']);
};

export const farmerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuth() && auth.role() === 'FARMER') return true;
  return router.createUrlTree(['/auth/login']);
};

export const dealerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuth() && auth.role() === 'DEALER') return true;
  return router.createUrlTree(['/auth/login']);
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuth() && auth.role() === 'ADMIN') return true;
  return router.createUrlTree(['/auth/login']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.isAuth()) return true;
  inject(AuthService).redirectByRole();
  return false;
};
