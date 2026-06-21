import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { RazorpayService } from '../../core/services/razorpay.service';
import { AuthService } from '../../core/services/auth.service';
import { Order, Payment, Negotiation } from '../../core/models';
import { NegotiationModalComponent } from '../../shared/negotiation-modal/negotiation-modal.component';

@Component({
  selector: 'app-dealer-orders',
  standalone: true,
  imports: [CommonModule, NegotiationModalComponent],
  template: `
    <!-- Toast -->
    @if (toast()) {
      <div [class]="toastType() === 'success' ? 'bg-green-600' : 'bg-red-600'"
           class="fixed top-4 right-4 z-50 px-5 py-3 rounded-lg shadow-lg text-white text-sm font-medium animate-fade-in">
        {{ toast() }}
      </div>
    }

    <!-- Negotiation modal -->
    <app-negotiation-modal
      [orderId]="modalOrderId()"
      [cropName]="modalCropName()"
      [currentPrice]="modalCurrentPrice()"
      [isOpen]="modalOpen()"
      (submitted)="onNegotiationSubmitted($event)"
      (closed)="modalOpen.set(false)" />

    <h1 class="text-2xl font-bold text-gray-900 mb-6">My Orders</h1>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else if (orders().length === 0) {
      <div class="card text-center py-16">
        <div class="text-5xl mb-4">📦</div>
        <p class="text-gray-500">No orders yet.
          <a href="/dealer/browse" class="text-green-600 underline">Browse crops</a>
        </p>
      </div>
    } @else {
      <div class="space-y-4">
        @for (order of orders(); track order.id) {
          <div class="card">

            <!-- Order header -->
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-3 mb-1 flex-wrap">
                  <h3 class="font-semibold text-gray-900">{{ order.cropName }}</h3>
                  <span [class]="statusBadge(order.status)">{{ statusLabel(order.status) }}</span>
                  @if (paymentMap()[order.id]?.status === 'SUCCESS') {
                    <span class="badge-green">✓ Paid</span>
                  }
                </div>
                <p class="text-sm text-gray-500">Farmer: {{ order.farmerName }} · Order #{{ order.id }}</p>
                <p class="text-sm text-gray-500">{{ order.createdAt | date:'mediumDate' }}</p>
              </div>
              <div class="text-right shrink-0">
                <p class="text-xl font-bold text-green-700">₹{{ order.totalAmount | number }}</p>
                <p class="text-sm text-gray-500">{{ order.quantity }} units &#64; ₹{{ order.pricePerUnit }}/unit</p>
                @if (latestNegotiation(order.id)) {
                  <p class="text-xs text-orange-600 font-medium mt-1">
                    Latest offer: ₹{{ latestNegotiation(order.id)!.proposedPrice | number }}/unit
                  </p>
                }
              </div>
            </div>

            <!-- Negotiation history thread -->
            @if (negotiationMap()[order.id]?.length) {
              <div class="mt-4 pt-4 border-t border-gray-100">
                <button
                  (click)="toggleHistory(order.id)"
                  class="text-xs text-gray-500 hover:text-gray-700 font-medium flex items-center gap-1 mb-3">
                  {{ historyOpen()[order.id] ? '▲ Hide' : '▼ Show' }}
                  negotiation history ({{ negotiationMap()[order.id].length }} rounds)
                </button>

                @if (historyOpen()[order.id]) {
                  <div class="space-y-2 max-h-64 overflow-y-auto pr-1">
                    @for (neg of negotiationMap()[order.id]; track neg.id) {
                      <div [class]="neg.proposedBy === 'DEALER'
                            ? 'ml-8 bg-green-50 border border-green-100'
                            : 'mr-8 bg-blue-50 border border-blue-100'"
                           class="rounded-lg px-4 py-3 text-sm">
                        <div class="flex items-center justify-between mb-1">
                          <span class="font-medium text-gray-700">
                            {{ neg.proposedBy === 'DEALER' ? '🤝 You' : '🌾 ' + order.farmerName }}
                          </span>
                          <span class="text-xs text-gray-400">{{ neg.createdAt | date:'shortTime' }}</span>
                        </div>
                        <p class="font-semibold text-gray-900">₹{{ neg.proposedPrice | number }}/unit</p>
                        @if (neg.message) {
                          <p class="text-gray-500 text-xs mt-1">{{ neg.message }}</p>
                        }
                        <span [class]="negStatusBadge(neg.status)" class="mt-1">{{ neg.status }}</span>
                      </div>
                    }
                  </div>
                }
              </div>
            }

            <!-- Action row -->
            <div class="flex gap-2 mt-4 pt-4 border-t border-gray-100 flex-wrap items-center">

              <!-- NEGOTIATING actions -->
              @if (order.status === 'NEGOTIATING') {
                <!-- Accept farmer's latest price -->
                @if (farmerOffered(order.id)) {
                  <button
                    (click)="accept(order)"
                    [disabled]="actioningId() === order.id"
                    class="btn-primary text-sm disabled:opacity-50">
                    ✓ Accept ₹{{ latestNegotiation(order.id)!.proposedPrice | number }}/unit
                  </button>
                }
                <!-- Counter offer -->
                <button
                  (click)="openModal(order)"
                  [disabled]="actioningId() === order.id"
                  class="btn-secondary text-sm">
                  ↩ Counter Offer
                </button>
                <!-- Load history if not loaded -->
                @if (!negotiationMap()[order.id]) {
                  <button (click)="loadHistory(order.id)" class="text-xs text-gray-400 hover:text-gray-600 underline">
                    Load history
                  </button>
                }
              }

              <!-- PENDING — can counter or cancel -->
              @if (order.status === 'PENDING') {
                <button (click)="openModal(order)" class="btn-secondary text-sm">↩ Counter Offer</button>
                <button (click)="cancel(order.id)" class="btn-danger text-sm">Cancel</button>
              }

              <!-- CONFIRMED — Pay Now -->
              @if (order.status === 'CONFIRMED' && paymentMap()[order.id]?.status !== 'SUCCESS') {
                @if (paymentMap()[order.id]?.razorpayOrderId) {
                  <button
                    (click)="payNow(order)"
                    [disabled]="razorpayService.processingOrderId() === order.id"
                    class="btn-primary text-sm flex items-center gap-2">
                    @if (razorpayService.processingOrderId() === order.id) {
                      <span class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                      Processing…
                    } @else {
                      💳 Pay Now ₹{{ order.totalAmount | number }}
                    }
                  </button>
                } @else {
                  <button (click)="loadPayment(order.id)" class="btn-secondary text-sm">🔄 Load Payment</button>
                }
              }

              @if (order.status === 'CONFIRMED' && paymentMap()[order.id]?.status === 'SUCCESS') {
                <span class="text-sm text-green-600 font-medium">✓ Payment complete</span>
              }

              @if (order.status === 'COMPLETED') {
                <span class="text-sm text-green-600 font-medium">✓ Order completed</span>
              }
            </div>

          </div>
        }
      </div>
    }
  `
})
export class DealerOrdersComponent implements OnInit {
  orders          = signal<Order[]>([]);
  paymentMap      = signal<Record<number, Payment>>({});
  negotiationMap  = signal<Record<number, Negotiation[]>>({});
  historyOpen     = signal<Record<number, boolean>>({});
  loading         = signal(true);
  actioningId     = signal<number | null>(null);
  toast           = signal<string | null>(null);
  toastType       = signal<'success' | 'error'>('success');

