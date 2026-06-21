import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../core/services/admin.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Reports & Exports</h1>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div class="card">
        <div class="text-3xl mb-3">📦</div>
        <h2 class="text-lg font-semibold text-gray-900 mb-1">Orders Report</h2>
        <p class="text-sm text-gray-500 mb-4">Export all orders with status, amounts, and farmer/dealer details.</p>
        <button class="btn-primary" [disabled]="exportingOrders()" (click)="exportOrders()">
          {{ exportingOrders() ? 'Exporting…' : '⬇ Export Excel' }}
        </button>
      </div>

      <div class="card">
        <div class="text-3xl mb-3">👥</div>
        <h2 class="text-lg font-semibold text-gray-900 mb-1">Users Report</h2>
        <p class="text-sm text-gray-500 mb-4">Export all registered users with roles and account status.</p>
        <button class="btn-primary" [disabled]="exportingUsers()" (click)="exportUsers()">
          {{ exportingUsers() ? 'Exporting…' : '⬇ Export Excel' }}
        </button>
      </div>
    </div>

    @if (message()) {
      <div class="mt-6 bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg px-4 py-3">
        {{ message() }}
      </div>
    }
  `
})
export class ReportsComponent {
  exportingOrders = signal(false);
  exportingUsers  = signal(false);
  message         = signal('');

  constructor(private adminService: AdminService) {}

  exportOrders(): void {
    this.exportingOrders.set(true);
    this.adminService.exportOrdersExcel().subscribe({
      next: blob => { this.download(blob, 'orders-report.xlsx'); this.exportingOrders.set(false); this.message.set('Orders exported successfully.'); },
      error: () => { this.exportingOrders.set(false); this.message.set('Export failed. Please try again.'); }
    });
  }

  exportUsers(): void {
    this.exportingUsers.set(true);
    this.adminService.exportUsersExcel().subscribe({
      next: blob => { this.download(blob, 'users-report.xlsx'); this.exportingUsers.set(false); this.message.set('Users exported successfully.'); },
      error: () => { this.exportingUsers.set(false); this.message.set('Export failed. Please try again.'); }
    });
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }
}
