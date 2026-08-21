import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ReceiptPrinterComponent } from '../../components/receipt-printer/receipt-printer.component';
import { OrderService, Order } from '../../services/order.service';

@Component({
  selector: 'app-receipt-view',
  standalone: true,
  imports: [CommonModule, RouterModule, ReceiptPrinterComponent],
  template: `
    <div style="min-height:100vh;background:#ffffff;display:flex;flex-direction:column;align-items:center;padding:24px 16px">
      
      <!-- Top Navigation -->
      <header style="width:100%;max-width:540px;display:flex;justify-content:space-between;align-items:center;margin-bottom:18px">
        <button type="button"
                style="display:inline-flex;align-items:center;gap:6px;background:#f8fafc;border:1px solid #e2e8f0;padding:6px 14px;border-radius:10px;font-size:13px;font-weight:700;color:#334155;cursor:pointer"
                (click)="goBack()">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          <span>Back to Menu</span>
        </button>

        <div style="display:flex;align-items:center;gap:8px">
          <span style="font-family:monospace;font-size:12px;font-weight:800;color:#ea580c;background:#fff7ed;padding:3px 8px;border-radius:6px;border:1px solid #fed7aa">
            {{ activeOrder() ? (activeOrder().orderNumber || activeOrder().id) : 'LIVE POS RECEIPT' }}
          </span>
        </div>
      </header>

      <!-- Thermal Receipt Printer Component -->
      <div style="width:100%;max-width:540px">
        <app-receipt-printer [order]="activeOrder()" [autoPrint]="true"></app-receipt-printer>
      </div>

    </div>
  `
})
export class ReceiptViewComponent implements OnInit {
  activeOrder = signal<Order | any>(null);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      const orderNumber = params['orderNumber'];
      const orders = this.orderService.getOrders();
      if (orderNumber) {
        const found = orders.find(o => o.orderNumber === orderNumber || String(o.id) === orderNumber);
        if (found) {
          this.activeOrder.set(found);
        } else {
          // Mock order fallback
          this.activeOrder.set({
            orderNumber: orderNumber,
            tableNumber: '04',
            restaurant: { name: 'RestQR Gourmet Bistro' },
            totalAmount: 47.25,
            items: [
              { name: 'Truffle Mushroom Burger', quantity: 2, subtotal: 32.00 },
              { name: 'Cold Brew Iced Coffee', quantity: 1, subtotal: 4.50 },
              { name: 'French Toast Brioche', quantity: 1, subtotal: 8.50 }
            ]
          });
        }
      } else {
        if (orders && orders.length > 0) {
          this.activeOrder.set(orders[0]);
        }
      }
    });
  }

  goBack() {
    this.router.navigate(['/menu']);
  }
}
