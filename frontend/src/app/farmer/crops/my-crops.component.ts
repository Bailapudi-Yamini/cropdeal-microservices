import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CropService } from '../../core/services/crop.service';
import { CropListing } from '../../core/models';

@Component({
  selector: 'app-my-crops',
  standalone: true,
  imports: [RouterLink, CommonModule],
  template: `
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">My Crop Listings</h1>
      <a routerLink="/farmer/crops/new" class="btn-primary">+ New Listing</a>
    </div>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else if (crops().length === 0) {
      <div class="card text-center py-16">
        <div class="text-5xl mb-4">🌱</div>
        <p class="text-gray-500">No listings yet. Create your first crop listing!</p>
        <a routerLink="/farmer/crops/new" class="btn-primary mt-4 inline-block">Create Listing</a>
      </div>
    } @else {
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        @for (crop of crops(); track crop.id) {
          <div class="card hover:shadow-md transition-shadow">
            <div class="flex justify-between items-start mb-3">
              <div>
                <h3 class="font-semibold text-gray-900">{{ crop.cropName }}</h3>
                <p class="text-sm text-gray-500">{{ crop.cropType }}</p>
              </div>
              <span [class]="statusBadge(crop.status)">{{ crop.status }}</span>
            </div>
            <div class="space-y-1 text-sm text-gray-600">
              <p>📍 {{ crop.location }}</p>
              <p>⚖️ {{ crop.quantityAvailable }} {{ crop.unit }}</p>
              <p class="text-lg font-bold text-primary-700">₹{{ crop.pricePerUnit }}/{{ crop.unit }}</p>
            </div>
            <div class="flex gap-2 mt-4">
              <a [routerLink]="['/farmer/crops', crop.id, 'edit']" class="btn-secondary text-xs flex-1 text-center">Edit</a>
              <button (click)="delete(crop.id)" class="btn-danger text-xs flex-1">Delete</button>
            </div>
          </div>
        }
      </div>
    }
  `
})
export class MyCropsComponent implements OnInit {
  crops  = signal<CropListing[]>([]);
  loading = signal(true);

  constructor(private cropService: CropService) {}

  ngOnInit(): void {
    this.cropService.getMyListings().subscribe({
      next: data => { this.crops.set(data); this.loading.set(false); },
      error: ()   => this.loading.set(false)
    });
  }

  delete(id: number): void {
    if (!confirm('Deactivate this listing?')) return;
    this.cropService.delete(id).subscribe(() =>
      this.crops.update(list => list.filter(c => c.id !== id))
    );
  }

  statusBadge(status: string): string {
    return status === 'AVAILABLE' ? 'badge-green' : status === 'SOLD' ? 'badge-blue' : 'badge-red';
  }
}
