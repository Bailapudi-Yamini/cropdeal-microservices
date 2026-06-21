import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, DashboardStats } from '../core/services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Dashboard</h1>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else if (stats()) {
      <div class="grid grid-cols-2 xl:grid-cols-3 gap-4 mb-8">
        <div class="card">
          <p class="text-sm text-gray-500">Total Users</p>
          <p class="text-3xl font-bold text-gray-900 mt-1">{{ stats()!.totalUsers | number }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Farmers</p>
          <p class="text-3xl font-bold text-primary-700 mt-1">{{ stats()!.totalFarmers | number }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Dealers</p>
          <p class="text-3xl font-bold text-accent-600 mt-1">{{ stats()!.totalDealers | number }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Total Orders</p>
          <p class="text-3xl font-bold text-gray-900 mt-1">{{ stats()!.totalOrders | number }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Total Revenue</p>
          <p class="text-3xl font-bold text-primary-700 mt-1">₹{{ stats()!.totalRevenue | number }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">Active Listings</p>
          <p class="text-3xl font-bold text-gray-900 mt-1">{{ stats()!.activeListings | number }}</p>
        </div>
      </div>
    }
  `
})
export class AdminDashboardComponent implements OnInit {
  stats   = signal<DashboardStats | null>(null);
  loading = signal(true);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.adminService.getDashboardStats().subscribe({
      next: s => { this.stats.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
