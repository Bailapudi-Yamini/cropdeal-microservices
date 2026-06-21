import { Component, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../core/services/admin.service';
import { User } from '../../core/models';
import { debounceTime } from 'rxjs/operators';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">User Management</h1>
      <span class="text-sm text-gray-500">{{ total() }} users total</span>
    </div>

    <!-- Filter bar -->
    <div class="card mb-6">
      <form [formGroup]="filterForm" class="flex gap-4">
        <select formControlName="role" class="input-field w-40">
          <option value="">All Roles</option>
          <option value="FARMER">Farmers</option>
          <option value="DEALER">Dealers</option>
          <option value="ADMIN">Admins</option>
        </select>
      </form>
    </div>

    @if (loading()) {
      <div class="text-center py-16 text-gray-400">Loading…</div>
    } @else {
      <div class="card overflow-hidden p-0">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b border-gray-100">
            <tr>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Name</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Email</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Role</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Location</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Status</th>
              <th class="text-left px-4 py-3 font-medium text-gray-600">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            @for (user of users(); track user.id) {
              <tr class="hover:bg-gray-50">
                <td class="px-4 py-3 font-medium">{{ user.name }}</td>
                <td class="px-4 py-3 text-gray-500">{{ user.email }}</td>
                <td class="px-4 py-3">
                  <span [class]="roleBadge(user.role)">{{ user.role }}</span>
                </td>
                <td class="px-4 py-3 text-gray-500">{{ user.location ?? '—' }}</td>
                <td class="px-4 py-3">
                  <span [class]="user.active ? 'badge-green' : 'badge-red'">
                    {{ user.active ? 'Active' : 'Inactive' }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <button (click)="toggle(user)" [class]="user.active ? 'btn-danger text-xs py-1 px-2' : 'btn-primary text-xs py-1 px-2'">
                    {{ user.active ? 'Deactivate' : 'Activate' }}
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex justify-center gap-2 mt-4">
        <button class="btn-secondary text-sm" [disabled]="page() === 0" (click)="loadPage(page() - 1)">← Prev</button>
        <span class="px-4 py-2 text-sm text-gray-600">Page {{ page() + 1 }}</span>
        <button class="btn-secondary text-sm" [disabled]="!hasMore()" (click)="loadPage(page() + 1)">Next →</button>
      </div>
    }
  `
})
export class UsersComponent implements OnInit {
  users   = signal<User[]>([]);
  loading = signal(true);
  page    = signal(0);
  total   = signal(0);
  hasMore = signal(false);

  filterForm = this.fb.group({ role: [''] });

  constructor(private fb: FormBuilder, private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadPage(0);
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => this.loadPage(0));
  }

  loadPage(p: number): void {
    this.loading.set(true);
    const role = this.filterForm.get('role')?.value || undefined;
    this.adminService.getUsers(p, 20, role).subscribe({
      next: data => {
        this.users.set(data.content);
        this.total.set(data.totalElements);
        this.hasMore.set(p + 1 < data.totalPages);
        this.page.set(p);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  toggle(user: User): void {
    this.adminService.toggleUserStatus(user.id).subscribe(updated =>
      this.users.update(list => list.map(u => u.id === user.id ? updated : u))
    );
  }

  roleBadge(role: string): string {
    return role === 'FARMER' ? 'badge-green' : role === 'DEALER' ? 'badge-blue' : 'badge-red';
  }
}
