import { Component } from '@angular/core';
import { ShellComponent, NavItem } from '../shared/components/shell.component';

@Component({
  selector: 'app-dealer-shell',
  standalone: true,
  imports: [ShellComponent],
  template: `<app-shell portalLabel="Dealer Portal" [navItems]="nav" />`
})
export class DealerShellComponent {
  nav: NavItem[] = [
    { label: 'Browse Crops', route: '/dealer/browse',   icon: '🔍' },
    { label: 'My Orders',    route: '/dealer/orders',   icon: '📦' },
    { label: 'Invoices',     route: '/dealer/invoices', icon: '🧾' },
  ];
}
