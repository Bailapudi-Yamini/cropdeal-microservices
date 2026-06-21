import { Component } from '@angular/core';
import { ShellComponent, NavItem } from '../shared/components/shell.component';

@Component({
  selector: 'app-farmer-shell',
  standalone: true,
  imports: [ShellComponent],
  template: `<app-shell portalLabel="Farmer Portal" [navItems]="nav" />`
})
export class FarmerShellComponent {
  nav: NavItem[] = [
    { label: 'My Crops',  route: '/farmer/crops',    icon: '🌱' },
    { label: 'Orders',    route: '/farmer/orders',   icon: '📦' },
    { label: 'Receipts',  route: '/farmer/receipts', icon: '🧾' },
  ];
}