  // Modal state
  modalOpen         = signal(false);
  modalOrderId      = signal(0);
  modalCropName     = signal('');
  modalCurrentPrice = signal(0);

  constructor(
    private orderService: OrderService,
    private paymentService: PaymentService,
    readonly razorpayService: RazorpayService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.orderService.getMyOrders('dealer').subscribe({
      next: p => {
        this.orders.set(p.content);
        this.loading.set(false);
        p.content.forEach(o => {
          if (o.status === 'CONFIRMED') this.loadPayment(o.id);
          if (o.status === 'NEGOTIATING') this.loadHistory(o.id);
        });
      },
      error: () => this.loading.set(false)
    });
  }

  // ── Negotiation ────────────────────────────────────────────────────────────

  openModal(order: Order): void {
    const latest = this.latestNegotiation(order.id);
    this.modalOrderId.set(order.id);
    this.modalCropName.set(order.cropName);
    this.modalCurrentPrice.set(latest?.proposedPrice ?? order.pricePerUnit);
    this.modalOpen.set(true);
  }

  onNegotiationSubmitted(event: { orderId: number; negotiation: Negotiation }): void {
    this.negotiationMap.update(m => ({
      ...m,
      [event.orderId]: [...(m[event.orderId] ?? []), event.negotiation]
    }));
    this.orders.update(list =>
      list.map(o => o.id === event.orderId ? { ...o, status: 'NEGOTIATING' } : o)
    );
    this.showToast('Counter offer sent!', 'success');
  }

