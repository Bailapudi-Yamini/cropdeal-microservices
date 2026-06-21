import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentService } from '../../core/services/payment.service';
import { RazorpayService } from '../../core/services/razorpay.service';
import { AuthService } from '../../core/services/auth.service';
import { Payment } from '../../core/models';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Invoices</h1>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else if (payments().length === 0) {
      <div class="card text-center py-16">
        <div class="text-5xl mb-4">🧾</div>
        <p class="text-gray-500">No invoices yet.</p>
      </div>
    } @else {
      <div class="card overflow-hidden p-0">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Invoice #</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Order</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Amount</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Status</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Date</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            @for (p of payments(); track p.id) {
              <tr class="hover:bg-gray-50">
                <td class="px-4 py-3 text-gray-500">INV-{{ p.id }}</td>
                <td class="px-4 py-3">#{{ p.orderId }}</td>
                <td class="px-4 py-3 font-semibold text-primary-700">₹{{ p.amount | number }}</td>
                <td class="px-4 py-3"><span [class]="statusBadge(p.status)">{{ p.status }}</span></td>
                <td class="px-4 py-3 text-gray-500">{{ p.createdAt | date:'mediumDate' }}</td>
                <td class="px-4 py-3">
                  @if (p.status === 'INITIATED' && p.razorpayOrderId) {
                    <button (click)="pay(p)" [disabled]="paying() === p.id"
                            class="btn-primary text-xs py-1 px-2">
                      {{ paying() === p.id ? 'Opening…' : 'Pay Now' }}
                    </button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }

    @if (errorMsg()) {
      <div class="mt-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
        {{ errorMsg() }}
      </div>
    }
  `
})
export class InvoicesComponent implements OnInit {
  payments = signal<Payment[]>([]);
  loading  = signal(true);
  paying   = signal<number | null>(null);
  errorMsg = signal('');

  constructor(
    private paymentService: PaymentService,
    private razorpayService: RazorpayService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.paymentService.getMyPayments().subscribe({
      next: p => { this.payments.set(p.content); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  pay(payment: Payment): void {
    this.paying.set(payment.id);
    this.errorMsg.set('');

    const user = this.auth.user()!;

    this.razorpayService.openCheckout({
      orderId: payment.orderId,
      razorpayOrderId: payment.razorpayOrderId!,
      amount: payment.amount,
      userName: user.name,
      userEmail: user.email,
      description: `Invoice INV-${payment.id} · Order #${payment.orderId}`
    }).subscribe({
      next: (updated: Payment) => {
        this.payments.update(list =>
          list.map(p => p.id === payment.id ? { ...p, ...updated } : p)
        );
        this.paying.set(null);
      },
      error: (err: any) => {
        if (err?.message !== 'Payment cancelled') {
          this.errorMsg.set(err?.message ?? 'Payment failed');
        }
        this.paying.set(null);
      }
    });
  }

  statusBadge(s: string): string {
    const map: Record<string, string> = {
      SUCCESS: 'badge-green', INITIATED: 'badge-yellow',
      FAILED: 'badge-red', REFUNDED: 'badge-blue'
    };
    return map[s] ?? 'badge-yellow';
  }
}
