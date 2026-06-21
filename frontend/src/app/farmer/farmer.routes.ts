import { Routes } from '@angular/router';
import { FarmerShellComponent } from './farmer-shell.component';

export const farmerRoutes: Routes = [
  {
    path: '',
    component: FarmerShellComponent,
    children: [
      { path: '', redirectTo: 'crops', pathMatch: 'full' },
      { path: 'crops',    loadComponent: () => import('./crops/my-crops.component').then(m => m.MyCropsComponent) },
      { path: 'crops/new', loadComponent: () => import('./crops/crop-form.component').then(m => m.CropFormComponent) },
      { path: 'crops/:id/edit', loadComponent: () => import('./crops/crop-form.component').then(m => m.CropFormComponent) },
      { path: 'orders',   loadComponent: () => import('./orders/farmer-orders.component').then(m => m.FarmerOrdersComponent) },
      { path: 'receipts', loadComponent: () => import('./receipts/receipts.component').then(m => m.ReceiptsComponent) },
    ]
  }
];
