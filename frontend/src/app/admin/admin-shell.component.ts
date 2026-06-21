import { Component } from '@angular/core';
import { ShellComponent, NavItem } from '../shared/components/shell.component';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [ShellComponent],
  template: `<app-shell portalLabel="Admin Panel" [navItems]="nav" />`
})
export class AdminShellComponent {
  nav: NavItem[] = [
    { label: 'Dashboard', route: '/admin/dashboard', icon: '📊' },
    { label: 'Users',     route: '/admin/users',     icon: '👥' },
    { label: 'Reports',   route: '/admin/reports',   icon: '📈' },
  ];
}
