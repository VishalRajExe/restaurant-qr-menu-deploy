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
import { TicketService, SupportTicketData, TicketMessageData } from '../../services/ticket.service';
import { UploadService } from '../../services/upload.service';
import { ToastService } from '../../services/toast.service';
import { ModalService } from '../../services/modal.service';
import { OrderService, Order } from '../../services/order.service';
import { PrintService } from '../../services/print.service';
import { BackButton } from '../../components/back-button/back-button';
import { Category } from '../../models/category.model';
import { MenuItem } from '../../models/menu-item.model';
import { environment } from '../../environments/environment';
import { NotificationCenter } from '../../components/notification-center/notification-center';
import { NotificationService } from '../../services/notification.service';
import { TableService, DiningTableData, TableStatus, TableStatsData } from '../../services/table.service';
import { UndoService } from '../../services/undo.service';
import { ChatService, ChatContact, ChatMessageItem } from '../../services/chat.service';
import { CustomerHistoryService, CustomerHistoryData, CustomerSummary } from '../../services/customer-history.service';

@Component({
  selector: 'app-owner-dashboard',
  imports: [CommonModule, FormsModule, NotificationCenter, RouterLink],
  templateUrl: './owner-dashboard.html',
  styleUrls: ['./owner-dashboard.css']
})
export class OwnerDashboard implements OnInit, OnDestroy {
  authService            = inject(AuthService);
  restaurantService      = inject(RestaurantService);
  categoryService        = inject(CategoryService);
  menuService            = inject(MenuService);
  offerService           = inject(OfferService);
  qrService              = inject(QrService);
  analyticsService       = inject(AnalyticsService);
  ticketService          = inject(TicketService);
  tableService           = inject(TableService);
  customerHistoryService = inject(CustomerHistoryService);
  uploadService          = inject(UploadService);
  toastService           = inject(ToastService);
  printService           = inject(PrintService);
  modalService           = inject(ModalService);
  orderService           = inject(OrderService);
  notificationService    = inject(NotificationService);
  undoService            = inject(UndoService);
  chatService            = inject(ChatService);
  router                 = inject(Router);

  // Active page state: 'overview' | 'orders' | 'categories' | 'items' | 'tables' | 'customers' | 'qr' | 'support' | 'settings'
  activeTab = signal<string>('overview');

  // Active restaurant selection
  activeRestaurant = computed(() => this.restaurantService.getActiveRestaurant());

  // Scans history log & analytics chart
  scansHistory = signal<any[]>([]);
  chartData    = signal<any[]>([]);

  // Orders Signal
  ordersList = computed(() => this.orderService.ordersSignal()());
  activeOrderFilter = signal<string>('ALL');
  selectedOrder = signal<Order | null>(null);

  // Unseen Orders Count for Sidebar Badge (clears to 0 when Orders tab is clicked)
  pendingOrdersCount = computed(() => this.orderService.unseenOrdersCount());

  // ── Smart Topbar Search with Navigation Tabs & Jump to Items ────────
  topSearchQuery   = signal<string>('');
  isSearchFocused  = signal<boolean>(false);

  readonly navigationTabs = [
    { id: 'overview', name: 'Dashboard Overview', category: 'Navigation', icon: 'dashboard', description: 'Metrics & Quick Stats' },
    { id: 'orders', name: 'Live Orders', category: 'Navigation', icon: 'receipt_long', description: 'Kitchen & Active Orders' },
    { id: 'items', name: 'Menu Catalog', category: 'Navigation', icon: 'restaurant_menu', description: 'Dishes, Pricing & Availability' },
    { id: 'tables', name: 'Tables & Floor Plan', category: 'Navigation', icon: 'table_restaurant', description: 'Dining Tables & Live Sessions' },
    { id: 'customers', name: 'Customer History & Tracking', category: 'Navigation', icon: 'person_search', description: 'Search Orders by 10-Digit Mobile' },
    { id: 'categories', name: 'Categories', category: 'Navigation', icon: 'category', description: 'Menu Categories' },
    { id: 'qr', name: 'QR Codes Generator', category: 'Navigation', icon: 'qr_code_2', description: 'Table QR Codes & Tokens' },
    { id: 'support', name: 'Support & Help Desk', category: 'Navigation', icon: 'support_agent', description: 'Support Tickets & Help' },
    { id: 'settings', name: 'Restaurant Settings', category: 'Navigation', icon: 'settings', description: 'Branding & Configuration' },
  ];

  filteredSearchTabs = computed(() => {
    const q = this.topSearchQuery().toLowerCase().trim();
    if (!q) return [];
    return this.navigationTabs.filter(t =>
      t.name.toLowerCase().includes(q) ||
      t.description.toLowerCase().includes(q) ||
      t.id.toLowerCase().includes(q)
    );
  });

  filteredSearchDishes = computed(() => {
    const q = this.topSearchQuery().toLowerCase().trim();
    if (!q) return [];
    return this.menuItems().filter(m =>
      m.name.toLowerCase().includes(q) ||
      (m.description && m.description.toLowerCase().includes(q))
    ).slice(0, 4);
  });

  filteredSearchTables = computed(() => {
    const q = this.topSearchQuery().toLowerCase().trim();
    if (!q) return [];
    return this.tablesList().filter(t =>
      t.tableNumber.toLowerCase().includes(q) ||
      t.status.toLowerCase().includes(q)
    ).slice(0, 4);
  });

  filteredSearchOrders = computed(() => {
    const q = this.topSearchQuery().toLowerCase().trim();
    if (!q) return [];
    return this.ordersList().filter(o =>
      (o.orderNumber && o.orderNumber.toLowerCase().includes(q)) ||
      (o.customerName && o.customerName.toLowerCase().includes(q)) ||
      (o.tableNumber && String(o.tableNumber).includes(q))
    ).slice(0, 4);
  });

