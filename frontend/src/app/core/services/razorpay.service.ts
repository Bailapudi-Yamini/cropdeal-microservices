import { Injectable, signal } from '@angular/core';
import { PaymentService } from './payment.service';
import { Payment } from '../models';
import { Observable, from, switchMap, throwError } from 'rxjs';

export interface CheckoutOptions {
  orderId: number;
  razorpayOrderId: string;
  amount: number;       // in rupees — converted to paise internally
  userName: string;
  userEmail: string;
  description?: string;
}

declare const Razorpay: any;

@Injectable({ providedIn: 'root' })
export class RazorpayService {
  /** Tracks which orderId is currently processing payment to disable the button */
  readonly processingOrderId = signal<number | null>(null);

  private get key(): string {
    return (window as any).__RAZORPAY_KEY__ ?? '';
  }

  constructor(private paymentService: PaymentService) {}

  /**
   * Opens Razorpay checkout and returns an Observable<Payment> that emits
   * the verified Payment on success, or errors on failure/dismiss.
   */
  openCheckout(opts: CheckoutOptions): Observable<Payment> {
    return new Observable(observer => {
      this.processingOrderId.set(opts.orderId);

      const options = {
        key: this.key,
        amount: Math.round(opts.amount * 100),   // paise
        currency: 'INR',
        name: 'CropDeal',
        description: opts.description ?? 'Crop Purchase Payment',
        order_id: opts.razorpayOrderId,
        prefill: { name: opts.userName, email: opts.userEmail },
        theme: { color: '#22c55e' },
        handler: (response: any) => {
          // Razorpay calls this on successful payment
          this.paymentService.verifyPayment({
            orderId: opts.orderId,
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature
          }).subscribe({
            next: payment => {
              this.processingOrderId.set(null);
              observer.next(payment);
              observer.complete();
            },
            error: err => {
              this.processingOrderId.set(null);
              observer.error(err);
            }
          });
        },
        modal: {
          ondismiss: () => {
            this.processingOrderId.set(null);
            observer.error(new Error('Payment cancelled'));
          }
        }
      };

      try {
        const rzp = new Razorpay(options);
        rzp.on('payment.failed', (resp: any) => {
          this.processingOrderId.set(null);
          observer.error(new Error(resp.error?.description ?? 'Payment failed'));
        });
        rzp.open();
      } catch (e) {
        this.processingOrderId.set(null);
        observer.error(new Error('Razorpay failed to load. Check your internet connection.'));
      }
    });
  }
}
