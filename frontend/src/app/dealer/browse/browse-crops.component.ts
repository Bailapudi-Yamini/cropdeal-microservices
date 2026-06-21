import { Component, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CropService } from '../../core/services/crop.service';
import { OrderService } from '../../core/services/order.service';
import { CropListing, PagedResponse } from '../../core/models';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-browse-crops',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Browse Crops</h1>

    <!-- Filters -->
    <div class="card mb-6">
      <form [formGroup]="filterForm" class="flex flex-wrap gap-4">
        <input formControlName="keyword" class="input-field flex-1 min-w-48" placeholder="🔍 Search crops…">
        <input formControlName="location" class="input-field w-48" placeholder="📍 Location">
        <select formControlName="cropType" class="input-field w-40">
          <option value="">All Types</option>
          @for (t of cropTypes; track t) { <option [value]="t">{{ t }}</option> }
        </select>
      </form>
    </div>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else if (page()?.content?.length === 0) {
      <div class="card text-center py-16">
        <div class="text-5xl mb-4">🌾</div>
        <p class="text-gray-500">No crops found matching your criteria.</p>
      </div>
    } @else {
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        @for (crop of page()?.content ?? []; track crop.id) {
          <div class="card hover:shadow-md transition-shadow">
            <div class="mb-3">
              <h3 class="font-semibold text-gray-900">{{ crop.cropName }}</h3>
              <p class="text-sm text-gray-500">{{ crop.cropType }} · {{ crop.farmerName }}</p>
            </div>
            <div class="space-y-1 text-sm text-gray-600 mb-4">
              <p>📍 {{ crop.location }}</p>
              <p>⚖️ {{ crop.quantityAvailable }} {{ crop.unit }} available</p>
              @if (crop.description) { <p class="text-xs text-gray-400 line-clamp-2">{{ crop.description }}</p> }
            </div>
            <div class="flex items-center justify-between">
              <span class="text-xl font-bold text-primary-700">₹{{ crop.pricePerUnit }}/{{ crop.unit }}</span>
              <button (click)="openOrder(crop)" class="btn-primary text-sm">Order Now</button>
            </div>
          </div>
        }
      </div>

      <!-- Pagination -->
      <div class="flex justify-center gap-2 mt-6">
        <button class="btn-secondary text-sm" [disabled]="currentPage() === 0" (click)="loadPage(currentPage() - 1)">← Prev</button>
        <span class="px-4 py-2 text-sm text-gray-600">Page {{ currentPage() + 1 }} of {{ page()?.totalPages }}</span>
        <button class="btn-secondary text-sm" [disabled]="currentPage() + 1 >= (page()?.totalPages ?? 1)" (click)="loadPage(currentPage() + 1)">Next →</button>
      </div>
    }

    <!-- Order Modal -->
    @if (selectedCrop()) {
      <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="card w-full max-w-md">
          <h2 class="text-lg font-bold mb-4">Place Order — {{ selectedCrop()!.cropName }}</h2>
          <p class="text-sm text-gray-600 mb-4">
            Price: <strong>₹{{ selectedCrop()!.pricePerUnit }}/{{ selectedCrop()!.unit }}</strong> ·
            Available: <strong>{{ selectedCrop()!.quantityAvailable }} {{ selectedCrop()!.unit }}</strong>
          </p>
          <form [formGroup]="orderForm" (ngSubmit)="placeOrder()" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Quantity ({{ selectedCrop()!.unit }})</label>
              <input formControlName="quantity" type="number" class="input-field"
                     [max]="selectedCrop()!.quantityAvailable" [min]="1">
              @if ((orderForm.get('quantity')?.value ?? 0) > (selectedCrop()?.quantityAvailable ?? 0)) {
                <p class="text-red-500 text-xs mt-1">
                  Only {{ selectedCrop()!.quantityAvailable }} {{ selectedCrop()!.unit }} available
                </p>
              }
            </div>
            <p class="text-sm font-semibold text-primary-700">
              Total: ₹{{ (orderForm.get('quantity')?.value ?? 0) * selectedCrop()!.pricePerUnit | number }}
            </p>
            @if (orderError()) {
              <div class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">{{ orderError() }}</div>
            }
            <div class="flex gap-3">
              <button type="submit" class="btn-primary flex-1"
                [disabled]="orderLoading() || orderForm.invalid">
                {{ orderLoading() ? 'Placing…' : 'Confirm Order' }}
              </button>
              <button type="button" class="btn-secondary flex-1" (click)="selectedCrop.set(null)">Cancel</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class BrowseCropsComponent implements OnInit {
  cropTypes = ['VEGETABLE','FRUIT','GRAIN','OTHER'];

  page        = signal<PagedResponse<CropListing> | null>(null);
  loading     = signal(true);
  currentPage = signal(0);
  selectedCrop = signal<CropListing | null>(null);
  orderLoading = signal(false);
  orderError   = signal('');

  filterForm = this.fb.group({ keyword: [''], location: [''], cropType: [''] });
  orderForm: FormGroup = this.fb.group({ quantity: [1] });

  constructor(
    private fb: FormBuilder,
    private cropService: CropService,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.loadPage(0);
    this.filterForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(() => { this.loading.set(true); return this.fetchCrops(0); })
    ).subscribe(p => { this.page.set(p); this.currentPage.set(0); this.loading.set(false); });
  }

  loadPage(p: number): void {
    this.loading.set(true);
    this.fetchCrops(p).subscribe(data => {
      this.page.set(data); this.currentPage.set(p); this.loading.set(false);
    });
  }

  private fetchCrops(p: number) {
    const { keyword, location, cropType } = this.filterForm.value;
    if (keyword) return this.cropService.search(keyword, p);
    return this.cropService.getAvailable(p, 9, cropType || undefined, location || undefined);
  }

  openOrder(crop: CropListing): void {
    this.selectedCrop.set(crop);
    this.orderForm = this.fb.group({
      quantity: [1, [Validators.required, Validators.min(1), Validators.max(crop.quantityAvailable)]]
    });
    this.orderError.set('');
  }

  placeOrder(): void {
    const qty = this.orderForm.get('quantity')?.value;
    if (!qty || qty < 1) return;
    this.orderLoading.set(true);
    this.orderService.placeOrder(this.selectedCrop()!.id, qty).subscribe({
      next: () => { this.selectedCrop.set(null); this.orderLoading.set(false); },
      error: err => { this.orderError.set(err.error?.message ?? 'Order failed'); this.orderLoading.set(false); }
    });
  }
}
