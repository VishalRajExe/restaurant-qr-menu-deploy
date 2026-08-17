import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RestaurantService } from '../../services/restaurant.service';
import { CategoryService } from '../../services/category.service';
import { MenuService } from '../../services/menu.service';
import { OfferService } from '../../services/offer.service';
import { QrService, QrCodeData } from '../../services/qr.service';
import { AnalyticsService } from '../../services/analytics.service';
import { TicketService } from '../../services/ticket.service';
import { UploadService } from '../../services/upload.service';
import { ToastService } from '../../services/toast.service';
import { ModalService } from '../../services/modal.service';
import { OrderService, Order } from '../../services/order.service';
import { BackButton } from '../../components/back-button/back-button';
import { Category } from '../../models/category.model';
import { MenuItem } from '../../models/menu-item.model';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-owner-dashboard',
  imports: [CommonModule, FormsModule, RouterLink, BackButton],
  templateUrl: './owner-dashboard.html',
  styleUrls: ['./owner-dashboard.css']
})
export class OwnerDashboard implements OnInit, OnDestroy {
  authService       = inject(AuthService);
  restaurantService = inject(RestaurantService);
  categoryService   = inject(CategoryService);
  menuService       = inject(MenuService);
  offerService      = inject(OfferService);
  qrService         = inject(QrService);
  analyticsService  = inject(AnalyticsService);
  ticketService     = inject(TicketService);
  uploadService     = inject(UploadService);
  toastService      = inject(ToastService);
  modalService      = inject(ModalService);
  orderService      = inject(OrderService);
  router            = inject(Router);

  // Active page state: 'overview' | 'orders' | 'categories' | 'items' | 'qr' | 'settings'
  activeTab = signal<string>('overview');

  // Active restaurant selection
  activeRestaurant = computed(() => this.restaurantService.getActiveRestaurant());

  // Scans history log & analytics chart
  scansHistory = signal<any[]>([]);
  chartData    = signal<any[]>([]);

  // Orders Signal
  ordersList = computed(() => this.orderService.ordersSignal()());
  activeOrderFilter = signal<string>('ALL');

  filteredOrders = computed(() => {
    const list = this.ordersList();
    const filter = this.activeOrderFilter();
    if (filter === 'ALL') return list;
    return list.filter(o => o.status.toUpperCase() === filter.toUpperCase());
  });

  // --- Category Management States ---
  newCategoryName     = signal<string>('');
  newCategoryIcon     = signal<string>('Utensils');
  editingCategoryId   = signal<string | null>(null);
  editingCategoryName = signal<string>('');

  // --- Menu Item Management States ---
  newItemName         = signal<string>('');
  newItemPrice        = signal<number>(12.00);
  newItemDescription  = signal<string>('');
  newItemImage        = signal<string>('');
  imageUploadPreview  = signal<string>('');   
  imageInputMode      = signal<'upload' | 'url'>('upload');
  newItemIsVeg        = signal<boolean>(true);
  newItemCategoryId   = signal<string>('');
  newItemSpicyLevel   = signal<number>(0);

  // Restaurant logo
  restaurantLogoUrl   = signal<string>('');

  // Editing menu item
  editingItemId       = signal<string | null>(null);

  // --- QR Template Editor States ---
  qrStyle             = signal<'square' | 'rounded' | 'dots'>('rounded');
  qrFgColor           = signal<string>('#fc6011');
  qrBgColor           = signal<string>('#ffffff');
  qrIncludeLogo       = signal<boolean>(true);
  qrDownloadSimulated = signal<boolean>(false);
  newTableNumber      = signal<string>('Table 01');

  // Pagination states
  currentPage = signal<number>(1);
  pageSize    = signal<number>(6);

  // QR Codes signal selector
  qrCodesList = computed(() => this.qrService.qrCodesList());
  selectedQr  = signal<QrCodeData | null>(null);

  // Helper selectors
  categories = computed(() => {
    const rId = this.activeRestaurant()?.id || '1';
    return this.categoryService.getCategoriesForRestaurant(rId);
  });

  menuItems = computed(() => {
    const catIds = this.categories().map((c: Category) => c.id);
    return this.menuService.getMenuItemsForCategories(catIds);
  });

  totalMenuItemsCount  = computed(() => this.menuItems().length);
  totalCategoriesCount = computed(() => this.categories().length);

  popularDishName = computed(() => {
    const list = this.menuItems();
    return list.length > 0 ? list[0].name : 'Featured Menu Item';
  });

  totalScanCount = computed(() => {
    const kpiScans = this.analyticsService.dashboardKpi().totalScans;
    if (kpiScans > 0) return kpiScans;
    const qrScans = this.qrCodesList().reduce((sum: number, q: QrCodeData) => sum + (q.scansCount || 0), 0);
    return qrScans;
  });

  selectedFilterCategoryId = signal<string>('all');

