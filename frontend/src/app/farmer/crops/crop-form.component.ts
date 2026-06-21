import { Component, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CropService } from '../../core/services/crop.service';

const CROP_TYPES = ['WHEAT', 'RICE', 'MAIZE', 'COTTON', 'SUGARCANE', 'SOYBEAN', 'PULSES', 'VEGETABLE', 'FRUIT', 'GRAIN', 'OTHER'];

@Component({
  selector: 'app-crop-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="max-w-2xl mx-auto">
      <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ isEdit() ? 'Edit Listing' : 'New Crop Listing' }}</h1>

      <div class="card">
        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-5">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Crop Name</label>
              <input formControlName="cropName" class="input-field" placeholder="e.g. Basmati Rice">
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Crop Type</label>
              <select formControlName="cropType" class="input-field">
                @for (t of cropTypes; track t) { <option [value]="t">{{ t }}</option> }
              </select>
            </div>
          </div>

          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
              <input formControlName="quantityAvailable" type="number" class="input-field" placeholder="100">
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Unit</label>
              <select formControlName="unit" class="input-field">
                <option>kg</option><option>quintal</option><option>ton</option><option>bag</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Price per Unit (₹)</label>
              <input formControlName="pricePerUnit" type="number" class="input-field" placeholder="2500">
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Location</label>
            <input formControlName="location" class="input-field" placeholder="City, State">
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Description (optional)</label>
            <textarea formControlName="description" rows="3" class="input-field" placeholder="Quality details, harvest date…"></textarea>
          </div>

          @if (error()) {
            <div class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">{{ error() }}</div>
          }

          <div class="flex gap-3">
            <button type="submit" class="btn-primary" [disabled]="loading()">
              {{ loading() ? 'Saving…' : (isEdit() ? 'Update Listing' : 'Create Listing') }}
            </button>
            <button type="button" class="btn-secondary" (click)="router.navigate(['/farmer/crops'])">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class CropFormComponent implements OnInit {
  cropTypes = CROP_TYPES;
  isEdit  = signal(false);
  loading = signal(false);
  error   = signal('');
  private editId?: number;

  form = this.fb.group({
    cropName:          ['', Validators.required],
    cropType:          ['GRAIN', Validators.required],
    quantityAvailable: [null as number | null, [Validators.required, Validators.min(0)]],
    unit:              ['kg', Validators.required],
    pricePerUnit:      [null as number | null, [Validators.required, Validators.min(1)]],
    location:          ['', Validators.required],
    description:       ['']
  });

  constructor(
    private fb: FormBuilder,
    private cropService: CropService,
    private route: ActivatedRoute,
    readonly router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      this.editId = +id;
      this.cropService.getById(this.editId).subscribe(c => this.form.patchValue(c as any));
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const payload = this.form.getRawValue() as any;
    const req$ = this.isEdit()
      ? this.cropService.update(this.editId!, payload)
      : this.cropService.create(payload);

    req$.subscribe({
      next: () => this.router.navigate(['/farmer/crops']),
      error: err => { this.error.set(err.error?.message ?? 'Save failed'); this.loading.set(false); }
    });
  }
}
