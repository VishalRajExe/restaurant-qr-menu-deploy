import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { RestaurantService } from '../../services/restaurant.service';
import { CategoryService } from '../../services/category.service';
import { MenuService } from '../../services/menu.service';
import { OfferService } from '../../services/offer.service';
import { PublicMenuService, PublicMenuPayload } from '../../services/public-menu.service';
import { Restaurant } from '../../models/restaurant.model';
import { Category } from '../../models/category.model';
import { MenuItem } from '../../models/menu-item.model';
import { Offer } from '../../models/offer.model';

import { ToastService } from '../../services/toast.service';
import { ModalService } from '../../services/modal.service';
import { OrderService, Order } from '../../services/order.service';
import { PrintService } from '../../services/print.service';
import { BackButton } from '../../components/back-button/back-button';
import { UndoService } from '../../services/undo.service';
import { TicketService, SupportTicketData } from '../../services/ticket.service';
import { DiningTableData } from '../../services/table.service';

export interface CartItem {
  menuItem: MenuItem;
  quantity: number;
}

@Component({
  selector: 'app-customer-menu',
  imports: [CommonModule, FormsModule],
  templateUrl: './customer-menu.html',
  styleUrls: ['./customer-menu.css']
})
export class CustomerMenu implements OnInit, OnDestroy {
  route             = inject(ActivatedRoute);
  router            = inject(Router);
  restaurantService = inject(RestaurantService);
  categoryService   = inject(CategoryService);
  menuService       = inject(MenuService);
  offerService      = inject(OfferService);
  publicMenuService = inject(PublicMenuService);
  toastService      = inject(ToastService);
  modalService      = inject(ModalService);
  orderService      = inject(OrderService);
  ticketService     = inject(TicketService);
  printService      = inject(PrintService);
  undoService       = inject(UndoService);

  // ── Restaurant data ──────────────────────────────────────────────────────────
  restaurant = signal<Restaurant | undefined>(undefined);
  isLoading  = signal<boolean>(true);
  loadError  = signal<string>('');

  // ── UI & Filter states ───────────────────────────────────────────────────────
  searchQuery          = signal<string>('');
  activeCategoryTagId  = signal<string>('all');
  foodTypeFilter       = signal<'all' | 'veg' | 'non-veg'>('all');
  maxPriceFilter       = signal<number>(100);
  selectedLanguage     = signal<string>('en');
  isDarkMode           = signal<boolean>(false);

  // ── Customer Dining Support & Issue Reports ──────────────────────────────────
  showCustomerReportModal  = signal<boolean>(false);
  reportIssueCategory      = signal<string>('FOOD_QUALITY');
  reportSubject            = signal<string>('');
  reportDescription        = signal<string>('');
  reportDinerMobile        = signal<string>('');
  reportDinerName          = signal<string>('');
  customerTicketsList      = signal<SupportTicketData[]>([]);
  isSubmittingReport       = signal<boolean>(false);
  showCustomerTicketsList  = signal<boolean>(false);
  activeReportTab          = signal<'create' | 'history'>('create');

