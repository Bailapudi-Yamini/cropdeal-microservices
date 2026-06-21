import { Routes } from '@angular/router';
import { DealerShellComponent } from './dealer-shell.component';

export const dealerRoutes: Routes = [
  {
    path: '',
    component: DealerShellComponent,
    children: [
      { path: '', redirectTo: 'browse', pathMatch: 'full' },
      { path: 'browse',   loadComponent: () => import('./browse/browse-crops.component').then(m => m.BrowseCropsComponent) },
      { path: 'orders',   loadComponent: () => import('./orders/dealer-orders.component').then(m => m.DealerOrdersComponent) },
      { path: 'invoices', loadComponent: () => import('./invoices/invoices.component').then(m => m.InvoicesComponent) },
    ]
  }
];
