import {
  Component, EventEmitter, Input, OnChanges, Output, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../core/services/order.service';
import { Negotiation } from '../../core/models';

export interface NegotiationSubmitted {
  orderId: number;
  negotiation: Negotiation;
}

@Component({
  selector: 'app-negotiation-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (open()) {
      <!-- Backdrop -->
      <div class="fixed inset-0 bg-black/50 z-40" (click)="close()"></div>

      <!-- Modal -->
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-md" (click)="$event.stopPropagation()">

          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">Counter Offer</h2>
              <p class="text-sm text-gray-500">Order #{{ orderId }} · {{ cropName }}</p>
            </div>
            <button (click)="close()" class="text-gray-400 hover:text-gray-600 text-xl leading-none">✕</button>
          </div>

          <!-- Current price context -->
          <div class="px-6 pt-4 pb-2">
            <div class="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3 text-sm">
              <span class="text-gray-500">Current price</span>
              <span class="font-semibold text-gray-900">₹{{ currentPrice | number }} / unit</span>
            </div>
          </div>

          <!-- Form -->
          <form (ngSubmit)="submit()" #f="ngForm" class="px-6 pb-6 pt-3 space-y-4">

            <!-- Proposed price -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                Your proposed price <span class="text-red-500">*</span>
              </label>
              <div class="relative">
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">₹</span>
                <input
                  type="number"
                  name="price"
                  [(ngModel)]="price"
                  required
                  min="1"
                  step="0.01"
                  placeholder="0.00"
                  class="input-field pl-7"
                  [class.border-red-400]="priceError()" />
              </div>
              @if (priceError()) {
                <p class="text-xs text-red-500 mt-1">{{ priceError() }}</p>
              }
              @if (price > 0 && price !== currentPrice) {
                <p class="text-xs mt-1" [class]="price < currentPrice ? 'text-green-600' : 'text-orange-500'">
                  {{ price < currentPrice ? '▼' : '▲' }}
                  {{ diffPercent() }}% {{ price < currentPrice ? 'lower' : 'higher' }} than current price
                </p>
              }
            </div>

            <!-- Message -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Message <span class="text-gray-400">(optional)</span></label>
              <textarea
                name="message"
                [(ngModel)]="message"
                rows="3"
                placeholder="Explain your offer…"
                class="input-field resize-none"></textarea>
            </div>

            <!-- Actions -->
            <div class="flex gap-3 pt-1">
              <button type="button" (click)="close()" class="btn-secondary flex-1">Cancel</button>
              <button
                type="submit"
                [disabled]="submitting()"
                class="btn-primary flex-1 flex items-center justify-center gap-2">
                @if (submitting()) {
                  <span class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  Sending…
                } @else {
                  Send Offer
                }
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class NegotiationModalComponent implements OnChanges {
  @Input() orderId!: number;
  @Input() cropName = '';
  @Input() currentPrice = 0;
  @Input() isOpen = false;

  @Output() submitted = new EventEmitter<NegotiationSubmitted>();
  @Output() closed    = new EventEmitter<void>();

  open       = signal(false);
  submitting = signal(false);
  priceError = signal<string | null>(null);

  price   = 0;
  message = '';

  constructor(private orderService: OrderService) {}

  ngOnChanges(): void {
    this.open.set(this.isOpen);
    if (this.isOpen) {
      this.price   = this.currentPrice;
      this.message = '';
      this.priceError.set(null);
    }
  }

  diffPercent(): string {
    if (!this.currentPrice) return '0';
    return Math.abs(((this.price - this.currentPrice) / this.currentPrice) * 100).toFixed(1);
  }

  submit(): void {
    this.priceError.set(null);
    if (!this.price || this.price <= 0) {
      this.priceError.set('Price must be greater than 0');
      return;
    }
    if (this.price === this.currentPrice) {
      this.priceError.set('Proposed price must differ from the current price');
      return;
    }

    this.submitting.set(true);
    this.orderService.negotiate(this.orderId, this.price, this.message).subscribe({
      next: negotiation => {
        this.submitting.set(false);
        this.submitted.emit({ orderId: this.orderId, negotiation });
        this.close();
      },
      error: err => {
        this.submitting.set(false);
        this.priceError.set(err.error?.message ?? 'Failed to submit offer');
      }
    });
  }

  close(): void {
    this.open.set(false);
    this.closed.emit();
  }
}