  filteredMenuItems = computed(() => {
    const list = this.menuItems();
    const filterId = this.selectedFilterCategoryId();
    if (filterId === 'all') return list;
    return list.filter((item: MenuItem) =>
      String(item.categoryId).toLowerCase().replace(/^c/, '') === String(filterId).toLowerCase().replace(/^c/, '')
    );
  });

  paginatedMenuItems = computed(() => {
    const list = this.filteredMenuItems();
    const start = (this.currentPage() - 1) * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  totalPages = computed(() => {
    return Math.ceil(this.filteredMenuItems().length / this.pageSize()) || 1;
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

  // Support ticket
  showSupportModal  = signal<boolean>(false);
  supportSubject    = signal<string>('');
  supportMessage    = signal<string>('');
  supportPriority   = signal<string>('medium');

  ngOnInit() {
    this.analyticsService.loadDashboardMetrics().subscribe();
    this.orderService.fetchOrders(this.activeRestaurant()?.id || 1).subscribe();

    if (this.categories().length > 0 && !this.newItemCategoryId()) {
      this.newItemCategoryId.set(this.categories()[0].id);
    }

    if (this.qrCodesList().length > 0) {
      this.selectedQr.set(this.qrCodesList()[0]);
    }
  }

  ngOnDestroy() {}

  selectTab(tabName: string) {
    this.activeTab.set(tabName);
    this.editingCategoryId.set(null);
    this.editingItemId.set(null);
    this.currentPage.set(1);
    if (tabName === 'orders') {
      this.orderService.fetchOrders(this.activeRestaurant()?.id || 1).subscribe();
    }
  }

  // Orders Management
  updateOrderStatus(orderId: string, status: string) {
    const rId = this.activeRestaurant()?.id || '1';
    this.orderService.updateOrderStatus(orderId, status, rId).subscribe(() => {
      this.toastService.success('Order Updated', `Order ${orderId} status set to ${status}`);
    });
  }

  // Categories
  handleAddCategory() {
    if (!this.newCategoryName().trim()) {
      this.toastService.warning('Validation', 'Please enter a category name.');
      return;
    }
    const rId = this.activeRestaurant()?.id || '1';
    this.categoryService.addCategory({
      restaurantId: rId,
      name: this.newCategoryName().trim(),
      icon: this.newCategoryIcon()
    });
    this.newCategoryName.set('');
    this.toastService.success('Category Added', 'New menu category created successfully.');
  }

  editCategory(cat: Category) {
    this.editingCategoryId.set(cat.id);
    this.editingCategoryName.set(cat.name);
  }

  handleSaveCategory() {
    if (this.editingCategoryId()) {
      this.categoryService.updateCategory(this.editingCategoryId()!, this.editingCategoryName());
      this.editingCategoryId.set(null);
      this.toastService.success('Category Updated', 'Category updated successfully.');
    }
  }

  deleteCategory(id: string) {
    this.modalService.confirm({
      title: 'Delete Category',
      message: 'Are you sure you want to delete this category?',
      type: 'danger',
      confirmText: 'Delete Category',
      onConfirm: () => {
        this.categoryService.deleteCategory(id);
        this.toastService.info('Category Removed', 'Category was deleted.');
      }
    });
  }

  // Image Upload Handling
  handleImageFileSelected(event: Event) {
    this.onDishImageSelected(event);
  }

  clearImageSelection() {
    this.newItemImage.set('');
    this.imageUploadPreview.set('');
  }

  onDishImageSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = reader.result as string;
        this.imageUploadPreview.set(dataUrl);
        this.newItemImage.set(dataUrl);
      };
      reader.readAsDataURL(file);

      this.uploadService.uploadImage(file).subscribe(url => {
        if (url) {
          this.newItemImage.set(url);
          this.imageUploadPreview.set(url);
        }
      });
    }
  }

  onLogoSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = reader.result as string;
        this.restaurantLogoUrl.set(dataUrl);
        if (this.activeRestaurant()) {
          this.restaurantService.updateProfile(this.activeRestaurant()!.id, { logoUrl: dataUrl });
        }
      };
      reader.readAsDataURL(file);
    }
  }

  // Dishes / Items
  toggleItemAvailability(itemId: string) {
    this.menuService.toggleAvailability(itemId);
    this.toastService.info('Availability Updated', 'Dish availability updated.');
  }

  handleAddItem() {
    if (!this.newItemName().trim()) {
      this.toastService.warning('Missing Information', 'Please fill out dish title.');
      return;
    }

    const img = this.newItemImage().trim() || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80';
    const selectedCatId = this.newItemCategoryId() || (this.categories()[0]?.id || '1');

    this.menuService.addMenuItem({
      categoryId: selectedCatId,
      name: this.newItemName().trim(),
      price: this.newItemPrice(),
      description: this.newItemDescription().trim(),
      image: img,
      isAvailable: true,
      isVeg: this.newItemIsVeg(),
      spicyLevel: this.newItemSpicyLevel()
    });

    this.newItemName.set('');
    this.newItemDescription.set('');
    this.newItemImage.set('');
    this.imageUploadPreview.set('');
    this.toastService.success('Dish Added', 'New food item created and uploaded to menu.');
  }

  deleteMenuItem(id: string) {
    this.deleteItem(id);
  }

  deleteItem(id: string) {
    this.modalService.confirm({
      title: 'Delete Food Item',
      message: 'Are you sure you want to delete this food item from your menu?',
      type: 'danger',
      confirmText: 'Delete Dish',
      onConfirm: () => {
        this.menuService.deleteMenuItem(id);
        this.toastService.info('Dish Deleted', 'Menu item removed.');
      }
    });
  }

  // QR Regeneration & Management
  generateTableQr() {
    const tableNum = this.newTableNumber() || 'Table 01';
    const rId = this.activeRestaurant()?.id || '1';
    this.qrService.generateQrCode(rId, tableNum).subscribe(() => {
      this.toastService.success('QR Code Generated', `QR code generated for ${tableNum}`);
    });
  }

  selectQr(q: QrCodeData) {
    this.selectedQr.set(q);
  }

  deleteQr(id: string) {
    const rId = this.activeRestaurant()?.id || '1';
    this.qrService.deleteQrCode(rId, id).subscribe(() => {
      this.toastService.info('QR Code Deleted', 'QR code removed.');
    });
  }

  supportTicketSent = signal<boolean>(false);

  submitSupportTicket() {
    this.supportTicketSent.set(true);
    this.toastService.success('Ticket Submitted', 'Our support team will contact you.');
  }

  regenerateQrCode(qrId?: string) {
    const targetId = qrId || (this.selectedQr()?.id || '1');
    const numericId = parseInt(targetId, 10) || 1;
    const rId = parseInt(this.activeRestaurant()?.id || '1', 10) || 1;

    this.qrService.regenerateQr(numericId, rId).subscribe(() => {
      this.toastService.success('QR Regenerated', 'QR code token refreshed and menu updated.');
    });
  }

  updateMenuQr() {
    this.toastService.success('Menu Updated', 'Menu changes synced automatically across all QR codes & tables.');
  }

  activeQrUrl = computed(() => {
    const current = this.selectedQr();
    if (current?.qrCodeUrl || current?.qrImageUrl) return current.qrCodeUrl || current.qrImageUrl!;
    const tableNum = current?.tableNumber || this.newTableNumber() || 'Table 01';
    const token    = current?.qrToken || 'preview';
    const menuUrl  = `${environment.frontendUrl}/menu/${token}?table=${encodeURIComponent(tableNum.replace(/^Table\s*/i, ''))}`;
    return `https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=H&color=fc6011&data=${encodeURIComponent(menuUrl)}`;
  });

  activeTargetMenuUrl = computed(() => {
    const current = this.selectedQr();
    if (current?.targetUrl) return current.targetUrl;
    const tableNum = current?.tableNumber || 'Table 01';
    const token    = current?.qrToken || 'preview';
    return `${environment.frontendUrl}/menu/${token}?table=${encodeURIComponent(tableNum.replace(/^Table\s*/i, ''))}`;
  });

  simulateQrDownload() {
    this.qrDownloadSimulated.set(true);
    const link = document.createElement('a');
    link.href = this.activeQrUrl();
    link.download = `${this.selectedQr()?.tableNumber || 'Table'}_QR_Code.png`;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    this.toastService.success('Download Triggered', 'High-res QR image downloaded.');

    setTimeout(() => {
      this.qrDownloadSimulated.set(false);
    }, 1000);
  }

  simulateQrPrint() {
    const printWin = window.open('', '_blank');
    if (printWin) {
      printWin.document.write(`
        <html>
          <head><title>Print QR Code - ${this.selectedQr()?.tableNumber || 'Table'}</title></head>
          <body style="text-align:center; padding: 40px; font-family: sans-serif;">
            <h2>${this.activeRestaurant()?.name || 'Gourmet Bistro'}</h2>
            <h3>${this.selectedQr()?.tableNumber || 'Table 01'}</h3>
            <img src="${this.activeQrUrl()}" style="width:250px; height:250px; margin: 20px 0;" />
            <p>Scan to view digital menu & place order</p>
          </body>
        </html>
      `);
      printWin.document.close();
      printWin.focus();
      setTimeout(() => printWin.print(), 500);
    }
  }

  logout() {
    this.modalService.confirm({
      title: 'Sign Out Workspace',
      message: 'Are you sure you want to sign out of your owner portal session?',
      type: 'warning',
      confirmText: 'Sign Out',
      onConfirm: () => {
        this.authService.logout();
        this.toastService.info('Signed Out', 'You have been logged out.');
        this.router.navigate(['/login']);
      }
    });
  }
}
