import {
  Component, OnInit, OnDestroy, HostListener, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { AppNotification } from '../../core/models';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative" #container>

      <!-- Bell button -->
      <button
        (click)="toggleDropdown()"
        class="relative p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition-colors focus:outline-none"
        aria-label="Notifications">
        <!-- Bell SVG -->
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <!-- Unread badge -->
        @if (unreadCount() > 0) {
          <span class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-red-500 text-white
                       text-[10px] font-bold rounded-full flex items-center justify-center px-1 leading-none">
            {{ unreadCount() > 99 ? '99+' : unreadCount() }}
          </span>
        }
      </button>

      <!-- Dropdown -->
      @if (open()) {
        <div class="absolute right-0 top-full mt-2 w-80 bg-white rounded-xl shadow-xl border border-gray-100 z-50 overflow-hidden">

          <!-- Header -->
          <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <span class="font-semibold text-gray-900 text-sm">Notifications</span>
            @if (unreadCount() > 0) {
              <button
                (click)="markAllRead()"
                [disabled]="markingAll()"
                class="text-xs text-green-600 hover:text-green-700 font-medium disabled:opacity-50">
                {{ markingAll() ? 'Marking…' : 'Mark all read' }}
              </button>
            }
          </div>

          <!-- List -->
          <div class="max-h-96 overflow-y-auto divide-y divide-gray-50">
            @if (notifications().length === 0) {
              <div class="py-10 text-center text-gray-400 text-sm">
                <div class="text-3xl mb-2">🔔</div>
                No notifications yet
              </div>
            }
            @for (n of notifications(); track n.id) {
              <button
                (click)="handleClick(n)"
                class="w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors flex gap-3 items-start"
                [class.bg-green-50]="!n.read">
                <!-- Type icon -->
                <span class="text-lg shrink-0 mt-0.5">{{ typeIcon(n.type) }}</span>
                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-2">
                    <p class="text-sm font-medium text-gray-900 leading-snug truncate">
                      {{ n.title }}
                    </p>
                    @if (!n.read) {
                      <span class="w-2 h-2 bg-green-500 rounded-full shrink-0 mt-1.5"></span>
                    }
                  </div>
                  <p class="text-xs text-gray-500 mt-0.5 line-clamp-2">{{ n.message }}</p>
                  <p class="text-xs text-gray-400 mt-1">{{ timeAgo(n.createdAt) }}</p>
                </div>
              </button>
            }
          </div>

          <!-- Footer -->
          @if (notifications().length > 0) {
            <div class="px-4 py-2 border-t border-gray-100 text-center">
              <span class="text-xs text-gray-400">Showing last {{ notifications().length }} notifications</span>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  notifications = signal<AppNotification[]>([]);
  unreadCount   = signal(0);
  open          = signal(false);
  markingAll    = signal(false);

  private subs: Subscription[] = [];

  constructor(private notifService: NotificationService) {}

  ngOnInit(): void {
    this.subs.push(
      this.notifService.notifications$.subscribe(n => this.notifications.set(n)),
      this.notifService.unreadCount$.subscribe(c => this.unreadCount.set(c))
    );
    this.notifService.startPolling();
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    // Do NOT stop polling here — shell persists across child route changes.
    // Polling is stopped by NotificationService.ngOnDestroy (app teardown)
    // or explicitly on logout via AuthService.
  }

  toggleDropdown(): void {
    this.open.update(v => !v);
  }

  /** Close dropdown when clicking outside */
  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent): void {
    if (!(e.target as HTMLElement).closest('app-notification-bell')) {
      this.open.set(false);
    }
  }

  handleClick(n: AppNotification): void {
    this.open.set(false);
    this.notifService.handleClick(n);
  }

  markAllRead(): void {
    this.markingAll.set(true);
    this.notifService.markAllRead().subscribe({
      next: () => this.markingAll.set(false),
      error: () => this.markingAll.set(false)
    });
  }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      CROP_POSTED:        '🌱',
      ORDER_PLACED:       '📦',
      NEGOTIATION_UPDATE: '🤝',
      PAYMENT_SUCCESS:    '✅',
      PAYMENT_FAILED:     '❌',
    };
    return map[type] ?? '🔔';
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins  = Math.floor(diff / 60_000);
    const hours = Math.floor(diff / 3_600_000);
    const days  = Math.floor(diff / 86_400_000);
    if (mins < 1)   return 'just now';
    if (mins < 60)  return `${mins} min ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7)   return `${days}d ago`;
    return new Date(iso).toLocaleDateString();
  }
}
