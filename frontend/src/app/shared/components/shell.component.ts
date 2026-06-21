import { Component, Input } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';

export interface NavItem { label: string; route: string; icon: string; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, NotificationBellComponent],
  template: `
    <div class="flex h-screen bg-gray-50">
      <!-- Sidebar -->
      <aside class="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div class="p-5 border-b border-gray-100">
          <span class="text-xl font-bold text-primary-700">🌾 CropDeal</span>
          <p class="text-xs text-gray-500 mt-1">{{ portalLabel }}</p>
        </div>
        <nav class="flex-1 p-4 space-y-1">
          @for (item of navItems; track item.route) {
            <a [routerLink]="item.route" routerLinkActive="bg-primary-50 text-primary-700 font-medium"
               class="flex items-center gap-3 px-3 py-2 rounded-lg text-gray-600 hover:bg-gray-50 transition-colors text-sm">
              <span>{{ item.icon }}</span>{{ item.label }}
            </a>
          }
        </nav>
        <div class="p-4 border-t border-gray-100">
          <div class="text-sm text-gray-700 font-medium mb-1">{{ auth.user()?.name }}</div>
          <div class="text-xs text-gray-400 mb-3">{{ auth.user()?.email }}</div>
          <button (click)="auth.logout()" class="btn-secondary w-full text-sm">Sign out</button>
        </div>
      </aside>

      <!-- Main -->
      <main class="flex-1 overflow-auto flex flex-col">
        <!-- Top bar -->
        <header class="bg-white border-b border-gray-100 px-8 py-3 flex items-center justify-end shrink-0">
          <app-notification-bell />
        </header>
        <div class="p-8 flex-1">
          <router-outlet />
        </div>
      </main>
    </div>
  `
})
export class ShellComponent {
  @Input() navItems: NavItem[] = [];
  @Input() portalLabel = '';
  constructor(readonly auth: AuthService) {}
}
