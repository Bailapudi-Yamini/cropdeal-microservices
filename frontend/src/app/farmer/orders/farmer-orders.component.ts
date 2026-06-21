import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../core/services/order.service';
import { Order, Negotiation } from '../../core/models';
import { NegotiationModalComponent } from '../../shared/negotiation-modal/negotiation-modal.component';

@Component({
  selector: 'app-farmer-orders',
  standalone: true,
  imports: [CommonModule, NegotiationModalComponent],
  template: `
    <!-- Toast -->
    @if (toast()) {
      <div [class]="toastType() === 'success' ? 'bg-green-600' : 'bg-red-600'"
           class="fixed top-4 right-4 z-50 px-5 py-3 rounded-lg shadow-lg text-white text-sm font-medium">
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

    <h1 class="text-2xl font-bold text-gray-900 mb-6">Incoming Orders</h1>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else if (orders().length === 0) {
      <div class="card text-center py-16">
        <div class="text-5xl mb-4">📦</div>
        <p class="text-gray-500">No orders yet.</p>
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
                </div>
                <p class="text-sm text-gray-500">Dealer: {{ order.dealerName }} · Order #{{ order.id }}</p>
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
                      <div [class]="neg.proposedBy === 'FARMER'
                            ? 'ml-8 bg-green-50 border border-green-100'
                            : 'mr-8 bg-blue-50 border border-blue-100'"
                           class="rounded-lg px-4 py-3 text-sm">
                        <div class="flex items-center justify-between mb-1">
                          <span class="font-medium text-gray-700">
                            {{ neg.proposedBy === 'FARMER' ? '🌾 You' : '🤝 ' + order.dealerName }}
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

              <!-- PENDING — Confirm or Counter -->
              @if (order.status === 'PENDING') {
                <button
                  (click)="accept(order)"
                  [disabled]="actioningId() === order.id"
                  class="bg-green-600 hover:bg-green-700 text-white text-sm font-medium py-2 px-4 rounded-lg transition-colors disabled:opacity-50">
                  @if (actioningId() === order.id) { … } @else { ✓ Confirm Order }
                </button>
                <button
                  (click)="openModal(order)"
                  [disabled]="actioningId() === order.id"
                  class="btn-secondary text-sm">
                  ↩ Counter Offer
                </button>
              }

              <!-- NEGOTIATING — Accept dealer's latest or Counter -->
              @if (order.status === 'NEGOTIATING') {
                @if (dealerOffered(order.id)) {
                  <button
                    (click)="accept(order)"
                    [disabled]="actioningId() === order.id"
                    class="bg-green-600 hover:bg-green-700 text-white text-sm font-medium py-2 px-4 rounded-lg transition-colors disabled:opacity-50">
                    ✓ Accept ₹{{ latestNegotiation(order.id)!.proposedPrice | number }}/unit
                  </button>
                }
                <button
                  (click)="openModal(order)"
                  [disabled]="actioningId() === order.id"
                  class="btn-secondary text-sm">
                  ↩ Counter Offer
                </button>
                <button
                  (click)="reject(order.id)"
                  [disabled]="actioningId() === order.id"
                  class="btn-danger text-sm">
                  ✕ Reject
                </button>
                @if (!negotiationMap()[order.id]) {
                  <button (click)="loadHistory(order.id)" class="text-xs text-gray-400 hover:text-gray-600 underline">
                    Load history
                  </button>
                }
              }

              <!-- CONFIRMED — waiting for payment -->
              @if (order.status === 'CONFIRMED') {
                <span class="text-sm text-blue-600 font-medium">⏳ Awaiting dealer payment</span>
              }

              <!-- COMPLETED -->
              @if (order.status === 'COMPLETED') {
                <span class="text-sm text-green-600 font-medium">✓ Payment received</span>
              }
            </div>

          </div>
        }
      </div>
    }
  `
})
export class FarmerOrdersComponent implements OnInit {
  orders         = signal<Order[]>([]);
  negotiationMap = signal<Record<number, Negotiation[]>>({});
  historyOpen    = signal<Record<number, boolean>>({});
  loading        = signal(true);
  actioningId    = signal<number | null>(null);
  toast          = signal<string | null>(null);
  toastType      = signal<'success' | 'error'>('success');

  // Modal state
  modalOpen         = signal(false);
  modalOrderId      = signal(0);
  modalCropName     = signal('');
  modalCurrentPrice = signal(0);

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.orderService.getMyOrders('farmer').subscribe({
      next: p => {
        this.orders.set(p.content);
        this.loading.set(false);
        p.content
          .filter(o => o.status === 'NEGOTIATING')
          .forEach(o => this.loadHistory(o.id));
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
    this.showToast('Counter offer sent to dealer!', 'success');
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

  /** True when the latest round was proposed by the dealer — farmer can accept it */
  dealerOffered(orderId: number): boolean {
    return this.latestNegotiation(orderId)?.proposedBy === 'DEALER';
  }

  accept(order: Order): void {
    this.actioningId.set(order.id);
    const call$ = order.status === 'PENDING'
      ? this.orderService.confirmOrder(order.id)   // POST /orders/{id}/confirm
      : this.orderService.acceptOrder(order.id);   // PUT  /orders/{id}/accept
    const msg = order.status === 'PENDING'
      ? 'Order confirmed — awaiting dealer payment'
      : 'Offer accepted — awaiting dealer payment';
    call$.subscribe({
      next: (updated: Order) => {
        this.orders.update(list => list.map(o => o.id === order.id ? updated : o));
        this.actioningId.set(null);
        this.showToast(msg, 'success');
      },
      error: (err: any) => {
        this.actioningId.set(null);
        this.showToast(err.error?.message ?? 'Action failed', 'error');
      }
    });
  }

  reject(id: number): void {
    if (!confirm('Reject and cancel this order?')) return;
    this.actioningId.set(id);
    this.orderService.rejectOrder(id).subscribe({
      next: updated => {
        this.orders.update(list => list.map(o => o.id === id ? updated : o));
        this.actioningId.set(null);
        this.showToast('Order rejected', 'success');
      },
      error: err => {
        this.actioningId.set(null);
        this.showToast(err.error?.message ?? 'Reject failed', 'error');
      }
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
      PENDING: 'New Order', NEGOTIATING: 'Negotiating',
      CONFIRMED: 'Confirmed', COMPLETED: 'Completed', CANCELLED: 'Cancelled'
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
