import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminService, AdminRestaurantData } from '../../services/admin.service';
import { TicketService, SupportTicketData, TicketMessageData } from '../../services/ticket.service';
import { ToastService } from '../../services/toast.service';
import { ModalService } from '../../services/modal.service';
import { NotificationCenter } from '../../components/notification-center/notification-center';
import { NotificationService } from '../../services/notification.service';
import { CustomerHistoryService, CustomerHistoryData, CustomerSummary } from '../../services/customer-history.service';
import { MenuService } from '../../services/menu.service';
import { CategoryService } from '../../services/category.service';
import { RestaurantService } from '../../services/restaurant.service';
import { MenuItem } from '../../models/menu-item.model';
import { Category } from '../../models/category.model';
import { Order } from '../../services/order.service';
import { UndoService } from '../../services/undo.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, FormsModule, NotificationCenter, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboard implements OnInit, OnDestroy {
  authService            = inject(AuthService);
  adminService           = inject(AdminService);
  ticketService          = inject(TicketService);
  toastService           = inject(ToastService);
  modalService           = inject(ModalService);
  notificationService    = inject(NotificationService);
  customerHistoryService = inject(CustomerHistoryService);
  menuService            = inject(MenuService);
  categoryService        = inject(CategoryService);
  restaurantService      = inject(RestaurantService);
  undoService            = inject(UndoService);
  router                 = inject(Router);

  // ── Tab navigation ────────────────────────────────────────────────────────
  activeTab = signal<'analytics' | 'restaurants' | 'tickets' | 'add-menu' | 'menus' | 'categories' | 'customers'>('analytics');

  // ── Profile & Language Dropdowns ──────────────────────────────────────────
  showProfileDropdown  = signal<boolean>(false);
  showLanguageDropdown = signal<boolean>(false);
  selectedLanguage     = signal<string>('English');

  availableLanguages = [
    { code: 'en', name: 'English' },
    { code: 'es', name: 'Español' },
    { code: 'fr', name: 'Français' },
    { code: 'de', name: 'Deutsch' },
    { code: 'hi', name: 'हिन्दी' }
  ];

  toggleProfileDropdown() {
    this.showProfileDropdown.update(v => !v);
    this.showLanguageDropdown.set(false);
  }

  toggleLanguageDropdown() {
    this.showLanguageDropdown.update(v => !v);
    this.showProfileDropdown.set(false);
  }

  selectLanguage(lang: string) {
    this.selectedLanguage.set(lang);
    this.showLanguageDropdown.set(false);
    this.toastService.success('Language Changed', `Admin display language set to ${lang}`);
  }

  logout() {
    this.modalService.confirm({
      title: 'Sign Out Admin Command',
      message: 'Are you sure you want to sign out of the Super Admin workspace?',
      type: 'warning',
      confirmText: 'Sign Out',
      onConfirm: () => {
        this.authService.logout();
        this.toastService.info('Signed Out', 'Admin session terminated.');
        this.router.navigate(['/login']);
      }
    });
  }

  // ── Analytics Stats (Stitch Analytics Reference) ─────────────────────────
  menusToday     = signal<number>(346);
  customersToday = signal<number>(221);
  totalRevenue   = signal<number>(951.52);
  employeeCount  = signal<number>(98);

  salesFilterType = signal<'Weekly' | 'Monthly' | 'Yearly'>('Weekly');
  isNumberToggle  = signal<boolean>(true);

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

  selectTab(tab: 'analytics' | 'restaurants' | 'tickets' | 'add-menu' | 'menus' | 'categories' | 'customers') {
    this.activeTab.set(tab);
    if (tab === 'tickets') {
      this.ticketService.markTicketsAsSeen();
      this.ticketService.fetchAdminTickets().subscribe();
    } else if (tab === 'customers') {
      const restId = this.selectedCustomerRestaurantId() || 1;
      this.customerHistoryService.fetchRecentCustomers(restId).subscribe();
    } else if (tab === 'menus' || tab === 'add-menu' || tab === 'categories') {
      const restId = this.selectedRestaurantId() || 1;
      this.categoryService.fetchCategories(restId).subscribe();
      this.menuService.fetchMenuItems(restId).subscribe();
    }
  }

  // ── Customer Order History & Tracking ─────────────────────────────────────
  customerSearchPhone          = signal<string>('');
  selectedCustomerRestaurantId = signal<number | string>(1);
  customerHistoryResult        = signal<CustomerHistoryData | null>(null);
  isSearchingCustomer          = signal<boolean>(false);
  customerSearchError          = signal<string | null>(null);
  customerRecentList           = computed(() => this.customerHistoryService.recentCustomers());
  customerDateFilter           = signal<'all' | 'today' | 'week' | 'month'>('all');
  customerStatusFilter         = signal<'all' | 'PENDING' | 'PREPARING' | 'READY' | 'COMPLETED'>('all');
  trackingOrderModal           = signal<Order | null>(null);

  // Customer Directory Page-wise Signals
  customerDirectoryPage        = signal<number>(1);
  customerDirectoryPageSize    = signal<number>(6);
  customerDirectorySearchQuery = signal<string>('');

  filteredCustomerDirectory = computed(() => {
    let list = this.customerRecentList();
    const query = this.customerDirectorySearchQuery().trim().toLowerCase();
    if (query) {
      list = list.filter(c => 
        (c.customerName && c.customerName.toLowerCase().includes(query)) ||
        (c.customerMobile && c.customerMobile.includes(query))
      );
    }
    return list;
  });

  customerDirectoryTotalPages = computed(() => {
    const total = this.filteredCustomerDirectory().length;
    const size = this.customerDirectoryPageSize();
    return Math.max(1, Math.ceil(total / size));
  });

  paginatedCustomerDirectory = computed(() => {
    const list = this.filteredCustomerDirectory();
    const page = this.customerDirectoryPage();
    const size = this.customerDirectoryPageSize();
    const start = (page - 1) * size;
    return list.slice(start, start + size);
  });

  customerPlatformStats = computed(() => {
    const list = this.customerRecentList();
    const totalDiners = list.length;
    const totalOrders = list.reduce((sum, c) => sum + (c.orderCount || 0), 0);
    const totalRevenue = list.reduce((sum, c) => sum + (c.totalSpent || 0), 0);
    const avgSpend = totalDiners > 0 ? totalRevenue / totalDiners : 0;
    return { totalDiners, totalOrders, totalRevenue, avgSpend };
  });

  prevCustomerDirectoryPage() {
    if (this.customerDirectoryPage() > 1) {
      this.customerDirectoryPage.update(p => p - 1);
    }
  }

  nextCustomerDirectoryPage() {
    if (this.customerDirectoryPage() < this.customerDirectoryTotalPages()) {
      this.customerDirectoryPage.update(p => p + 1);
    }
  }

  setCustomerDirectoryPage(p: number) {
    if (p >= 1 && p <= this.customerDirectoryTotalPages()) {
      this.customerDirectoryPage.set(p);
    }
  }

  onCustomerVenueChange(venueId: any) {
    this.selectedCustomerRestaurantId.set(venueId);
    this.customerDirectoryPage.set(1);
    this.customerHistoryService.fetchRecentCustomers(venueId, '', 100).subscribe();
  }

  selectCustomerFromDirectory(c: CustomerSummary) {
    this.customerSearchPhone.set(c.customerMobile);
    this.searchCustomerHistory();
  }

  clearSelectedCustomer() {
    this.customerHistoryResult.set(null);
    this.customerSearchPhone.set('');
    this.customerSearchError.set(null);
  }

  filteredCustomerOrders = computed(() => {
    const data = this.customerHistoryResult();
    if (!data || !data.orders) return [];

    let orders = [...data.orders];
    const sf = this.customerStatusFilter();
    if (sf !== 'all') {
      orders = orders.filter(o => (o.status || '').toUpperCase() === sf);
    }

    const df = this.customerDateFilter();
    if (df !== 'all') {
      const now = new Date();
      orders = orders.filter(o => {
        const d = new Date(o.createdAt || o.placedAt || '');
        if (isNaN(d.getTime())) return true;
        if (df === 'today') {
          return d.toDateString() === now.toDateString();
        }
        if (df === 'week') {
          const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
          return d >= weekAgo;
        }
        if (df === 'month') {
          const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
          return d >= monthAgo;
        }
        return true;
      });
    }

    return orders;
  });

  searchCustomerHistory() {
    const raw = this.customerSearchPhone().trim();
    const clean = raw.replace(/\D/g, '');

    if (!clean) {
      this.customerSearchError.set('Please enter a 10-digit mobile number');
      return;
    }
    if (clean.length !== 10) {
      this.customerSearchError.set('Phone number must be exactly 10 digits');
      return;
    }

    this.isSearchingCustomer.set(true);
    this.customerSearchError.set(null);

    const restId = this.selectedCustomerRestaurantId() || 1;
    this.customerHistoryService.fetchCustomerHistory(restId, clean).subscribe(result => {
      this.isSearchingCustomer.set(false);
      if (result) {
        this.customerHistoryResult.set(result);
        this.customerSearchError.set(null);
        this.toastService.success('Customer Found', `${result.customerName || 'Customer'} has ${result.totalOrders} recorded orders`);
      } else {
        this.customerHistoryResult.set(null);
        this.customerSearchError.set('No order history found for this phone number at the selected venue.');
      }
    });
  }

  quickSearchCustomer(phone: string) {
    this.customerSearchPhone.set(phone);
    this.searchCustomerHistory();
  }

  openOrderTrackingModal(order: Order) {
    this.trackingOrderModal.set(order);
  }

  closeOrderTrackingModal() {
    this.trackingOrderModal.set(null);
  }

  // ── Menu Catalog & Operations ─────────────────────────────────────────────
  menuSearchQuery        = signal<string>('');
  selectedMenuCategory   = signal<string>('all');
  selectedRestaurantId   = signal<number | string>(1);
  menuItems              = computed(() => this.menuService.menuItems());
  categories             = computed(() => this.categoryService.categories());

  filteredMenuItems = computed(() => {
    let items = this.menuItems();
    const q = this.menuSearchQuery().toLowerCase().trim();
    if (q) {
      items = items.filter((i: MenuItem) => i.name.toLowerCase().includes(q) || (i.description || '').toLowerCase().includes(q));
    }
    const cat = this.selectedMenuCategory();
    if (cat !== 'all') {
      items = items.filter((i: MenuItem) => String(i.categoryId) === String(cat));
    }
    return items;
  });

  getCategoryName(catId: string): string {
    const found = this.categories().find(c => String(c.id) === String(catId));
    return found ? found.name : 'All-Day Menu';
  }

  getItemImage(item: MenuItem): string {
    if (item.image && (item.image.startsWith('http') || item.image.startsWith('data:'))) {
      return item.image;
    }
    const name = (item.name || '').toLowerCase();
    if (name.includes('burger')) return 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&auto=format&fit=crop&q=80';
    if (name.includes('salmon') || name.includes('fish') || name.includes('lobster')) return 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=600&auto=format&fit=crop&q=80';
    if (name.includes('toast') || name.includes('croissant') || name.includes('pancake') || name.includes('breakfast') || name.includes('tartine')) return 'https://images.unsplash.com/photo-1525351484163-7529414344d8?w=600&auto=format&fit=crop&q=80';
    if (name.includes('steak') || name.includes('beef') || name.includes('ribeye') || name.includes('duck')) return 'https://images.unsplash.com/photo-1544025162-d76694265947?w=600&auto=format&fit=crop&q=80';
    if (name.includes('pasta') || name.includes('tagliolini') || name.includes('fettuccine')) return 'https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=600&auto=format&fit=crop&q=80';
    if (name.includes('dessert') || name.includes('chocolate') || name.includes('cake') || name.includes('panna cotta') || name.includes('fondant')) return 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600&auto=format&fit=crop&q=80';
    if (name.includes('coffee') || name.includes('brew') || name.includes('drink') || name.includes('wine') || name.includes('beverage')) return 'https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=600&auto=format&fit=crop&q=80';
    if (name.includes('shakshuka') || name.includes('egg')) return 'https://images.unsplash.com/photo-1590412200988-a436970781fa?w=600&auto=format&fit=crop&q=80';
    return 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&auto=format&fit=crop&q=80';
  }

  toggleMenuItemAvailability(item: MenuItem) {
    this.menuService.toggleAvailability(item.id);
    this.toastService.success('Dish Status', `"${item.name}" availability updated`);
  }

  toggleMenuItemPopular(item: MenuItem, event?: Event) {
    if (event) event.stopPropagation();
    const next = !item.isPopular;
    this.menuService.updateMenuItem(item.id, { isPopular: next });
    this.toastService.success(
      next ? 'Marked as Popular' : 'Removed from Popular',
      `"${item.name}" is ${next ? 'now featured as Popular' : 'no longer marked as Popular'}`
    );
    this.undoService.showUndo(
      `"${item.name}" ${next ? 'marked as Popular' : 'unmarked from Popular'}`,
      () => this.menuService.updateMenuItem(item.id, { isPopular: !next }),
      8,
      `Click UNDO or press Ctrl+Z to revert`
    );
  }

  deleteMenuItem(item: MenuItem) {
    this.modalService.confirm({
      title: 'Delete Menu Dish',
      message: `Are you sure you want to remove "${item.name}" from the catalogue?`,
      type: 'danger',
      confirmText: 'Delete Dish',
      onConfirm: () => {
        this.menuService.deleteMenuItem(item.id);
        this.toastService.success('Dish Deleted', `"${item.name}" was removed from menu`);
        this.undoService.showUndo(
          `Dish "${item.name}" Deleted`,
          () => this.menuService.restoreMenuItem(item.id, item),
          8,
          `Click UNDO or press Ctrl+Z to restore "${item.name}"`
        );
      }
    });
  }

  // ── Add New Menu Wizard ───────────────────────────────────────────────────
  newItemName        = signal<string>('');
  newItemDescription = signal<string>('');
  newItemPrice       = signal<number>(12.99);
  newItemCategoryId  = signal<string>('');
  newItemIsVeg       = signal<boolean>(true);
  newItemIsPopular   = signal<boolean>(false);
  isSavingMenuItem   = signal<boolean>(false);

  saveNewMenuItem() {
    const name = this.newItemName().trim();
    if (!name) {
      this.toastService.show('Please enter a dish name', 'warning');
      return;
    }
    const price = Number(this.newItemPrice());
    if (isNaN(price) || price <= 0) {
      this.toastService.show('Please enter a valid price', 'warning');
      return;
    }

    this.isSavingMenuItem.set(true);
    const payload: Omit<MenuItem, 'id'> = {
      name,
      description: this.newItemDescription().trim(),
      price,
      categoryId: this.newItemCategoryId() || (this.categories()[0]?.id || '1'),
      isVeg: this.newItemIsVeg(),
      isPopular: this.newItemIsPopular(),
      isAvailable: true,
      image: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&auto=format&fit=crop&q=80'
    };

    try {
      this.menuService.addMenuItem(payload);
      this.isSavingMenuItem.set(false);
      this.toastService.success('Dish Created', `"${name}" added to menu catalog!`);
      this.newItemName.set('');
      this.newItemDescription.set('');
      this.newItemPrice.set(12.99);
      this.activeTab.set('menus');
    } catch {
      this.isSavingMenuItem.set(false);
      this.toastService.show('Failed to save menu dish. Please check inputs.', 'error');
    }
  }

  // ── Categories Management ─────────────────────────────────────────────────
  newCategoryName  = signal<string>('');
  newCategoryIcon  = signal<string>('restaurant');
  isSavingCategory = signal<boolean>(false);

  getCategoryIcon(cat: Category | string): string {
    const name = typeof cat === 'string' ? cat : (cat?.name || cat?.icon || '');
    const lower = name.toLowerCase();
    if (lower.includes('breakfast') || lower.includes('bakery') || lower.includes('pancake')) return 'bakery_dining';
    if (lower.includes('lunch') || lower.includes('burger') || lower.includes('sandwich')) return 'lunch_dining';
    if (lower.includes('dinner') || lower.includes('steak') || lower.includes('main')) return 'dinner_dining';
    if (lower.includes('dessert') || lower.includes('sweet') || lower.includes('cake') || lower.includes('ice cream')) return 'icecream';
    if (lower.includes('beverage') || lower.includes('drink') || lower.includes('wine') || lower.includes('coffee') || lower.includes('tea')) return 'local_bar';
    if (lower.includes('pizza')) return 'local_pizza';
    if (lower.includes('salad') || lower.includes('veg')) return 'nutrition';
    return 'restaurant';
  }

  saveNewCategory() {
    const name = this.newCategoryName().trim();
    if (!name) {
      this.toastService.show('Please enter a category name', 'warning');
      return;
    }

    this.isSavingCategory.set(true);
    const payload: Omit<Category, 'id'> = {
      name,
      icon: this.newCategoryIcon().trim() || 'restaurant',
      restaurantId: String(this.selectedRestaurantId() || '1')
    };

    try {
      this.categoryService.addCategory(payload);
      this.isSavingCategory.set(false);
      this.newCategoryName.set('');
      this.toastService.success('Category Created', `Category "${name}" is active!`);
    } catch {
      this.isSavingCategory.set(false);
      this.toastService.show('Failed to create category', 'error');
    }
  }

  deleteCategory(cat: Category) {
    this.modalService.confirm({
      title: 'Delete Category',
      message: `Are you sure you want to delete category "${cat.name}"?`,
      type: 'danger',
      confirmText: 'Delete',
      onConfirm: () => {
        this.categoryService.deleteCategory(cat.id);
        this.toastService.success('Category Removed', `"${cat.name}" deleted.`);
        this.undoService.showUndo(
          `Category "${cat.name}" Deleted`,
          () => this.categoryService.restoreCategory(cat.id, cat),
          8,
          `Click UNDO or press Ctrl+Z to restore "${cat.name}"`
        );
      }
    });
  }

  // ── Support Tickets ───────────────────────────────────────────────────────
  tickets          = computed(() => this.ticketService.ticketsList());
  ticketFilter     = signal<'all' | 'open' | 'resolved'>('all');
  expandedTicketId = signal<string | null>(null);
  selectedTicketDetails = signal<{ ticket: SupportTicketData; messages: TicketMessageData[] } | null>(null);
  showTicketModal       = signal<boolean>(false);
  adminReplyMessage     = signal<string>('');
  isSendingReply        = signal<boolean>(false);

  filteredTickets = computed(() => {
    const f = this.ticketFilter();
    const list = this.tickets();
    if (f === 'all') return list;
    if (f === 'open') return list.filter((t: SupportTicketData) => t.status === 'OPEN' || t.status === 'IN_PROGRESS' || t.status === 'open');
    if (f === 'resolved') return list.filter((t: SupportTicketData) => t.status === 'RESOLVED' || t.status === 'CLOSED' || t.status === 'resolved');
    return list;
  });

  openTicketsCount     = computed(() => this.tickets().filter((t: SupportTicketData) => t.status === 'OPEN' || t.status === 'IN_PROGRESS' || t.status === 'open').length);
  resolvedTicketsCount = computed(() => this.tickets().filter((t: SupportTicketData) => t.status === 'RESOLVED' || t.status === 'CLOSED' || t.status === 'resolved').length);

  private autoRefreshTimer: any;

  ngOnInit() {
    this.refreshData();

    // Live refresh poll
    this.autoRefreshTimer = setInterval(() => {
      this.refreshData();
    }, 5000);
  }

  ngOnDestroy() {
    if (this.autoRefreshTimer) {
      clearInterval(this.autoRefreshTimer);
    }
  }

  openSupportMessagesModal() {
    this.ticketService.markTicketsAsSeen();
    this.ticketService.fetchAdminTickets().subscribe(tickets => {
      const list = tickets && tickets.length > 0 ? tickets : this.tickets();
      if (list && list.length > 0) {
        const target = list.find((t: SupportTicketData) => t.status === 'OPEN' || t.status === 'IN_PROGRESS' || t.status === 'open') || list[0];
        this.openTicketDetails(target);
      } else {
        this.activeTab.set('tickets');
        this.toastService.info('Support Desk', 'No support tickets found yet. Opened Support Desk tab.');
      }
    });
  }

  onSelectChatTicket(ticketId: string) {
    const t = this.tickets().find(x => x.id === ticketId);
    if (t) {
      this.openTicketDetails(t);
    }
  }

  openTicketDetails(ticket: SupportTicketData) {
    this.ticketService.getTicketDetails(ticket.id).subscribe(res => {
      this.selectedTicketDetails.set(res || { ticket, messages: [] });
      this.showTicketModal.set(true);
    });
  }

  sendAdminReply() {
    const text = this.adminReplyMessage().trim();
    const details = this.selectedTicketDetails();
    if (!text || !details?.ticket) return;

    this.isSendingReply.set(true);
    this.ticketService.addMessage(details.ticket.id, text, 'Super Admin').subscribe(msg => {
      this.isSendingReply.set(false);
      this.adminReplyMessage.set('');
      if (msg) {
        this.selectedTicketDetails.update(d => d ? { ...d, messages: [...d.messages, msg] } : d);
        this.toastService.success('Reply Dispatched', 'Response sent to restaurant and diner.');
        this.refreshData();
      }
    });
  }

  updateTicketStatus(ticketId: string, status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED', showUndo: boolean = true) {
    const t = this.tickets().find(x => x.id === ticketId);
    const prevStatus = (t?.status || 'OPEN') as 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

    this.ticketService.updateTicketStatus(ticketId, status).subscribe(() => {
      this.toastService.success('Status Updated', `Ticket #${t?.ticketNumber || ticketId} marked as ${status}`);
      this.selectedTicketDetails.update(d => d ? { ...d, ticket: { ...d.ticket, status } } : d);
      this.refreshData();

      if (showUndo && prevStatus !== status) {
        this.undoService.showUndo(
          `Ticket #${t?.ticketNumber || ticketId} marked as ${status}`,
          () => this.updateTicketStatus(ticketId, prevStatus, false),
          8,
          `Click UNDO or press Ctrl+Z to revert to ${prevStatus}`
        );
      }
    });
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
          const newStatus = r?.status === 'active' ? 'Suspended' : 'Active';
          this.toastService.success('Status Updated', `${r?.name} is now ${newStatus}`);
          this.undoService.showUndo(
            `Restaurant "${r?.name}" marked as ${newStatus}`,
            () => this.adminService.toggleRestaurantStatus(id),
            8,
            `Click UNDO or press Ctrl+Z to restore ${r?.name} status`
          );
        });
      }
    });
  }

  approveVerification(id: string) {
    this.adminService.updateVerificationStatus(id, 'VERIFIED').subscribe(() => {
      const session = this.authService.currentUser();
      if (session) {
        session.verificationStatus = 'VERIFIED';
        localStorage.setItem('user_session', JSON.stringify(session));
        this.authService.currentUser.set({ ...session });
      }
      this.toastService.success('Venue Approved', 'Restaurant venue credentials approved successfully!');
    });
  }

  rejectVerification(id: string) {
    this.adminService.updateVerificationStatus(id, 'REJECTED').subscribe(() => {
      const session = this.authService.currentUser();
      if (session) {
        session.verificationStatus = 'REJECTED';
        localStorage.setItem('user_session', JSON.stringify(session));
        this.authService.currentUser.set({ ...session });
      }
      this.toastService.warning('Venue Rejected', 'Restaurant venue registration marked as rejected.');
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

  timeAgo(dateInput: Date | string | undefined): string {
    if (!dateInput) return 'recently';
    const d = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    const mins = Math.floor((Date.now() - d.getTime()) / 60000);
    if (isNaN(mins) || mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    if (mins < 1440) return `${Math.floor(mins / 60)}h ago`;
    return `${Math.floor(mins / 1440)}d ago`;
  }
}
