import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
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
import { BackButton } from '../../components/back-button/back-button';

export interface CartItem {
  menuItem: MenuItem;
  quantity: number;
}

@Component({
  selector: 'app-customer-menu',
  imports: [CommonModule, FormsModule, BackButton],
  templateUrl: './customer-menu.html',
  styleUrls: ['./customer-menu.css']
})
export class CustomerMenu implements OnInit {
  route             = inject(ActivatedRoute);
  restaurantService = inject(RestaurantService);
  categoryService   = inject(CategoryService);
  menuService       = inject(MenuService);
  offerService      = inject(OfferService);
  publicMenuService = inject(PublicMenuService);
  toastService      = inject(ToastService);
  modalService      = inject(ModalService);

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
  isDarkMode           = signal<boolean>(true);

  showMobileCart       = signal<boolean>(false);
  showMobileSidebar    = signal<boolean>(false);

  // ── Table number ─────────────────────────────────────────────────────────────
  tableNumber      = signal<number>(0);
  isEditingTable   = signal<boolean>(false);
  tableInputValue  = signal<string>('');

  // ── Order flow ───────────────────────────────────────────────────────────────
  orderPlaced    = signal<boolean>(false);
  orderId        = signal<string>('');
  isPlacingOrder = signal<boolean>(false);

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

      // Use unified /public/menu/{tokenOrSlug} endpoint (handles QR token + slug)
      this.publicMenuService.fetchPublicMenu(tokenOrSlug).subscribe((payload: PublicMenuPayload | null) => {
        if (payload) {
          this.restaurant.set(payload.restaurant);
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
            } else {
              this.loadError.set('Menu not found. Please scan a valid QR code.');
              this.isLoading.set(false);
            }
          });
        }
      });
    });
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

  // ── Table number editing ─────────────────────────────────────────────────────
  startEditTable() {
    this.tableInputValue.set(this.tableNumber() > 0 ? String(this.tableNumber()) : '');
    this.isEditingTable.set(true);
  }

  confirmTableEdit() {
    const val = Number(this.tableInputValue());
    if (!isNaN(val) && val > 0) {
      this.tableNumber.set(val);
    }
    this.isEditingTable.set(false);
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
    this.cartItems.update((cart: CartItem[]) => cart.filter((c: CartItem) => c.menuItem.id !== itemId));
  }

  clearCart() {
    this.cartItems.set([]);
  }

  // ── Place order ──────────────────────────────────────────────────────────────
  placeOrder() {
    if (this.cartIsEmpty() || this.isPlacingOrder()) return;

    this.isPlacingOrder.set(true);

    setTimeout(() => {
      const id = 'ORD-' + Math.random().toString(36).substring(2, 8).toUpperCase();
      this.orderId.set(id);
      this.orderPlaced.set(true);
      this.isPlacingOrder.set(false);
      this.showMobileCart.set(false);
    }, 1200);
  }

  closeOrderSuccess() {
    this.orderPlaced.set(false);
    this.clearCart();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────
  formatPrice(price: number): string {
    return '₹' + price.toFixed(2);
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
