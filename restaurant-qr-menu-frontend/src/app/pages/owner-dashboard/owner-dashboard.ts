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
  router            = inject(Router);

  // Active page state: 'overview' | 'categories' | 'items' | 'qr' | 'settings'
  activeTab = signal<string>('overview');

  // Active restaurant selection
  activeRestaurant = computed(() => this.restaurantService.getActiveRestaurant());

  // Scans history log & analytics chart
  scansHistory = signal<any[]>([]);
  chartData    = signal<any[]>([]);

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
  supportTicketSent = signal<boolean>(false);

  private autoRefreshTimer: any;

  ngOnInit() {
    this.refreshDashboardData();

    // ── Live auto-refresh polling every 5 seconds ──
    this.autoRefreshTimer = setInterval(() => {
      this.refreshDashboardData();
    }, 5000);
  }

  ngOnDestroy() {
    if (this.autoRefreshTimer) {
      clearInterval(this.autoRefreshTimer);
    }
  }

  refreshDashboardData() {
    const rId = this.activeRestaurant()?.id || '1';
    this.categoryService.fetchCategories(rId).subscribe();
    this.menuService.fetchMenuItems(rId).subscribe();
    this.offerService.fetchActiveOffers(rId).subscribe();
    this.qrService.fetchQrCodes(rId).subscribe((codes: QrCodeData[]) => {
      if (codes && codes.length > 0 && !this.selectedQr()) {
        this.selectedQr.set(codes[0]);
      }
    });
    this.analyticsService.fetchDashboardKpi(rId).subscribe();
    this.restaurantService.fetchRestaurantProfile(rId).subscribe();
  }

  submitSupportTicket() {
    if (!this.supportSubject().trim() || !this.supportMessage().trim()) {
      this.toastService.warning('Required Fields', 'Please enter subject and details for your ticket.');
      return;
    }
    const rId = this.activeRestaurant()?.id || '1';
    this.ticketService.createTicket(rId, this.supportSubject(), this.supportMessage(), this.supportPriority())
      .subscribe(() => {
        this.supportTicketSent.set(true);
        this.supportSubject.set('');
        this.supportMessage.set('');
        this.supportPriority.set('medium');
        this.toastService.success('Support Ticket Sent', 'Our team will review your request shortly.');
      });
  }

  selectTab(tabName: string) {
    this.activeTab.set(tabName);
    this.editingCategoryId.set(null);
    this.editingItemId.set(null);
    this.currentPage.set(1);
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
      title: 'Delete Division Category',
      message: 'Are you sure you want to delete this category? Dishes in this division will lose their group.',
      type: 'danger',
      confirmText: 'Delete Category',
      onConfirm: () => {
        this.categoryService.deleteCategory(id);
        this.toastService.info('Category Removed', 'Category was deleted.');
      }
    });
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

    // Default to first category if user didn't explicitly select one
    let targetCatId = this.newItemCategoryId();
    if (!targetCatId && this.categories().length > 0) {
      targetCatId = this.categories()[0].id;
    }

    if (!targetCatId) {
      this.toastService.warning('Category Required', 'Please create a category before adding items.');
      return;
    }

    this.menuService.addMenuItem({
      categoryId: targetCatId,
      name: this.newItemName().trim(),
      price: this.newItemPrice(),
      description: this.newItemDescription().trim(),
      image: this.newItemImage().trim() || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80',
      isAvailable: true,
      isVeg: this.newItemIsVeg(),
      spicyLevel: this.newItemSpicyLevel()
    });

    this.newItemName.set('');
    this.newItemPrice.set(12.00);
    this.newItemDescription.set('');
    this.newItemIsVeg.set(true);
    this.newItemSpicyLevel.set(0);
    this.imageUploadPreview.set('');
    this.newItemImage.set('');

    this.toastService.success('Dish Published!', 'Food item added to menu and saved successfully.');
  }

  handleImageFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.toastService.error('Invalid Format', 'Please select a valid image file.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.toastService.error('File Too Large', 'Please keep image size under 5MB.');
      return;
    }

    const rId = this.activeRestaurant()?.id || '1';
    this.toastService.info('Uploading Image', 'Sending media to Cloudinary storage...');
    this.uploadService.uploadImage(file, rId).subscribe({
      next: (url: string) => {
        this.imageUploadPreview.set(url);
        this.newItemImage.set(url);
        this.toastService.success('Image Uploaded', 'Cloudinary photo attached.');
      },
      error: () => {
        this.toastService.error('Upload Failed', 'Cloudinary fallback active.');
      }
    });
  }

  clearImageSelection() {
    this.imageUploadPreview.set('');
    this.newItemImage.set('');
  }

  deleteMenuItem(id: string) {
    this.modalService.confirm({
      title: 'Delete Dish Platter',
      message: 'Are you sure you want to delete this food item? It will be removed from customer digital menu.',
      type: 'danger',
      confirmText: 'Delete Dish',
      onConfirm: () => {
        this.menuService.deleteMenuItem(id);
        this.toastService.info('Dish Deleted', 'Food item removed.');
      }
    });
  }

  generateTableQr() {
    if (!this.newTableNumber().trim()) {
      this.toastService.warning('Table Required', 'Please enter a table number (e.g. Table 05).');
      return;
    }
    const rId = this.activeRestaurant()?.id || '1';
    this.qrService.generateQrCode(rId, this.newTableNumber().trim()).subscribe((qr: QrCodeData) => {
      this.selectedQr.set(qr);
      this.newTableNumber.set('');
      this.toastService.success('QR Generated', 'New table QR code rendered successfully.');
    });
  }

  selectQr(qr: QrCodeData) {
    this.selectedQr.set(qr);
  }

  deleteQr(id: string) {
    this.modalService.confirm({
      title: 'Delete Table QR',
      message: 'Delete this QR code? Guests will no longer be able to scan this table code.',
      type: 'danger',
      confirmText: 'Delete QR Code',
      onConfirm: () => {
        const rId = this.activeRestaurant()?.id || '1';
        this.qrService.deleteQrCode(rId, id).subscribe(() => {
          if (this.selectedQr()?.id === id) {
            this.selectedQr.set(this.qrCodesList()[0] || null);
          }
          this.toastService.info('QR Code Deleted', 'Table code removed.');
        });
      }
    });
  }

  activeQrUrl = computed(() => {
    const current = this.selectedQr();
    if (current?.qrCodeUrl) {
      return current.qrCodeUrl;
    }
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
