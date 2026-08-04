import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminService, AdminRestaurantData } from '../../services/admin.service';
import { TicketService, SupportTicketData } from '../../services/ticket.service';
import { ToastService } from '../../services/toast.service';
import { ModalService } from '../../services/modal.service';
import { BackButton } from '../../components/back-button/back-button';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, RouterLink, BackButton],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard implements OnInit, OnDestroy {
  authService   = inject(AuthService);
  adminService  = inject(AdminService);
  ticketService = inject(TicketService);
  toastService  = inject(ToastService);
  modalService  = inject(ModalService);
  router        = inject(Router);

  // ── Tab navigation ────────────────────────────────────────────────────────
  activeTab = signal<'restaurants' | 'tickets'>('restaurants');

  // ── Restaurants & Pagination ─────────────────────────────────────────────
  searchQuery = signal<string>('');
  expandedId  = signal<string | null>(null);

  currentPage = signal<number>(1);
  pageSize    = signal<number>(8);

  restaurants = computed(() => this.adminService.restaurantsList());

  filteredRestaurants = computed(() => {
    const q = this.searchQuery().toLowerCase();
    if (!q) return this.restaurants();
    return this.restaurants().filter((r: AdminRestaurantData) =>
      r.name.toLowerCase().includes(q)    ||
      r.owner.toLowerCase().includes(q)   ||
      r.location.toLowerCase().includes(q)
    );
  });

  paginatedRestaurants = computed(() => {
    const list = this.filteredRestaurants();
    const start = (this.currentPage() - 1) * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  totalPages = computed(() => {
    return Math.ceil(this.filteredRestaurants().length / this.pageSize()) || 1;
  });

  nextPage() {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.update(p => p + 1);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.currentPage.update(p => p - 1);
    }
  }

  // Summary stats
  totalRestaurants  = computed(() => this.restaurants().length);
  activeRestaurants = computed(() => this.restaurants().filter((r: AdminRestaurantData) => r.status === 'active').length);
  totalScans        = computed(() => this.restaurants().reduce((s: number, r: AdminRestaurantData) => s + r.totalScans, 0));
  proPlans          = computed(() => this.restaurants().filter((r: AdminRestaurantData) => r.plan === 'Pro').length);

  // ── Support Tickets ───────────────────────────────────────────────────────
  tickets          = computed(() => this.ticketService.ticketsList());
  ticketFilter     = signal<'all' | 'open' | 'resolved'>('all');
  expandedTicketId = signal<string | null>(null);

  filteredTickets = computed(() => {
    const f = this.ticketFilter();
    if (f === 'all') return this.tickets();
    return this.tickets().filter((t: SupportTicketData) => t.status === f);
  });

  openTicketsCount     = computed(() => this.tickets().filter((t: SupportTicketData) => t.status === 'open').length);
  resolvedTicketsCount = computed(() => this.tickets().filter((t: SupportTicketData) => t.status === 'resolved').length);

  private autoRefreshTimer: any;

  ngOnInit() {
    this.refreshData();

    // ── Live time running feature: Poll every 5 seconds for live updates ──
    this.autoRefreshTimer = setInterval(() => {
      this.refreshData();
    }, 5000);
  }

  ngOnDestroy() {
    if (this.autoRefreshTimer) {
      clearInterval(this.autoRefreshTimer);
    }
  }

  refreshData() {
    this.adminService.fetchRestaurants().subscribe();
    this.ticketService.fetchAdminTickets().subscribe();
  }

  toggleRestaurant(id: string) {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  toggleTicket(id: string) {
    this.expandedTicketId.set(this.expandedTicketId() === id ? null : id);
  }

  toggleStatus(id: string) {
    const r = this.restaurants().find((r: AdminRestaurantData) => r.id === id);
    const actionName = r?.status === 'active' ? 'Suspend' : 'Activate';

    this.modalService.confirm({
      title: `${actionName} Venue Account`,
      message: `Are you sure you want to ${actionName.toLowerCase()} ${r?.name}?`,
      type: r?.status === 'active' ? 'warning' : 'info',
      confirmText: actionName,
      onConfirm: () => {
        this.adminService.toggleRestaurantStatus(id).subscribe(() => {
          this.toastService.success('Status Updated', `${r?.name} is now ${r?.status === 'active' ? 'Suspended' : 'Active'}`);
        });
      }
    });
  }

  resolveTicket(id: string) {
    this.ticketService.resolveTicket(id).subscribe(() => {
      this.toastService.success('Ticket Resolved', 'Support ticket marked as resolved.');
    });
  }

  reopenTicket(id: string) {
    this.ticketService.reopenTicket(id).subscribe(() => {
      this.toastService.info('Ticket Reopened', 'Ticket moved back to open queue.');
    });
  }

  planBadge(plan: string): string {
    return plan === 'Pro'
      ? 'bg-orange-500/15 text-orange-400 border-orange-500/20'
      : 'bg-white/5 text-gray-400 border-white/10';
  }

  statusBadge(status: string): string {
    return status === 'active'
      ? 'bg-green-500/15 text-green-400 border-green-500/20'
      : 'bg-red-500/15 text-red-400 border-red-500/20';
  }

  priorityBadge(priority: string): string {
    return {
      high:     'bg-red-500/15 text-red-400 border-red-500/20',
      critical: 'bg-red-500/15 text-red-400 border-red-500/20',
      medium:   'bg-amber-500/15 text-amber-400 border-amber-500/20',
      low:      'bg-blue-500/15 text-blue-400 border-blue-500/20',
    }[priority] ?? '';
  }

  timeAgo(dateInput: Date | string | undefined): string {
    if (!dateInput) return 'recently';
    const d = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    const mins = Math.floor((Date.now() - d.getTime()) / 60000);
    if (isNaN(mins) || mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    if (mins < 1440) return `${Math.floor(mins / 60)}h ago`;
    return `${Math.floor(mins / 1440)}d ago`;
  }

  logout() {
    this.modalService.confirm({
      title: 'Sign Out Admin',
      message: 'Are you sure you want to log out of Super Admin portal?',
      type: 'warning',
      confirmText: 'Sign Out',
      onConfirm: () => {
        this.authService.logout();
        this.toastService.info('Signed Out', 'Super Admin logged out.');
        this.router.navigate(['/login']);
      }
    });
  }
}
