import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin-shell.component';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./dashboard.component').then(m => m.AdminDashboardComponent) },
      { path: 'users',     loadComponent: () => import('./users/users.component').then(m => m.UsersComponent) },
      { path: 'reports',   loadComponent: () => import('./reports/reports.component').then(m => m.ReportsComponent) },
    ]
  }
];
