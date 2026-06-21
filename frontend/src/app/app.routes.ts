import { Routes } from '@angular/router';
import { authGuard, farmerGuard, dealerGuard, adminGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },

  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./auth/auth.routes').then(m => m.authRoutes)
  },
  {
    path: 'farmer',
    canActivate: [farmerGuard],
    loadChildren: () => import('./farmer/farmer.routes').then(m => m.farmerRoutes)
  },
  {
    path: 'dealer',
    canActivate: [dealerGuard],
    loadChildren: () => import('./dealer/dealer.routes').then(m => m.dealerRoutes)
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./admin/admin.routes').then(m => m.adminRoutes)
  },
  { path: '**', redirectTo: 'auth/login' }
];