  navigateFromSearch(result: any) {
    this.isSearchFocused.set(false);
    this.topSearchQuery.set('');

    if (result.category === 'Navigation' || (result.id && typeof result.id === 'string' && !result.type)) {
      this.selectTab(result.id);
    } else if (result.type === 'dish') {
      this.selectTab('items');
      this.menuSearchQuery.set(result.raw.name);
    } else if (result.type === 'table') {
      this.selectTab('tables');
      this.openTableDetails(result.raw);
    } else if (result.type === 'order') {
      this.selectTab('orders');
      this.selectedOrder.set(result.raw);
    }
  }

  // ── Direct Owner <-> Chef Chat Signals ───────────────────────────
  showDirectChatModal     = signal<boolean>(false);
  directChatMessageInput  = signal<string>('');
  unreadChatCount         = computed(() => this.chatService.unreadTotalCount());
  chatContacts            = computed(() => this.chatService.contacts());
  activeChatContact       = computed(() => this.chatService.activeContact());
  activeChatThread        = computed(() => this.chatService.activeThread());
  isSendingChat           = computed(() => this.chatService.isSending());
  isLoadingThread         = computed(() => this.chatService.isLoadingThread());

  openDirectChatModal(contact?: ChatContact) {
    this.showDirectChatModal.set(true);
    const rId = this.activeRestaurant()?.id || 1;
    this.chatService.fetchContacts(rId).subscribe(contacts => {
      if (contact) {
        this.chatService.loadThread(rId, contact).subscribe();
      } else if (contacts && contacts.length > 0) {
        const target = this.chatService.activeContact() || contacts[0];
        this.chatService.loadThread(rId, target).subscribe();
      }
    });
  }

  closeDirectChatModal() {
    this.showDirectChatModal.set(false);
  }

  selectChatContact(contact: ChatContact) {
    const rId = this.activeRestaurant()?.id || 1;
    this.chatService.loadThread(rId, contact).subscribe();
  }

  sendDirectMessage() {
    const text = this.directChatMessageInput().trim();
    const contact = this.chatService.activeContact();
    if (!text || !contact) return;

    const rId = this.activeRestaurant()?.id || 1;
    this.directChatMessageInput.set('');
    this.chatService.sendMessage(rId, contact.userId, text).subscribe();
  }

  sendQuickChatMessage(snippet: string) {
    this.directChatMessageInput.set(snippet);
    this.sendDirectMessage();
  }

  // Support Ticket signals & badges
  ownerTickets = computed(() => this.ticketService.ticketsList());
  unseenTicketsCount = computed(() => this.ticketService.unseenTicketsCount());
  supportFilter = signal<'ALL' | 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'>('ALL');
  selectedTicketDetails = signal<{ ticket: SupportTicketData; messages: TicketMessageData[] } | null>(null);
  showTicketDetailsModal = signal<boolean>(false);
  newReplyMessage = signal<string>('');
  isSendingReply = signal<boolean>(false);
  showCreateTicketModal = signal<boolean>(false);
  ticketCategory = signal<string>('TECHNICAL_ISSUE');
  ticketPriority = signal<string>('MEDIUM');
  ticketSubject = signal<string>('');
  ticketDescription = signal<string>('');

  // ── Dining Table Management Signals ──────────────────────────────
  tablesList = computed(() => this.tableService.tablesList());
  tableStats = computed(() => this.tableService.tableStats());
  activeTableFilter = signal<'ALL' | 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'CLEANING'>('ALL');
  selectedTableDetails = signal<DiningTableData | null>(null);
  showTableDetailsModal = signal<boolean>(false);
  showAddTableModal = signal<boolean>(false);
  showReserveTableModal = signal<boolean>(false);
  showConfirmAvailableModal = signal<boolean>(false);
  tableToMakeAvailable = signal<DiningTableData | null>(null);

  // Add/Edit Table Form
  tableFormNumber = signal<string>('');
  tableFormCapacity = signal<number>(4);
  editingTableId = signal<number | null>(null);

  // Reserve Table Form
  reserveGuestName = signal<string>('');
  reserveGuestPhone = signal<string>('');
  reserveTime = signal<string>('');
  reserveGuestCount = signal<number>(2);
  reserveNotes = signal<string>('');

  filteredTables = computed(() => {
    const list = this.tablesList();
    const f = this.activeTableFilter();
    if (f === 'ALL') return list;
    return list.filter(t => t.status === f);
  });

  filteredOwnerTickets = computed(() => {
    const list = this.ownerTickets();
    const f = this.supportFilter();
    if (f === 'ALL') return list;
    return list.filter(t => t.status.toUpperCase() === f);
  });

  private pollTimer: any;

  ngOnInit() {
    this.loadInitialData();
    // Background polling every 2.5s so Chef, Chat, & Table status changes reflect immediately
    this.pollTimer = setInterval(() => {
      const rId = this.activeRestaurant()?.id || 1;
      this.orderService.fetchOrders(rId).subscribe(orders => {
        const cur = this.selectedOrder();
        if (cur && orders && orders.length > 0) {
          const fresh = orders.find(o => o.id === cur.id || o.orderNumber === cur.orderNumber);
          if (fresh) {
            if (fresh.status !== cur.status) {
              this.selectedOrder.set(fresh);
              if (fresh.status === 'READY') {
                this.toastService.show(`Chef Alert: Order #${fresh.orderNumber || fresh.id} (Table ${fresh.tableNumber}) is ready for serving.`, 'success');
              }
            }
          }
        }
      });
      this.tableService.fetchTables(rId).subscribe(tables => {
        const curTable = this.selectedTableDetails();
        if (curTable) {
          const freshTable = tables.find(t => t.id === curTable.id);
          if (freshTable) {
            this.selectedTableDetails.set(freshTable);
          }
        }
      });
      // Poll chat
      this.chatService.fetchUnreadCount(rId);
      if (this.showDirectChatModal()) {
        const activeC = this.chatService.activeContact();
        if (activeC) {
          this.chatService.refreshThreadSilently(rId, activeC.userId);
        }
      }
      if (this.activeTab() === 'support') {
        this.loadOwnerTickets();
      }
    }, 2500);
  }

