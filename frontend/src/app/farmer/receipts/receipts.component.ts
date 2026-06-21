import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentService } from '../../core/services/payment.service';
import { Payment } from '../../core/models';

@Component({
  selector: 'app-receipts',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Payment Receipts</h1>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else if (payments().length === 0) {
      <div class="card text-center py-16">
        <div class="text-5xl mb-4">🧾</div>
        <p class="text-gray-500">No receipts yet.</p>
      </div>
    } @else {
      <div class="space-y-3">
        @for (p of payments(); track p.id) {
          <div class="card flex items-center justify-between">
            <div>
              <p class="font-medium text-gray-900">Order #{{ p.orderId }}</p>
              <p class="text-sm text-gray-500">{{ p.createdAt | date:'mediumDate' }}</p>
              @if (p.transactionId) {
                <p class="text-xs text-gray-400">TXN: {{ p.transactionId }}</p>
              }
            </div>
            <div class="text-right">
              <p class="text-xl font-bold text-primary-700">₹{{ p.amount | number }}</p>
              <span [class]="statusBadge(p.status)">{{ p.status }}</span>
            </div>
          </div>
        }
      </div>
    }
  `
})
export class ReceiptsComponent implements OnInit {
  payments = signal<Payment[]>([]);
  loading  = signal(true);

  constructor(private paymentService: PaymentService) {}

  ngOnInit(): void {
    this.paymentService.getMyPaymentsAsFarmer().subscribe({
      next: (p: any) => { this.payments.set(p.content); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  statusBadge(s: string): string {
    const map: Record<string, string> = {
      SUCCESS: 'badge-green', PENDING: 'badge-yellow', FAILED: 'badge-red', REFUNDED: 'badge-blue'
    };
    return map[s] ?? 'badge-yellow';
  }
}