  goBack() {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      this.router.navigate(['/']);
    }
  }

  showMobileCart       = signal<boolean>(false);
  showMobileSidebar    = signal<boolean>(false);

  // ── Table number & Live Availability ─────────────────────────────────────────
  tableNumber      = signal<number>(0);
  isEditingTable   = signal<boolean>(false);
  tableInputValue  = signal<string>('');
  tablesList       = signal<DiningTableData[]>([]);

  getTableNumberDigits(tableNumberStr: string | number | undefined): number {
    if (!tableNumberStr) return 0;
    const digits = String(tableNumberStr).replace(/\D/g, '');
    return digits ? parseInt(digits, 10) : 0;
  }

  formatTableDisplay(tbl: DiningTableData | string | number | undefined): string {
    if (!tbl) return 'Table 01';
    if (typeof tbl === 'object' && tbl !== null) {
      const raw = tbl.tableNumber || '';
      if (raw.toLowerCase().startsWith('table')) return raw;
      const n = this.getTableNumberDigits(raw);
      return n > 0 ? `Table ${n < 10 ? '0' + n : n}` : `Table ${raw}`;
    }
    const n = typeof tbl === 'number' ? tbl : this.getTableNumberDigits(tbl);
    return n > 0 ? `Table ${n < 10 ? '0' + n : n}` : 'Table 01';
  }

  availableTables = computed(() => this.tablesList().filter(t => t.status === 'AVAILABLE'));
  occupiedTables  = computed(() => this.tablesList().filter(t => t.status === 'OCCUPIED' || t.status === 'RESERVED' || t.status === 'CLEANING'));

  currentTableData = computed(() => {
    const num = this.tableNumber();
    if (!num) return null;
    return this.tablesList().find(t => this.getTableNumberDigits(t.tableNumber) === num) || null;
  });

  isCurrentTableOccupied = computed(() => {
    const t = this.currentTableData();
    return t ? t.status !== 'AVAILABLE' : false;
  });

  // ── Order flow & Customer Mobile ─────────────────────────────────────────────
  customerMobile        = signal<string>('');
  customerName          = signal<string>('Guest Customer');
  specialInstructions   = signal<string>('');
  orderPlaced           = signal<boolean>(false);
  orderId               = signal<string>('');
  isPlacingOrder        = signal<boolean>(false);
  showProceedOrderModal = signal<boolean>(false);

  openProceedOrder() {
    if (this.cartIsEmpty()) {
      this.toastService.show('Please select dishes to proceed with your order', 'warning');
      return;
    }
    this.showProceedOrderModal.set(true);
  }

  closeProceedOrder() {
    this.showProceedOrderModal.set(false);
  }

  // ── Order Status Tracking & Customer History ──────────────────────────────────
  activeOrder           = signal<Order | null>(null);
  showOrderTracker      = signal<boolean>(false);
  showTrackLookupModal  = signal<boolean>(false);
  customerOrderHistory  = signal<Order[]>([]);
  showOrderHistoryModal = signal<boolean>(false);
  lookupQuery           = signal<string>('');
  trackedOrdersList     = signal<Order[]>([]);
  isTracking            = signal<boolean>(false);

  private pollTimer: any;

  // ── Cart ─────────────────────────────────────────────────────────────────────
  cartItems = signal<CartItem[]>([]);

  // ── Computed ─────────────────────────────────────────────────────────────────
  categories = computed(() => {
    const r = this.restaurant();
    if (!r) return [];
    return this.categoryService.getCategoriesForRestaurant(r.id);
  });

  menuItems = computed(() => {
    const cats = this.categories();
    return this.menuService.getMenuItemsForCategories(cats.map((c: Category) => c.id));
  });

  filteredDishes = computed(() => {
    let list = this.menuItems();
    const tag   = this.activeCategoryTagId();
    const query = this.searchQuery().trim().toLowerCase();
    const type  = this.foodTypeFilter();
    const maxP  = this.maxPriceFilter();

    if (tag !== 'all') {
      list = list.filter((item: MenuItem) => item.categoryId === tag);
    }
    if (type === 'veg') {
      list = list.filter((item: MenuItem) => item.isVeg === true);
    } else if (type === 'non-veg') {
      list = list.filter((item: MenuItem) => item.isVeg === false);
    }
    if (maxP < 100) {
      list = list.filter((item: MenuItem) => item.price <= maxP);
    }
    if (query) {
      list = list.filter((item: MenuItem) =>
        item.name.toLowerCase().includes(query) ||
        item.description?.toLowerCase().includes(query)
      );
    }
    return list;
  });

  activeOffers = computed(() => {
    const r = this.restaurant();
    if (!r) return [];
    return this.offerService.getOffersForRestaurant(r.id);
  });

  // Cart computed values
  cartCount = computed(() =>
    this.cartItems().reduce((sum: number, i: CartItem) => sum + i.quantity, 0)
  );

  cartSubtotal = computed(() =>
    this.cartItems().reduce((sum: number, i: CartItem) => sum + i.menuItem.price * i.quantity, 0)
  );

  cartTax = computed(() => this.cartSubtotal() * 0.05);

  cartTotal = computed(() => this.cartSubtotal() + this.cartTax());

  cartIsEmpty = computed(() => this.cartItems().length === 0);

  totalDishesCount = computed(() => this.menuItems().length);

  currentFormattedDate = computed(() => {
    const now = new Date();
    return now.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }) + 
           ' • ' + now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  });

  categoryItemCount = computed(() => {
    const map: Record<string, number> = {};
    for (const item of this.menuItems()) {
      map[item.categoryId] = (map[item.categoryId] || 0) + 1;
    }
    return map;
  });

  // ── Lifecycle ────────────────────────────────────────────────────────────────
  ngOnInit() {
    this.route.queryParamMap.subscribe((params: ParamMap) => {
      const t = params.get('table');
      if (t && !isNaN(Number(t))) {
        this.tableNumber.set(Number(t));
      }
    });

    this.route.paramMap.subscribe((params: ParamMap) => {
      const tokenOrSlug = params.get('restaurantId') || 'gourmet-bistro';
      this.isLoading.set(true);
      this.loadError.set('');
      this.activeCategoryTagId.set('all');

      // Use unified /public/menu/{tokenOrSlug} endpoint
      this.publicMenuService.fetchPublicMenu(tokenOrSlug).subscribe((payload: PublicMenuPayload | null) => {
        if (payload) {
          this.restaurant.set(payload.restaurant);

          // Populate tables list & live availability
          const restId = payload.restaurant.id || 1;
          if (payload.tables && payload.tables.length > 0) {
            this.tablesList.set(payload.tables);
          } else {
            this.publicMenuService.fetchPublicTables(restId).subscribe(tables => {
              if (tables && tables.length > 0) {
                this.tablesList.set(tables);
              }
            });
          }

          // Auto-detect table number if embedded in QR payload
          if (payload.qrCode && payload.qrCode.tableNumber) {
            const qrTableNum = this.getTableNumberDigits(payload.qrCode.tableNumber);
            if (!isNaN(qrTableNum) && qrTableNum > 0 && this.tableNumber() === 0) {
              this.tableNumber.set(qrTableNum);
            }
          }

          // If tableNumber was already set, check its availability
          if (this.tableNumber() > 0 && this.tablesList().length > 0) {
            const match = this.tablesList().find(t => this.getTableNumberDigits(t.tableNumber) === this.tableNumber());
            if (match && match.status !== 'AVAILABLE') {
              this.toastService.show(`Table ${this.tableNumber()} is currently marked as ${match.status.toLowerCase()}. Please choose a free table.`, 'warning');
            }
          } else if (this.tableNumber() === 0 && this.availableTables().length > 0) {
            const firstAvail = this.availableTables()[0];
            if (firstAvail) {
              this.tableNumber.set(this.getTableNumberDigits(firstAvail.tableNumber));
            }
          }

          this.isLoading.set(false);
        } else {
          // Fallback: try individual service calls
          this.restaurantService.fetchRestaurantProfile(tokenOrSlug).subscribe((res: Restaurant | undefined) => {
            if (res) {
              this.restaurant.set(res);
              this.categoryService.fetchCategories(res.id).subscribe();
              this.menuService.fetchMenuItems(res.id).subscribe(() => {
                this.isLoading.set(false);
              });
              this.offerService.fetchActiveOffers(res.id).subscribe();
              this.publicMenuService.fetchPublicTables(res.id).subscribe(tables => {
                if (tables && tables.length > 0) {
                  this.tablesList.set(tables);
                }
              });
            } else {
              this.loadError.set('Menu not found. Please scan a valid QR code.');
              this.isLoading.set(false);
            }
          });
        }
      });
    });

    this.loadCustomerOrderHistory();

    // Start real-time background status polling timer for active kitchen order
    this.pollTimer = setInterval(() => {
      if (this.activeOrder()) {
        const status = (this.activeOrder()?.status || '').toUpperCase();
        if (this.showOrderTracker() || !['COMPLETED', 'DELIVERED', 'CANCELLED'].includes(status)) {
          this.refreshActiveOrderStatus();
        }
      }
    }, 2500);
  }

  ngOnDestroy() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
    }
  }

  toggleTheme() {
    this.isDarkMode.update((v: boolean) => !v);
  }

  setLanguage(lang: string) {
    this.selectedLanguage.set(lang);
  }

  setFoodFilter(type: 'all' | 'veg' | 'non-veg') {
    this.foodTypeFilter.set(type);
  }

  // ── Category filter ──────────────────────────────────────────────────────────
  selectCategory(tagId: string) {
    this.activeCategoryTagId.set(tagId);
    this.showMobileSidebar.set(false);
  }

  // ── Table number editing & validation ─────────────────────────────────────────
  startEditTable() {
    this.tableInputValue.set(this.tableNumber() > 0 ? String(this.tableNumber()) : '');
    this.isEditingTable.set(true);

    // Refresh live table statuses from backend
    const restId = this.restaurant()?.id || 1;
    this.publicMenuService.fetchPublicTables(restId).subscribe(tables => {
      if (tables && tables.length > 0) {
        this.tablesList.set(tables);
      }
    });
  }

  selectTable(table: DiningTableData) {
    if (table.status !== 'AVAILABLE') {
      this.toastService.show(`${this.formatTableDisplay(table)} is currently ${table.status.toLowerCase()}. Please select an available free table.`, 'warning');
      return;
    }
    const num = this.getTableNumberDigits(table.tableNumber);
    this.tableNumber.set(num);
    this.tableInputValue.set(num > 0 ? (num < 10 ? '0' + num : String(num)) : String(table.tableNumber));
    this.isEditingTable.set(false);
    this.toastService.success('Table Selected', `Now dining at ${this.formatTableDisplay(table)} (${table.capacity} Seats)`);
  }

  confirmTableEdit() {
    const raw = this.tableInputValue().trim();
    const val = this.getTableNumberDigits(raw);
    if (isNaN(val) || val <= 0) {
      this.toastService.show('Please enter a valid table number', 'warning');
      return;
    }

    if (this.tablesList().length > 0) {
      const match = this.tablesList().find(t => this.getTableNumberDigits(t.tableNumber) === val);
      if (!match) {
        this.toastService.show(`Table ${val} does not exist at this restaurant. Please choose from the available tables.`, 'warning');
        return;
      }
      if (match.status !== 'AVAILABLE') {
        this.toastService.show(`Table ${val} is currently ${match.status.toLowerCase()}. Please choose a free table.`, 'warning');
        return;
      }
    }

    this.tableNumber.set(val);
    this.isEditingTable.set(false);
    this.toastService.success('Table Updated', `Dining at Table ${val < 10 ? '0' + val : val}`);
  }

  cancelTableEdit() {
    this.isEditingTable.set(false);
  }

  // ── Cart actions ─────────────────────────────────────────────────────────────
  getItemQty(itemId: string): number {
    return this.cartItems().find((c: CartItem) => c.menuItem.id === itemId)?.quantity ?? 0;
  }

  addToCart(item: MenuItem) {
    this.cartItems.update((cart: CartItem[]) => {
      const existing = cart.find((c: CartItem) => c.menuItem.id === item.id);
      if (existing) {
        return cart.map((c: CartItem) =>
          c.menuItem.id === item.id ? { ...c, quantity: c.quantity + 1 } : c
        );
      }
      return [...cart, { menuItem: item, quantity: 1 }];
    });
  }

  decrementQty(itemId: string) {
    this.cartItems.update((cart: CartItem[]) => {
      const existing = cart.find((c: CartItem) => c.menuItem.id === itemId);
      if (!existing) return cart;
      if (existing.quantity === 1) {
        return cart.filter((c: CartItem) => c.menuItem.id !== itemId);
      }
      return cart.map((c: CartItem) =>
        c.menuItem.id === itemId ? { ...c, quantity: c.quantity - 1 } : c
      );
    });
  }

  removeFromCart(itemId: string) {
    const item = this.cartItems().find(c => c.menuItem.id === itemId);
    if (!item) return;

    this.cartItems.update((cart: CartItem[]) => cart.filter((c: CartItem) => c.menuItem.id !== itemId));
    this.undoService.showUndo(`"${item.menuItem.name}" removed from cart`, () => {
      this.cartItems.update(cart => [...cart, item]);
    }, 7);
  }

  clearCart() {
    const previous = [...this.cartItems()];
    if (previous.length === 0) return;

    this.cartItems.set([]);
    this.undoService.showUndo('Cart cleared', () => {
      this.cartItems.set(previous);
    }, 7);
  }

  // ── Place order ──────────────────────────────────────────────────────────────
  placeOrder() {
    if (this.cartIsEmpty() || this.isPlacingOrder()) return;

    const cleanMobile = this.customerMobile().trim().replace(/\D/g, '');
    if (!cleanMobile || cleanMobile.length !== 10) {
      this.toastService.show('Please enter a valid 10-digit mobile number to place your order', 'error');
      return;
    }

    // Validate table availability
    if (this.tablesList().length > 0) {
      if (this.tableNumber() === 0) {
        this.toastService.show('Please select an available dining table first', 'warning');
        this.startEditTable();
        return;
      }
      const tableData = this.tablesList().find(t => this.getTableNumberDigits(t.tableNumber) === this.tableNumber());
      if (tableData && tableData.status !== 'AVAILABLE') {
        this.toastService.show(`Table ${this.tableNumber()} is currently ${tableData.status.toLowerCase()}. Please choose an available free table before ordering.`, 'warning');
        this.startEditTable();
        return;
      }
    }

    this.isPlacingOrder.set(true);

    const tableNum = this.tableNumber() > 0 ? (this.tableNumber() < 10 ? '0' + this.tableNumber() : String(this.tableNumber())) : '01';
    const restId = parseInt(this.restaurant()?.id || '1', 10) || 1;
    const restSlug = this.restaurant()?.slug || 'gourmet-bistro';

    const orderPayload = {
      restaurantId: restId,
      restaurantSlug: restSlug,
      tableNumber: tableNum,
      customerMobile: cleanMobile,
      customerName: this.customerName().trim() || 'Customer',
      specialInstructions: this.specialInstructions().trim(),
      items: this.cartItems().map((i: CartItem) => ({
        menuItemId: parseInt(i.menuItem.id.replace(/\D/g, ''), 10) || 1,
        name: i.menuItem.name,
        price: i.menuItem.price,
        qty: i.quantity,
        note: i.menuItem.isVeg ? 'Veg' : 'Standard Spice'
      }))
    };

    this.orderService.createOrder(orderPayload).subscribe((placedOrder: Order) => {
      this.activeOrder.set(placedOrder);
      this.saveOrderToHistory(placedOrder);
      this.orderId.set(placedOrder.orderNumber || placedOrder.id);
      this.orderPlaced.set(true);
      this.isPlacingOrder.set(false);
      this.showProceedOrderModal.set(false);
      this.showMobileCart.set(false);
      this.toastService.show('Order placed successfully!', 'success');
    });
  }

  private getHistoryStorageKey(): string {
    return 'aura_customer_orders_' + (this.restaurant()?.id || '1');
  }

  loadCustomerOrderHistory() {
    try {
      const stored = localStorage.getItem(this.getHistoryStorageKey());
      if (stored) {
        const list = JSON.parse(stored);
        if (Array.isArray(list)) {
          this.customerOrderHistory.set(list);
          if (list.length > 0 && !this.activeOrder()) {
            this.activeOrder.set(list[0]);
          }
        }
      }
    } catch (e) {
      console.warn('Failed to load customer order history', e);
    }
  }

  saveOrderToHistory(order: Order) {
    const list = this.customerOrderHistory();
    const filtered = list.filter((o: Order) => o.id !== order.id && o.orderNumber !== order.orderNumber);
    const updated = [order, ...filtered];
    this.customerOrderHistory.set(updated);
    try {
      localStorage.setItem(this.getHistoryStorageKey(), JSON.stringify(updated));
    } catch (e) {
      console.warn('Failed to save order to history', e);
    }
  }

  openOrderHistory() {
    this.loadCustomerOrderHistory();
    this.showOrderHistoryModal.set(true);
  }

  closeOrderHistory() {
    this.showOrderHistoryModal.set(false);
  }

  trackSpecificOrder(order: Order) {
    this.activeOrder.set(order);
    this.showOrderHistoryModal.set(false);
    this.showOrderTracker.set(true);
    this.refreshActiveOrderStatus();
  }

  reorderItems(order: Order) {
    if (!order.items || order.items.length === 0) return;
    const allDishes = this.menuItems();
    
    order.items.forEach(orderItem => {
      let targetDish = allDishes.find((d: MenuItem) => d.name.toLowerCase() === orderItem.name.toLowerCase());
      if (!targetDish) {
        targetDish = {
          id: 're_' + Math.random(),
          categoryId: 'c1',
          name: orderItem.name,
          price: orderItem.price || 150,
          description: 'Re-ordered item',
          image: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80',
          isAvailable: true,
          isVeg: true
        };
      }
      for (let i = 0; i < (orderItem.qty || 1); i++) {
        this.addToCart(targetDish);
      }
    });

    this.showOrderHistoryModal.set(false);
    this.showMobileCart.set(true);
    this.toastService.show(`Added items from ${order.orderNumber || 'previous order'} to your cart!`, 'success');
  }

  closeOrderSuccess() {
    this.orderPlaced.set(false);
    this.showOrderTracker.set(true);
    this.clearCart();
  }

  openOrderTracker() {
    if (!this.activeOrder()) {
      this.lookupQuery.set(this.customerMobile() || '');
      this.showTrackLookupModal.set(true);
    } else {
      this.showOrderTracker.set(true);
    }
  }

  refreshActiveOrderStatus() {
    const current = this.activeOrder();
    if (!current) return;
    const searchId = current.orderNumber || current.id;
    this.orderService.trackOrders(searchId).subscribe(orders => {
      if (orders && orders.length > 0) {
        const updatedOrder = orders[0];
        const prevStatus = (current.status || '').toUpperCase();
        const nextStatus = (updatedOrder.status || '').toUpperCase();

        if (prevStatus && prevStatus !== nextStatus) {
          if (nextStatus === 'PREPARING') {
            this.toastService.show('Kitchen Update: The chef is now preparing your meal.', 'info');
          } else if (nextStatus === 'READY') {
            this.toastService.show(`Order Ready: Order #${updatedOrder.orderNumber || updatedOrder.id} is ready for serving.`, 'success');
          } else if (nextStatus === 'COMPLETED' || nextStatus === 'DELIVERED') {
            this.toastService.show('Order delivered. Enjoy your meal.', 'success');
          }
        }

        this.activeOrder.set(updatedOrder);
        this.saveOrderToHistory(updatedOrder);
      }
    });
  }

  lookupOrders() {
    const q = this.lookupQuery().trim();
    if (!q) {
      this.toastService.show('Please enter Mobile Number or Order ID', 'error');
      return;
    }
    this.isTracking.set(true);
    this.orderService.trackOrders(q).subscribe(orders => {
      this.trackedOrdersList.set(orders);
      this.isTracking.set(false);
      if (orders.length === 0) {
        this.toastService.show('No orders found for this Mobile Number or Order ID', 'error');
      } else {
        this.activeOrder.set(orders[0]);
        this.showTrackLookupModal.set(false);
        this.showOrderTracker.set(true);
      }
    });
  }

  closeOrderTracker() {
    this.showOrderTracker.set(false);
  }

  printCustomerInvoice(order?: Order | null) {
    const target = order || this.activeOrder();
    if (!target) {
      this.toastService.show('No active order to print invoice', 'warning');
      return;
    }
    const r = this.restaurant();
    this.printService.printInvoice(target, {
      name: r?.name || 'RestQR Gourmet Bistro',
      address: r?.address || '123 Gourmet Blvd, New York, NY',
      phone: r?.phone || '+1 (555) 345-6789',
      email: r?.email || 'contact@restqr.com',
      currency: '$'
    });
    this.toastService.success('Tax Invoice Generated', `Invoice ready for Order #${target.orderNumber || target.id}`);
  }

  printCustomerKOT(order?: Order | null) {
    const target = order || this.activeOrder();
    if (!target) {
      this.toastService.show('No active order to print KOT', 'warning');
      return;
    }
    const r = this.restaurant();
    this.printService.printKOT(target, {
      name: r?.name || 'RestQR Gourmet Bistro',
      address: r?.address || '123 Gourmet Blvd, New York, NY',
      phone: r?.phone || '+1 (555) 345-6789'
    });
    this.toastService.success('KOT Printed', `Kitchen slip printed for Table ${target.tableNumber || '01'}`);
  }

  printCustomerBill(order?: Order | null) {
    this.printCustomerInvoice(order);
  }

  isStatusPassed(stepName: string, currentStatus?: string): boolean {
    if (!currentStatus) return stepName === 'RECEIVED';
    const statusHierarchy: Record<string, number> = {
      'RECEIVED': 1,
      'PENDING': 1,
      'PREPARING': 2,
      'COOKING': 2,
      'ACCEPTED': 2,
      'READY': 3,
      'DELIVERED': 4,
      'COMPLETED': 4
    };
    const currentWeight = statusHierarchy[currentStatus.toUpperCase()] || 1;
    const stepWeight = statusHierarchy[stepName.toUpperCase()] || 1;
    return currentWeight >= stepWeight;
  }

  isStatusCurrent(stepName: string, currentStatus?: string): boolean {
    if (!currentStatus) return stepName === 'RECEIVED';
    const s = currentStatus.toUpperCase();
    if (stepName === 'RECEIVED') return s === 'PENDING' || s === 'RECEIVED';
    if (stepName === 'PREPARING') return s === 'PREPARING' || s === 'COOKING' || s === 'ACCEPTED';
    if (stepName === 'READY') return s === 'READY';
    if (stepName === 'DELIVERED') return s === 'DELIVERED' || s === 'COMPLETED';
    return false;
  }

  // ── Dining Support & Feedback Report Methods ────────────────────────────────
  openCustomerReportModal() {
    if (!this.reportDinerMobile() && this.customerMobile()) {
      this.reportDinerMobile.set(this.customerMobile());
    }
    if (!this.reportDinerName() && this.customerName()) {
      this.reportDinerName.set(this.customerName());
    }
    this.showCustomerReportModal.set(true);
    if (this.reportDinerMobile()) {
      this.loadCustomerTickets();
    }
  }

  closeCustomerReportModal() {
    this.showCustomerReportModal.set(false);
  }

  submitCustomerReport() {
    const mobile = this.reportDinerMobile().trim();
    if (!mobile || !/^\d{10}$/.test(mobile)) {
      this.toastService.show('Please provide a valid 10-digit mobile number for ticket tracking', 'warning');
      return;
    }
    if (!this.reportSubject().trim() || !this.reportDescription().trim()) {
      this.toastService.show('Please describe the issue or feedback', 'warning');
      return;
    }

    const rId = this.restaurant()?.id || 1;
    this.isSubmittingReport.set(true);

    this.ticketService.createCustomerTicket({
      restaurantId: rId,
      customerName: this.reportDinerName().trim() || 'Dining Guest',
      customerMobile: mobile,
      category: this.reportIssueCategory(),
      subject: `[Table ${this.tableNumber() || '01'}] ${this.reportSubject().trim()}`,
      description: this.reportDescription().trim()
    }).subscribe(() => {
      this.isSubmittingReport.set(false);
      this.toastService.success('Report Sent to Management', 'Staff and kitchen have been notified.');
      this.reportSubject.set('');
      this.reportDescription.set('');
      this.activeReportTab.set('history');
      this.loadCustomerTickets();
    });
  }

  loadCustomerTickets() {
    const mobile = this.reportDinerMobile().trim();
    if (!mobile) return;
    const rId = this.restaurant()?.id || 1;
    this.ticketService.fetchCustomerTickets(rId, mobile).subscribe(list => {
      this.customerTicketsList.set(list);
    });
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────
  formatPrice(price: number): string {
    return '$' + price.toFixed(2);
  }

  getCategoryName(categoryId: string): string {
    return this.categories().find((c: Category) => c.id === categoryId)?.name ?? '';
  }

  trackById(_: number, item: { id: string }): string {
    return item.id;
  }

  estimatedMinutes = computed(() => {
    const count = this.cartCount();
    if (count <= 2) return 15;
    if (count <= 5) return 25;
    return 35;
  });
}