  ngOnDestroy() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
    }
  }

  loadInitialData() {
    const userSession = this.authService.currentUser();
    const rId = userSession?.restaurantId || this.activeRestaurant()?.id || '1';
    this.restaurantService.fetchRestaurantProfile(rId).subscribe(rest => {
      if (rest) {
        this.restaurantService.setRestaurant(rest);
      }
    });

    this.tableService.fetchTables(rId).subscribe();
    this.tableService.fetchStats(rId).subscribe();

    this.categoryService.fetchCategories(rId).subscribe(cats => {
      if (cats && cats.length > 0 && !this.newItemCategoryId()) {
        this.newItemCategoryId.set(cats[0].id);
      }
    });

    this.menuService.fetchMenuItems(rId).subscribe();
    this.qrService.fetchQrCodes(rId).subscribe(qrs => {
      if (qrs && qrs.length > 0) {
        this.selectedQr.set(qrs[0]);
      }
    });

    this.analyticsService.loadDashboardMetrics().subscribe();
    this.orderService.fetchOrders(rId).subscribe();
    this.loadOwnerTickets();
  }

  selectTab(tab: string) {
    this.activeTab.set(tab);
    this.editingCategoryId.set(null);
    this.editingItemId.set(null);
    this.currentPage.set(1);
    const rId = this.activeRestaurant()?.id || '1';

    if (tab === 'orders') {
      this.orderService.markOrdersAsSeen(rId);
      this.orderService.fetchOrders(rId).subscribe();
    } else if (tab === 'customers') {
      this.customerHistoryService.fetchRecentCustomers(rId).subscribe();
      this.orderService.fetchOrders(rId).subscribe();
    } else if (tab === 'support') {
      this.ticketService.markTicketsAsSeen();
      this.loadOwnerTickets();
    } else if (tab === 'items' || tab === 'categories') {
      this.categoryService.fetchCategories(rId).subscribe();
      this.menuService.fetchMenuItems(rId).subscribe();
    } else if (tab === 'qr') {
      this.qrService.fetchQrCodes(rId).subscribe();
    }
  }

  loadOwnerTickets() {
    const rId = this.activeRestaurant()?.id || 1;
    this.ticketService.fetchOwnerTickets(rId).subscribe();
  }

  openCreateTicketModal() {
    this.ticketSubject.set('');
    this.ticketDescription.set('');
    this.ticketCategory.set('TECHNICAL_ISSUE');
    this.ticketPriority.set('MEDIUM');
    this.showCreateTicketModal.set(true);
  }

  submitOwnerTicket() {
    if (!this.ticketSubject().trim() || !this.ticketDescription().trim()) {
      this.toastService.show('Please provide a subject and details for your ticket', 'warning');
      return;
    }
    const rId = this.activeRestaurant()?.id || 1;
    this.ticketService.createTicket({
      restaurantId: rId,
      category: this.ticketCategory(),
      priority: this.ticketPriority(),
      subject: this.ticketSubject().trim(),
      description: this.ticketDescription().trim()
    }).subscribe(created => {
      this.toastService.success(
        `Ticket #${created.ticketNumber || created.id} Created`,
        `Subject: ${created.subject} • Status: ${created.status} • Date: ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
      );
      this.showCreateTicketModal.set(false);
      this.loadOwnerTickets();
      if (created) {
        this.openTicketDetails(created);
      }
    });
  }

  openTicketDetails(ticket: SupportTicketData) {
    this.ticketService.markTicketsAsSeen();
    this.ticketService.getTicketDetails(ticket.id).subscribe(res => {
      this.selectedTicketDetails.set(res || { ticket, messages: [] });
      this.showTicketDetailsModal.set(true);
    });
  }

  escalateTicketToAdmin(ticketId: string | number, reason?: string) {
    this.ticketService.escalateTicket(ticketId, reason || 'Owner escalated issue to platform administrators').subscribe(() => {
      this.toastService.success('Escalated to Admin Desk', 'Platform Super Admin team has received this incident.');
      this.loadOwnerTickets();
      if (this.selectedTicketDetails()?.ticket?.id === String(ticketId)) {
        this.openTicketDetails({ ...this.selectedTicketDetails()!.ticket, isEscalated: true, status: 'IN_PROGRESS' });
      }
    });
  }

  sendTicketReply() {
    const text = this.newReplyMessage().trim();
    const details = this.selectedTicketDetails();
    if (!text || !details?.ticket) return;

    this.isSendingReply.set(true);
    this.ticketService.addMessage(details.ticket.id, text).subscribe(msg => {
      this.isSendingReply.set(false);
      this.newReplyMessage.set('');
      if (msg) {
        this.selectedTicketDetails.update(d => d ? { ...d, messages: [...d.messages, msg] } : d);
        this.toastService.success('Reply Sent', 'Your message has been posted.');
      }
    });
  }

  resolveOwnerTicket(ticketId: string) {
    this.ticketService.updateTicketStatus(ticketId, 'RESOLVED').subscribe(() => {
      this.toastService.success('Ticket Resolved', 'Ticket marked as resolved.');
      this.selectedTicketDetails.update(d => d ? { ...d, ticket: { ...d.ticket, status: 'RESOLVED' } } : d);
      this.loadOwnerTickets();
    });
  }

  filteredOrders = computed(() => {
    const list = this.ordersList();
    const filter = this.activeOrderFilter();
    if (filter === 'ALL') return list;
    return list.filter(o => o.status.toUpperCase() === filter.toUpperCase());
  });

  selectOrder(order: Order) {
    this.selectedOrder.set(order);
  }

  updateOrderStatus(orderId: string, newStatus: string) {
    const rId = this.activeRestaurant()?.id || 1;
    this.orderService.updateOrderStatus(orderId, newStatus, rId).subscribe(() => {
      const current = this.selectedOrder();
      if (current && (current.id === orderId || current.orderNumber === orderId)) {
        this.selectedOrder.set({ ...current, status: newStatus.toUpperCase() as any });
      }
      this.toastService.success('Order Updated', `Order marked as ${newStatus}`);
      this.notificationService.pushNotification({
        eventType: 'ORDER_STATUS_CHANGED',
        title: 'Order Status Updated',
        message: `Order #${orderId} status changed to ${newStatus}.`
      });
    });
  }

  cancelOrder(orderId: string) {
    this.modalService.confirm({
      title: 'Cancel Order',
      message: `Are you sure you want to cancel Order #${orderId}?`,
      type: 'danger',
      confirmText: 'Cancel Order',
      onConfirm: () => {
        this.updateOrderStatus(orderId, 'CANCELLED');
      }
    });
  }

  getOrderSubtotal(order: Order | null): number {
    if (!order) return 0;
    if (order.totalAmount) return order.totalAmount;
    return order.items.reduce((sum, i) => sum + (i.subtotal || (i.price || 15) * i.qty), 0);
  }

  getOrderTax(order: Order | null): number {
    return +(this.getOrderSubtotal(order) * 0.08).toFixed(2);
  }

  getOrderServiceCharge(order: Order | null): number {
    return +(this.getOrderSubtotal(order) * 0.05).toFixed(2);
  }

  getOrderTotal(order: Order | null): number {
    const sub = this.getOrderSubtotal(order);
    return +(sub + this.getOrderTax(order) + this.getOrderServiceCharge(order)).toFixed(2);
  }

  printKOT(order?: Order | null) {
    const target = order || this.selectedOrder();
    if (!target) {
      this.toastService.show('Please select an order to print KOT', 'warning');
      return;
    }
    const r = this.activeRestaurant();
    this.printService.printKOT(target, {
      name: r?.name || 'RestQR Gourmet Bistro',
      address: r?.address || '123 Gourmet Blvd, New York, NY',
      phone: r?.phone || '+1 (555) 345-6789'
    });
    this.toastService.success('KOT Dispatched', `Kitchen ticket printed for Table ${target.tableNumber || '01'}`);
  }

  generateInvoice(order?: Order | null) {
    const target = order || this.selectedOrder();
    if (!target) {
      this.toastService.show('Please select an order to generate invoice', 'warning');
      return;
    }
    const r = this.activeRestaurant();
    this.printService.printInvoice(target, {
      name: r?.name || 'RestQR Gourmet Bistro',
      address: r?.address || '123 Gourmet Blvd, New York, NY',
      phone: r?.phone || '+1 (555) 345-6789',
      email: r?.email || 'contact@restqr.com',
      currency: '$'
    });
    this.toastService.success('Tax Invoice Generated', `Invoice ready for Order #${target.orderNumber || target.id}`);
  }

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
  newTableDigit       = signal<number>(1);

  onTableDigitInput(event: Event) {
    const raw = (event.target as HTMLInputElement).value;
    if (!raw) return;
    let val = parseInt(raw, 10);
    if (isNaN(val)) val = 1;
    val = Math.max(1, Math.min(99, val));
    this.newTableDigit.set(val);
    const formatted = `Table ${String(val).padStart(2, '0')}`;
    this.newTableNumber.set(formatted);
  }

  // Pagination states
  currentPage = signal<number>(1);
  pageSize    = signal<number>(6);

  // QR Codes signal selector
  qrCodesList = computed(() => this.qrService.qrCodesList());
  selectedQr  = signal<QrCodeData | null>(null);
  enlargedQrModal = signal<QrCodeData | null>(null);

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
  menuSearchQuery          = signal<string>('');

  filteredMenuItems = computed(() => {
    let list = this.menuItems();
    const filterId = this.selectedFilterCategoryId();
    const query = this.menuSearchQuery().trim().toLowerCase();

    if (filterId !== 'all') {
      list = list.filter((item: MenuItem) =>
        String(item.categoryId).toLowerCase().replace(/^c/, '') === String(filterId).toLowerCase().replace(/^c/, '')
      );
    }

    if (query) {
      list = list.filter((item: MenuItem) =>
        item.name.toLowerCase().includes(query) ||
        (item.description && item.description.toLowerCase().includes(query))
      );
    }

    return list;
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

  // Add Item Modal
  showAddItemModal  = signal<boolean>(false);

  // Profile Settings
  settingName    = signal<string>('RestQR Gourmet Bistro');
  settingAddress = signal<string>('124 Culinary Boulevard, Downtown Gourmet District');
  settingPhone   = signal<string>('+1 (555) 234-5678');

  chefInviteCode = computed(() => {
    return this.authService.currentUser()?.chefInviteCode || (this.activeRestaurant() as any)?.chefInviteCode || 'CHEF-REST01';
  });

  restaurantSlug = computed(() => {
    return this.authService.currentUser()?.restaurantSlug || (this.activeRestaurant() as any)?.slug || 'gourmet-bistro';
  });

  // Chef Code Editing States & Actions
  isEditingChefCode = signal<boolean>(false);
  editedChefCode    = signal<string>('');
  isSavingChefCode  = signal<boolean>(false);

  startEditingChefCode() {
    this.editedChefCode.set(this.chefInviteCode());
    this.isEditingChefCode.set(true);
  }

  cancelEditingChefCode() {
    this.isEditingChefCode.set(false);
  }

  saveChefCode() {
    const raw = this.editedChefCode().trim().toUpperCase();
    if (!raw || raw.length < 3) {
      this.toastService.show('Chef code must be at least 3 characters long', 'warning');
      return;
    }
    const rId = this.activeRestaurant()?.id || '1';
    this.isSavingChefCode.set(true);

    this.restaurantService.updateChefInviteCode(rId, raw).subscribe({
      next: () => {
        this.isSavingChefCode.set(false);
        this.isEditingChefCode.set(false);
        this.toastService.success('Chef Code Updated', `New kitchen registration code is ${raw}`);
        
        // Update user session and active restaurant state
        const curUser = this.authService.currentUser();
        if (curUser) {
          this.authService.updateCurrentUserState({ chefInviteCode: raw });
        }
      },
      error: (err) => {
        this.isSavingChefCode.set(false);
        const msg = err?.error?.message || 'Failed to update chef code. Code may already be taken.';
        this.toastService.show(msg, 'error');
      }
    });
  }

  copyChefInviteCode() {
    navigator.clipboard.writeText(this.chefInviteCode());
    this.toastService.success('Copied to Clipboard', `Chef registration code ${this.chefInviteCode()} copied.`);
  }

  copyPublicMenuUrl() {
    const url = `${window.location.origin}/restaurant/${this.restaurantSlug()}`;
    navigator.clipboard.writeText(url);
    this.toastService.success('Copied Public Menu URL', url);
  }

  // ══════════════════════════════════════════════════════════════════════════
  // CUSTOMER ORDER HISTORY & TRACKING MODULE
  // ══════════════════════════════════════════════════════════════════════════
  customerSearchPhone        = signal<string>('');
  customerPhoneError         = signal<string | null>(null);
  customerHistoryData        = computed(() => this.customerHistoryService.activeCustomerHistory());
  recentCustomersList        = computed(() => this.customerHistoryService.recentCustomers());
  isSearchingCustomer        = computed(() => this.customerHistoryService.isLoading());
  customerHistorySearchError = computed(() => this.customerHistoryService.searchError());

  // Filters for Customer's Orders
  customerOrderDateFilter    = signal<'ALL' | 'TODAY' | 'WEEK' | 'MONTH'>('ALL');
  customerOrderStatusFilter  = signal<string>('ALL');
  customerOrderTypeFilter    = signal<string>('ALL');
  customerOrderSearchQuery   = signal<string>('');

  filteredCustomerOrders = computed(() => {
    const data = this.customerHistoryData();
    if (!data || !data.orders) return [];

    let list = [...data.orders];
    const dateF = this.customerOrderDateFilter();
    const statusF = this.customerOrderStatusFilter();
    const typeF = this.customerOrderTypeFilter();
    const q = this.customerOrderSearchQuery().toLowerCase().trim();
    const now = Date.now();

    if (dateF !== 'ALL') {
      list = list.filter(o => {
        const d = o.createdAt || o.placedAt;
        const orderTime = d ? new Date(d).getTime() : now;
        const diffMs = now - orderTime;
        if (dateF === 'TODAY') return diffMs <= 86400000;
        if (dateF === 'WEEK') return diffMs <= 7 * 86400000;
        if (dateF === 'MONTH') return diffMs <= 30 * 86400000;
        return true;
      });
    }

    if (statusF !== 'ALL') {
      list = list.filter(o => String(o.status).toUpperCase() === statusF.toUpperCase());
    }

    if (typeF !== 'ALL') {
      list = list.filter(o => {
        const tblStr = String(o.tableNumber || o.table || '');
        if (typeF === 'TAKEAWAY') return tblStr === 'TAKEAWAY';
        return tblStr === typeF || tblStr.includes(typeF);
      });
    }

    if (q) {
      list = list.filter(o =>
        (o.orderNumber && o.orderNumber.toLowerCase().includes(q)) ||
        (o.items && o.items.some(i => {
          const name = i.itemName || i.name || '';
          return name.toLowerCase().includes(q);
        }))
      );
    }

    return list;
  });

  // Tracking & Timeline Modal
  trackingOrder          = signal<Order | null>(null);
  showOrderTrackingModal = signal<boolean>(false);

  searchCustomerByPhone(presetPhone?: string) {
    const raw = (presetPhone !== undefined ? presetPhone : this.customerSearchPhone()).trim();
    const clean = raw.replace(/\D/g, '');
    if (!clean) {
      this.customerPhoneError.set('Please enter a 10-digit customer mobile number');
      return;
    }
    if (clean.length !== 10) {
      this.customerPhoneError.set(`Phone number must be exactly 10 digits (you entered ${clean.length} digits)`);
      return;
    }
    this.customerPhoneError.set(null);
    this.customerSearchPhone.set(clean);
    const rId = this.activeRestaurant()?.id || 1;
    this.customerHistoryService.fetchCustomerHistory(rId, clean).subscribe(res => {
      if (res && res.totalOrders > 0) {
        this.toastService.success('Customer Profile Found', `${res.customerName} has placed ${res.totalOrders} orders at this restaurant.`);
      } else {
        this.toastService.info('No Order History', `No previous orders found for mobile ${clean} at this restaurant.`);
      }
    });
  }

  selectRecentCustomer(cust: CustomerSummary) {
    this.customerSearchPhone.set(cust.customerMobile);
    this.searchCustomerByPhone(cust.customerMobile);
  }

  openOrderTrackingModal(order: Order) {
    this.trackingOrder.set(order);
    this.showOrderTrackingModal.set(true);
  }

  closeOrderTrackingModal() {
    this.showOrderTrackingModal.set(false);
    this.trackingOrder.set(null);
  }

  isStatusDone(status: string | undefined, step: string): boolean {
    const s = (status || '').toUpperCase();
    if (s === 'CANCELLED') return false;
    switch (step) {
      case 'PLACED':
      case 'CONFIRMED':
        return true;
      case 'COOKING':
        return ['PREPARING', 'READY', 'COMPLETED', 'DELIVERED'].includes(s);
      case 'READY':
        return ['READY', 'COMPLETED', 'DELIVERED'].includes(s);
      case 'COMPLETED':
        return ['COMPLETED', 'DELIVERED'].includes(s);
      default:
        return false;
    }
  }

  isStatusActive(status: string | undefined, step: string): boolean {
    const s = (status || '').toUpperCase();
    switch (step) {
      case 'COOKING':
        return s === 'PREPARING';
      case 'READY':
        return s === 'READY';
      case 'COMPLETED':
        return ['COMPLETED', 'DELIVERED'].includes(s);
      default:
        return false;
    }
  }

  printCustomerReceipt(order: Order) {
    const r = this.activeRestaurant();
    this.printService.printInvoice(order, {
      name: r?.name || 'RestQR Gourmet Bistro',
      address: r?.address || '123 Gourmet Blvd, New York, NY',
      phone: r?.phone || '+1 (555) 345-6789',
      email: r?.email || 'contact@restqr.com',
      currency: r?.currency || '$'
    });
  }

  openAddItemModal() {
    this.cancelEditMenuItem();
    this.showAddItemModal.set(true);
  }

  closeAddItemModal() {
    this.showAddItemModal.set(false);
  }

  createNewItem() {
    this.openAddItemModal();
  }

  openSupportModal() {
    this.showSupportModal.set(true);
  }

  closeSupportModal() {
    this.showSupportModal.set(false);
  }

  saveRestaurantSettings() {
    this.toastService.success('Settings Saved', 'Restaurant profile settings updated.');
    this.notificationService.pushNotification({
      eventType: 'ACCOUNT_UPDATED',
      title: 'Restaurant Profile Updated',
      message: 'Profile name, address and phone contact synchronized.'
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
    const target = this.categories().find(c => c.id === id);
    const catName = target?.name || 'Category';

    this.categoryService.deleteCategory(id);
    this.undoService.showUndo(`Category "${catName}" deleted`, () => this.categoryService.restoreCategory(id, target), 7);
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

  startEditMenuItem(dish: MenuItem) {
    this.editingItemId.set(dish.id);
    this.newItemName.set(dish.name);
    this.newItemPrice.set(dish.price);
    this.newItemDescription.set(dish.description || '');
    this.newItemImage.set(dish.image || '');
    this.imageUploadPreview.set(dish.image || '');
    this.newItemIsVeg.set(dish.isVeg);
    this.newItemCategoryId.set(dish.categoryId);
    this.newItemSpicyLevel.set(dish.spicyLevel || 0);
  }

  cancelEditMenuItem() {
    this.editingItemId.set(null);
    this.newItemName.set('');
    this.newItemPrice.set(12.00);
    this.newItemDescription.set('');
    this.newItemImage.set('');
    this.imageUploadPreview.set('');
    this.newItemIsVeg.set(true);
    this.newItemSpicyLevel.set(0);
  }

  handleAddItem() {
    if (!this.newItemName().trim()) {
      this.toastService.warning('Missing Information', 'Please fill out dish title.');
      return;
    }

    const img = this.newItemImage().trim() || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80';
    const selectedCatId = this.newItemCategoryId() || (this.categories()[0]?.id || '1');

    if (this.editingItemId()) {
      const editId = this.editingItemId()!;
      this.menuService.updateMenuItem(editId, {
        categoryId: selectedCatId,
        name: this.newItemName().trim(),
        price: this.newItemPrice(),
        description: this.newItemDescription().trim(),
        image: img,
        isVeg: this.newItemIsVeg(),
        spicyLevel: this.newItemSpicyLevel()
      });
      const updatedName = this.newItemName().trim();
      this.cancelEditMenuItem();
      this.showAddItemModal.set(false);
      this.toastService.success('Dish Updated', 'Menu item changes saved successfully.');
      this.notificationService.pushNotification({
        eventType: 'MENU_ITEM_UPDATED',
        title: 'Menu Item Updated',
        message: `Dish "${updatedName}" modified in catalogue.`
      });
    } else {
      const createdName = this.newItemName().trim();
      this.menuService.addMenuItem({
        categoryId: selectedCatId,
        name: createdName,
        price: this.newItemPrice(),
        description: this.newItemDescription().trim(),
        image: img,
        isAvailable: true,
        isVeg: this.newItemIsVeg(),
        spicyLevel: this.newItemSpicyLevel()
      });
      this.cancelEditMenuItem();
      this.showAddItemModal.set(false);
      this.toastService.success('Dish Added', `New food item "${createdName}" uploaded to menu.`);
      this.notificationService.pushNotification({
        eventType: 'MENU_ITEM_ADDED',
        title: 'New Dish Added',
        message: `"${createdName}" added to digital menu for diners.`
      });
    }
  }

  deleteMenuItem(id: string) {
    this.deleteItem(id);
  }

  deleteItem(id: string) {
    const target = this.menuItems().find(i => i.id === id);
    const dishName = target?.name || 'Dish';

    this.menuService.deleteMenuItem(id);
    this.undoService.showUndo(`Dish "${dishName}" deleted`, () => this.menuService.restoreMenuItem(id, target), 7);
  }

  // QR Regeneration & Management
  generateTableQr() {
    const digit = this.newTableDigit() || 1;
    const tableNum = `Table ${String(digit).padStart(2, '0')}`;
    const rId = this.activeRestaurant()?.id || '1';
    this.qrService.generateQrCode(rId, tableNum).subscribe(() => {
      this.toastService.success('QR Code Generated', `QR code generated for ${tableNum}`);
      this.notificationService.pushNotification({
        eventType: 'QR_GENERATED',
        title: 'New Table QR Generated',
        message: `${tableNum} QR code generated successfully and ready for print.`
      });
    });
  }

  selectQr(q: QrCodeData) {
    this.selectedQr.set(q);
  }

  openEnlargedQr(q: QrCodeData) {
    this.selectedQr.set(q);
    this.enlargedQrModal.set(q);
  }

  closeEnlargedQr() {
    this.enlargedQrModal.set(null);
  }

  deleteQr(id: string) {
    const rId = this.activeRestaurant()?.id || '1';
    this.qrService.deleteQrCode(rId, id).subscribe(() => {
      this.toastService.info('QR Code Deleted', 'QR code removed.');
    });
  }

  submitSupportTicket() {
    if (!this.supportSubject().trim() || !this.supportMessage().trim()) {
      this.toastService.show('Please enter a subject and message details', 'warning');
      return;
    }
    this.toastService.success('Ticket Submitted', 'Our technical support team has received your request.');
    this.notificationService.pushNotification({
      eventType: 'SYSTEM_ALERT',
      title: 'Support Inquiry Dispatched',
      message: `Ticket "${this.supportSubject()}" submitted with priority: ${this.supportPriority().toUpperCase()}`
    });
    this.supportSubject.set('');
    this.supportMessage.set('');
    this.showSupportModal.set(false);
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

  // ── Table Management Operations ─────────────────────────────
  openTableDetails(table: DiningTableData) {
    this.selectedTableDetails.set(table);
    this.showTableDetailsModal.set(true);
  }

  closeTableDetails() {
    this.showTableDetailsModal.set(false);
    this.selectedTableDetails.set(null);
  }

  openAddTableModal() {
    this.editingTableId.set(null);
    const nextNum = this.tablesList().length + 1;
    this.tableFormNumber.set(`Table ${nextNum < 10 ? '0' + nextNum : nextNum}`);
    this.tableFormCapacity.set(4);
    this.showAddTableModal.set(true);
  }

  openEditTableModal(table: DiningTableData, event?: Event) {
    if (event) event.stopPropagation();
    this.editingTableId.set(table.id);
    this.tableFormNumber.set(table.tableNumber);
    this.tableFormCapacity.set(table.capacity);
    this.showAddTableModal.set(true);
  }

  // Quick Table Picker Box Grid (1 to 24)
  quickTableBoxes = computed(() => {
    const existing = this.tablesList();
    const boxes: {
      number: number;
      label: string;
      exists: boolean;
      existingTable?: DiningTableData;
      status?: string;
      capacity?: number;
    }[] = [];

    const maxCount = Math.max(20, existing.length + 8);
    for (let i = 1; i <= maxCount; i++) {
      const numStr = i < 10 ? `0${i}` : `${i}`;
      const label = `Table ${numStr}`;
      
      const found = existing.find(t => {
        const raw = t.tableNumber.trim().toLowerCase();
        const numOnly = raw.replace(/\D/g, '');
        return raw === label.toLowerCase() ||
               raw === `t-${numStr}` ||
               raw === `table ${i}` ||
               numOnly === String(i) ||
               numOnly === numStr;
      });

      boxes.push({
        number: i,
        label: label,
        exists: !!found,
        existingTable: found,
        status: found?.status,
        capacity: found?.capacity || this.tableFormCapacity()
      });
    }
    return boxes;
  });

  quickAddTable(box: { number: number; label: string; exists: boolean; existingTable?: DiningTableData }) {
    if (box.exists && box.existingTable) {
      this.closeAddTableModal();
      this.openTableDetails(box.existingTable);
      return;
    }

    const rId = this.activeRestaurant()?.id || 1;
    const capacity = this.tableFormCapacity() || 4;

    this.tableService.createTable(rId, {
      tableNumber: box.label,
      capacity: capacity,
      status: 'AVAILABLE'
    }).subscribe({
      next: (created) => {
        this.toastService.success('Table Added', `${created.tableNumber} (${capacity} seats) & QR generated!`);
      },
      error: (err) => this.toastService.show(err?.error?.message || 'Failed to create table', 'error')
    });
  }

  setQuickCapacity(cap: number) {
    this.tableFormCapacity.set(cap);
  }

  closeAddTableModal() {
    this.showAddTableModal.set(false);
    this.editingTableId.set(null);
    this.tableFormNumber.set('');
  }

  saveTable() {
    if (!this.tableFormNumber().trim()) {
      this.toastService.show('Please enter a valid table number', 'warning');
      return;
    }
    const rId = this.activeRestaurant()?.id || 1;
    const isEdit = this.editingTableId() !== null;

    if (isEdit) {
      this.tableService.updateTable(rId, this.editingTableId()!, {
        tableNumber: this.tableFormNumber().trim(),
        capacity: this.tableFormCapacity()
      }).subscribe({
        next: (updated) => {
          this.toastService.success('Table Updated', `${updated.tableNumber} updated successfully.`);
          this.closeAddTableModal();
        },
        error: (err) => this.toastService.show(err?.error?.message || 'Failed to update table', 'error')
      });
    } else {
      this.tableService.createTable(rId, {
        tableNumber: this.tableFormNumber().trim(),
        capacity: this.tableFormCapacity(),
        status: 'AVAILABLE'
      }).subscribe({
        next: (created) => {
          this.toastService.success('Table Created', `${created.tableNumber} and QR code generated.`);
          this.closeAddTableModal();
        },
        error: (err) => this.toastService.show(err?.error?.message || 'Failed to create table', 'error')
      });
    }
  }

  deleteTable(tableId: number, event?: Event) {
    if (event) event.stopPropagation();
    const rId = this.activeRestaurant()?.id || 1;
    const target = this.tablesList().find(t => t.id === tableId);
    const tableNum = target?.tableNumber || `Table ${tableId}`;

    this.tableService.deleteTable(rId, tableId).subscribe({
      next: () => {
        if (this.selectedTableDetails()?.id === tableId) {
          this.closeTableDetails();
        }
        this.undoService.showUndo(`${tableNum} deleted`, () => this.tableService.restoreTable(rId, tableId), 7);
      },
      error: (err) => this.toastService.show(err?.error?.message || 'Failed to delete table', 'error')
    });
  }

  openReserveModal(table: DiningTableData, event?: Event) {
    if (event) event.stopPropagation();
    this.selectedTableDetails.set(table);
    this.reserveGuestName.set(table.reservationName || '');
    this.reserveGuestPhone.set(table.reservationPhone || '');
    this.reserveTime.set(table.reservationTime || '7:30 PM');
    this.reserveGuestCount.set(table.reservationGuests || table.capacity || 2);
    this.reserveNotes.set(table.reservationNotes || '');
    this.showReserveTableModal.set(true);
  }

  closeReserveModal() {
    this.showReserveTableModal.set(false);
  }

  submitReservation() {
    if (!this.reserveGuestName().trim() || !this.reserveTime().trim()) {
      this.toastService.show('Please enter guest name and reservation time', 'warning');
      return;
    }
    const table = this.selectedTableDetails();
    if (!table) return;
    const rId = this.activeRestaurant()?.id || 1;

    this.tableService.reserveTable(rId, table.id, {
      guestName: this.reserveGuestName().trim(),
      guestPhone: this.reserveGuestPhone().trim(),
      reservationTime: this.reserveTime().trim(),
      guestCount: this.reserveGuestCount(),
      notes: this.reserveNotes().trim()
    }).subscribe({
      next: (updated) => {
        this.toastService.success('Reservation Confirmed', `${table.tableNumber} reserved for ${updated.reservationName}`);
        this.selectedTableDetails.set(updated);
        this.closeReserveModal();
      },
      error: (err) => this.toastService.show(err?.error?.message || 'Failed to reserve table', 'error')
    });
  }

  closeTableSession(tableId: number, event?: Event) {
    if (event) event.stopPropagation();
    const rId = this.activeRestaurant()?.id || 1;
    const target = this.tablesList().find(t => t.id === tableId);
    const prevStatus = target?.status || 'OCCUPIED';
    const tableNum = target?.tableNumber || `Table ${tableId}`;

    this.tableService.closeTable(rId, tableId).subscribe({
      next: (closed) => {
        if (this.selectedTableDetails()?.id === tableId) {
          this.selectedTableDetails.set(closed);
        }
        this.undoService.showUndo(`${tableNum} closed & marked for cleaning`, () => this.tableService.updateStatus(rId, tableId, prevStatus), 7);
      },
      error: (err) => this.toastService.show(err?.error?.message || 'Failed to close table', 'error')
    });
  }

  markTableAvailable(table: DiningTableData, event?: Event) {
    if (event) event.stopPropagation();
    if (table.status === 'OCCUPIED' || (table.activeOrdersCount && table.activeOrdersCount > 0)) {
      this.tableToMakeAvailable.set(table);
      this.showConfirmAvailableModal.set(true);
    } else {
      this.executeMakeAvailable(table);
    }
  }

  confirmMarkAvailable() {
    const table = this.tableToMakeAvailable();
    if (table) {
      this.executeMakeAvailable(table);
    }
    this.showConfirmAvailableModal.set(false);
    this.tableToMakeAvailable.set(null);
  }

  private executeMakeAvailable(table: DiningTableData) {
    const rId = this.activeRestaurant()?.id || 1;
    this.tableService.updateStatus(rId, table.id, 'AVAILABLE').subscribe({
      next: (updated) => {
        this.toastService.success('Table Available', `${updated.tableNumber} is now ready for new guests.`);
        if (this.selectedTableDetails()?.id === table.id) {
          this.selectedTableDetails.set(updated);
        }
      },
      error: (err) => this.toastService.show(err?.error?.message || 'Failed to update table status', 'error')
    });
  }

  updateTableDirectStatus(tableId: number, status: TableStatus, event?: Event) {
    if (event) event.stopPropagation();
    const rId = this.activeRestaurant()?.id || 1;
    this.tableService.updateStatus(rId, tableId, status).subscribe({
      next: (updated) => {
        this.toastService.success('Status Updated', `${updated.tableNumber} is now ${status}.`);
        if (this.selectedTableDetails()?.id === tableId) {
          this.selectedTableDetails.set(updated);
        }
      },
      error: (err) => this.toastService.show(err?.error?.message || 'Failed to update table status', 'error')
    });
  }

  printTableQr(table: DiningTableData, event?: Event) {
    if (event) event.stopPropagation();
    const printWin = window.open('', '_blank');
    if (printWin) {
      printWin.document.write(`
        <html>
          <head><title>Print QR Code - ${table.tableNumber}</title></head>
          <body style="text-align:center; padding: 40px; font-family: sans-serif;">
            <h2>${this.activeRestaurant()?.name || 'Gourmet Bistro'}</h2>
            <h3>${table.tableNumber}</h3>
            <p style="color:#64748b; font-size:14px;">Capacity: ${table.capacity} Guests • Scan to Order</p>
            <img src="${table.qrImageUrl || this.activeQrUrl()}" style="width:260px; height:260px; margin: 20px 0;" />
            <p style="font-weight:bold; font-size:16px;">Scan to view digital menu & place live order</p>
          </body>
        </html>
      `);
      printWin.document.close();
      printWin.focus();
      setTimeout(() => printWin.print(), 500);
    }
  }

  downloadTableQr(table: DiningTableData, event?: Event) {
    if (event) event.stopPropagation();
    const link = document.createElement('a');
    link.href = table.qrImageUrl || this.activeQrUrl();
    link.download = `${table.tableNumber.replace(/\s+/g, '_')}_QR_Code.png`;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    this.toastService.success('Download Triggered', `High-res QR image downloaded for ${table.tableNumber}.`);
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