  loadHistory(orderId: number): void {
    this.orderService.getNegotiations(orderId).subscribe({
      next: list => this.negotiationMap.update(m => ({ ...m, [orderId]: list })),
      error: () => {}
    });
  }

  toggleHistory(orderId: number): void {
    this.historyOpen.update(m => ({ ...m, [orderId]: !m[orderId] }));
    if (!this.negotiationMap()[orderId]) this.loadHistory(orderId);
  }

  latestNegotiation(orderId: number): Negotiation | null {
    const list = this.negotiationMap()[orderId];
    return list?.length ? list[list.length - 1] : null;
  }

  /** True when the latest negotiation round was proposed by the farmer */
  farmerOffered(orderId: number): boolean {
    const latest = this.latestNegotiation(orderId);
    return latest?.proposedBy === 'FARMER';
  }

  accept(order: Order): void {
    this.actioningId.set(order.id);
    this.orderService.acceptOrder(order.id).subscribe({
      next: updated => {
        this.orders.update(list => list.map(o => o.id === order.id ? updated : o));
        this.actioningId.set(null);
        this.showToast('Price accepted — payment will be initiated shortly', 'success');
        this.loadPayment(order.id);
      },
      error: err => {
        this.actioningId.set(null);
        this.showToast(err.error?.message ?? 'Accept failed', 'error');
      }
    });
  }

  // ── Payment ────────────────────────────────────────────────────────────────

  loadPayment(orderId: number): void {
    this.paymentService.getPaymentByOrderId(orderId).subscribe({
      next: p => this.paymentMap.update(m => ({ ...m, [orderId]: p })),
      error: () => {}
    });
  }

  payNow(order: Order): void {
    const payment = this.paymentMap()[order.id];
    if (!payment?.razorpayOrderId) {
      this.showToast('Payment not ready yet. Try again in a moment.', 'error');
      return;
    }
    const user = this.authService.user();
    this.razorpayService.openCheckout({
      orderId: order.id,
      razorpayOrderId: payment.razorpayOrderId,
      amount: order.totalAmount,
      userName: user?.name ?? '',
      userEmail: user?.email ?? '',
      description: `Payment for ${order.cropName} — Order #${order.id}`
    }).subscribe({
      next: verified => {
        this.paymentMap.update(m => ({ ...m, [order.id]: verified }));
        this.orders.update(list =>
          list.map(o => o.id === order.id ? { ...o, status: 'COMPLETED' } : o)
        );
        this.showToast('Payment successful! Receipt generated.', 'success');
      },
      error: err => {
        if (err?.message !== 'Payment cancelled') {
          this.showToast(err?.message ?? 'Payment failed', 'error');
        }
      }
    });
  }

  // ── Cancel ─────────────────────────────────────────────────────────────────

  cancel(id: number): void {
    if (!confirm('Cancel this order?')) return;
    this.orderService.cancelOrder(id).subscribe({
      next: () => this.orders.update(list =>
        list.map(o => o.id === id ? { ...o, status: 'CANCELLED' } : o)
      ),
      error: err => this.showToast(err.error?.message ?? 'Cancel failed', 'error')
    });
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'badge-yellow', NEGOTIATING: 'badge-yellow',
      CONFIRMED: 'badge-blue', COMPLETED: 'badge-green', CANCELLED: 'badge-red'
    };
    return m[s] ?? 'badge-yellow';
  }

  statusLabel(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'Pending', NEGOTIATING: 'Negotiating',
      CONFIRMED: 'Confirmed — Pay Now', COMPLETED: 'Completed', CANCELLED: 'Cancelled'
    };
    return m[s] ?? s;
  }

  negStatusBadge(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'badge-yellow', ACCEPTED: 'badge-green',
      REJECTED: 'badge-red', COUNTERED: 'badge-blue'
    };
    return m[s] ?? 'badge-yellow';
  }

  private showToast(msg: string, type: 'success' | 'error'): void {
    this.toast.set(msg);
    this.toastType.set(type);
    setTimeout(() => this.toast.set(null), 4000);
  }
}
