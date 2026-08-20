import { Injectable } from '@angular/core';
import { Order } from './order.service';

export interface RestaurantPrintInfo {
  name?: string;
  address?: string;
  phone?: string;
  email?: string;
  gstin?: string;
  tagline?: string;
  currency?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PrintService {

  private defaultRestaurantInfo: RestaurantPrintInfo = {
    name: 'RestQR Gourmet Bistro',
    address: '123 Gourmet Blvd, New York, NY 10001',
    phone: '+1 (555) 345-6789',
    email: 'contact@restqr.com',
    gstin: '27AAPFU0939F1ZV',
    tagline: 'Fresh Flavors • Crafted Daily',
    currency: '$'
  };

  /**
   * Generates and triggers printable Kitchen Order Ticket (KOT)
   */
  printKOT(order: Order, restaurantInfo?: RestaurantPrintInfo): void {
    const info = { ...this.defaultRestaurantInfo, ...restaurantInfo };
    const orderNum = order.orderNumber || order.id || 'ORD-' + Math.floor(1000 + Math.random() * 9000);
    const kotNum = 'KOT-' + String(orderNum).replace(/\D/g, '').slice(-4);
    const table = order.tableNumber || order.table || '01';
    const placedDate = order.placedAt ? new Date(order.placedAt) : new Date();
    const formattedDate = placedDate.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
    const formattedTime = placedDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    const itemsHtml = (order.items || []).map((item, idx) => `
      <div style="display:flex;justify-content:space-between;align-items:flex-start;padding:6px 0;border-bottom:1px dashed #ccc;font-size:15px">
        <div style="flex:1;padding-right:8px">
          <strong style="font-size:16px;letter-spacing:0.02em">${item.qty} × ${item.name}</strong>
          ${item.note || item.notes ? `<div style="font-size:12px;color:#c2410c;font-style:italic;margin-top:2px">⚠️ Note: ${item.note || item.notes}</div>` : ''}
        </div>
      </div>
    `).join('');

    const specialRequestHtml = order.specialRequest ? `
      <div style="margin-top:12px;padding:8px 10px;border:1.5px dashed #c2410c;border-radius:6px;background:#fff1ec;font-size:13px">
        <strong style="color:#c2410c">SPECIAL CHEF INSTRUCTION:</strong>
        <div style="margin-top:2px;font-weight:600">${order.specialRequest}</div>
      </div>
    ` : '';

    const kotContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>KOT - Table ${table} - ${orderNum}</title>
        <meta charset="utf-8">
        <style>
          @page { size: 80mm auto; margin: 4mm; }
          body {
            font-family: 'Courier New', Courier, monospace, system-ui;
            margin: 0;
            padding: 8px;
            color: #000;
            background: #fff;
            max-width: 320px;
            margin: 0 auto;
          }
          .text-center { text-align: center; }
          .bold { font-weight: bold; }
          .divider { border-top: 2px dashed #000; margin: 8px 0; }
          .double-divider { border-top: 2px solid #000; border-bottom: 2px solid #000; height: 3px; margin: 8px 0; }
          .header-title { font-size: 20px; font-weight: 900; letter-spacing: 1px; }
          .table-badge {
            font-size: 24px;
            font-weight: 900;
            padding: 4px 8px;
            border: 2px solid #000;
            display: inline-block;
            margin: 6px 0;
          }
          .meta-row { display: flex; justify-content: space-between; font-size: 13px; margin: 3px 0; }
          @media print {
            body { max-width: 100%; padding: 0; }
            .no-print { display: none !important; }
          }
        </style>
      </head>
      <body>
        <div class="text-center">
          <div class="header-title">KITCHEN ORDER TICKET</div>
          <div style="font-size:13px;font-weight:bold">${info.name}</div>
          <div class="table-badge">TABLE: ${table}</div>
        </div>

        <div class="divider"></div>
        <div class="meta-row"><span>KOT No: <strong>${kotNum}</strong></span><span>Order: <strong>${orderNum}</strong></span></div>
        <div class="meta-row"><span>Date: ${formattedDate}</span><span>Time: <strong>${formattedTime}</strong></span></div>
        <div class="meta-row"><span>Diner: ${order.customerName || 'Guest'}</span><span>Status: ${order.status || 'NEW'}</span></div>
        <div class="divider"></div>

        <div style="font-size:14px;font-weight:bold;margin-bottom:6px">ORDERED ITEMS:</div>
        <div>${itemsHtml || '<div style="padding:10px 0;text-align:center">No items listed</div>'}</div>

        ${specialRequestHtml}

        <div class="double-divider"></div>
        <div class="text-center" style="font-size:12px;font-weight:bold;margin-top:6px">
          *** KITCHEN DISPATCH COPY ***
        </div>
      </body>
      </html>
    `;

    this.executePrintWindow(kotContent, `KOT_${table}_${orderNum}`);
  }

  /**
   * Generates and triggers printable Tax Invoice & Dining Bill
   */
  printInvoice(order: Order, restaurantInfo?: RestaurantPrintInfo): void {
    const info = { ...this.defaultRestaurantInfo, ...restaurantInfo };
    const orderNum = order.orderNumber || order.id || 'ORD-' + Math.floor(1000 + Math.random() * 9000);
    const invNum = 'INV-' + String(orderNum).replace(/\D/g, '').padStart(6, '0');
    const table = order.tableNumber || order.table || '01';
    const curr = info.currency || '$';

    const placedDate = order.placedAt ? new Date(order.placedAt) : new Date();
    const formattedDate = placedDate.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
    const formattedTime = placedDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    let subtotal = 0;
    const items = order.items || [];
    const itemsRows = items.map((item, i) => {
      const price = item.price || (item.subtotal && item.qty ? item.subtotal / item.qty : 12.00);
      const lineTotal = item.subtotal || (price * item.qty);
      subtotal += lineTotal;
      return `
        <tr>
          <td style="padding:6px 4px;border-bottom:1px solid #e2e8f0;font-size:13px">${i + 1}</td>
          <td style="padding:6px 4px;border-bottom:1px solid #e2e8f0;font-size:13px;font-weight:600">
            ${item.name}
            ${item.note ? `<div style="font-size:11px;color:#64748b">${item.note}</div>` : ''}
          </td>
          <td style="padding:6px 4px;border-bottom:1px solid #e2e8f0;font-size:13px;text-align:center">${item.qty}</td>
          <td style="padding:6px 4px;border-bottom:1px solid #e2e8f0;font-size:13px;text-align:right">${curr}${price.toFixed(2)}</td>
          <td style="padding:6px 4px;border-bottom:1px solid #e2e8f0;font-size:13px;text-align:right;font-weight:700">${curr}${lineTotal.toFixed(2)}</td>
        </tr>
      `;
    }).join('');

    if (subtotal === 0 && (order.totalAmount || order.totalPrice)) {
      subtotal = (order.totalAmount || order.totalPrice || 0) / 1.10;
    }

    const tax = +(subtotal * 0.05).toFixed(2);
    const serviceCharge = +(subtotal * 0.05).toFixed(2);
    const grandTotal = +(subtotal + tax + serviceCharge).toFixed(2);

    const invoiceContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Tax Invoice - ${invNum} - ${info.name}</title>
        <meta charset="utf-8">
        <style>
          @page { size: portrait; margin: 10mm; }
          body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            margin: 0;
            padding: 24px;
            color: #0f172a;
            background: #fff;
            max-width: 600px;
            margin: 0 auto;
            line-height: 1.4;
          }
          .invoice-card {
            border: 1.5px solid #cbd5e1;
            border-radius: 12px;
            padding: 24px;
            background: #ffffff;
          }
          .header-flex {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            border-bottom: 2px solid #ff6b35;
            padding-bottom: 16px;
            margin-bottom: 16px;
          }
          .brand-title {
            font-size: 22px;
            font-weight: 900;
            color: #0f172a;
            letter-spacing: -0.02em;
          }
          .brand-subtitle {
            font-size: 12px;
            color: #64748b;
            margin-top: 3px;
          }
          .invoice-tag {
            text-align: right;
          }
          .invoice-title {
            font-size: 18px;
            font-weight: 800;
            color: #ff6b35;
            text-transform: uppercase;
            letter-spacing: 0.05em;
          }
          .meta-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
            background: #f8fafc;
            padding: 12px 16px;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
            margin-bottom: 18px;
            font-size: 12.5px;
          }
          .items-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 18px;
          }
          .items-table th {
            background: #f1f5f9;
            color: #475569;
            font-size: 11.5px;
            font-weight: 700;
            text-transform: uppercase;
            padding: 8px 4px;
            border-bottom: 2px solid #cbd5e1;
            text-align: left;
          }
          .totals-wrap {
            display: flex;
            justify-content: flex-end;
            margin-top: 12px;
          }
          .totals-table {
            width: 260px;
            border-collapse: collapse;
            font-size: 13px;
          }
          .totals-table td {
            padding: 4px 6px;
          }
          .grand-total-row {
            border-top: 2px solid #0f172a;
            border-bottom: 2px solid #0f172a;
            font-size: 16px;
            font-weight: 900;
            color: #0f172a;
          }
          .footer-note {
            text-align: center;
            margin-top: 24px;
            padding-top: 16px;
            border-top: 1px dashed #cbd5e1;
            font-size: 12px;
            color: #64748b;
          }
          .paid-stamp {
            display: inline-block;
            padding: 4px 12px;
            border: 2px solid #166534;
            color: #166534;
            background: #f0fdf4;
            font-weight: 900;
            font-size: 13px;
            border-radius: 6px;
            text-transform: uppercase;
            letter-spacing: 0.08em;
          }
          @media print {
            body { padding: 0; max-width: 100%; }
            .invoice-card { border: none; padding: 0; }
            .no-print { display: none !important; }
          }
        </style>
      </head>
      <body>
        <div class="invoice-card">
          <div class="header-flex">
            <div>
              <div class="brand-title">${info.name}</div>
              <div class="brand-subtitle">${info.address}</div>
              <div class="brand-subtitle">Phone: ${info.phone} • Email: ${info.email}</div>
              ${info.gstin ? `<div class="brand-subtitle" style="font-weight:600">GST/Tax ID: ${info.gstin}</div>` : ''}
            </div>
            <div class="invoice-tag">
              <div class="invoice-title">TAX INVOICE</div>
              <div style="font-weight:800;font-family:monospace;font-size:14px;color:#0f172a">${invNum}</div>
              <div style="margin-top:6px"><span class="paid-stamp">● COMPLETED</span></div>
            </div>
          </div>

          <div class="meta-grid">
            <div>
              <div><strong>Billed To:</strong> ${order.customerName || 'Dining Guest'}</div>
              <div><strong>Contact:</strong> ${order.customerMobile || 'N/A'}</div>
              <div><strong>Table No:</strong> <span style="font-size:14px;font-weight:900;color:#ff6b35">Table ${table}</span></div>
            </div>
            <div style="text-align:right">
              <div><strong>Invoice Date:</strong> ${formattedDate}</div>
              <div><strong>Order Time:</strong> ${formattedTime}</div>
              <div><strong>Order ID:</strong> ${orderNum}</div>
            </div>
          </div>

          <table class="items-table">
            <thead>
              <tr>
                <th style="width:28px">#</th>
                <th>Item & Description</th>
                <th style="width:40px;text-align:center">Qty</th>
                <th style="width:70px;text-align:right">Rate</th>
                <th style="width:80px;text-align:right">Amount</th>
              </tr>
            </thead>
            <tbody>
              ${itemsRows || '<tr><td colspan="5" style="text-align:center;padding:12px">No items</td></tr>'}
            </tbody>
          </table>

          <div class="totals-wrap">
            <table class="totals-table">
              <tr>
                <td>Subtotal:</td>
                <td style="text-align:right;font-weight:600">${curr}${subtotal.toFixed(2)}</td>
              </tr>
              <tr>
                <td>GST / Tax (5%):</td>
                <td style="text-align:right;color:#64748b">${curr}${tax.toFixed(2)}</td>
              </tr>
              <tr>
                <td>Service Charge (5%):</td>
                <td style="text-align:right;color:#64748b">${curr}${serviceCharge.toFixed(2)}</td>
              </tr>
              <tr class="grand-total-row">
                <td style="padding:8px 6px">Grand Total:</td>
                <td style="padding:8px 6px;text-align:right">${curr}${grandTotal.toFixed(2)}</td>
              </tr>
            </table>
          </div>

          <div class="footer-note">
            <div style="font-weight:700;color:#0f172a;margin-bottom:4px">Thank you for dining at ${info.name}!</div>
            <div>This is a computer-generated tax invoice. Keep this receipt for your records.</div>
          </div>
        </div>
      </body>
      </html>
    `;

    this.executePrintWindow(invoiceContent, `Invoice_${invNum}`);
  }

  private executePrintWindow(htmlContent: string, title: string): void {
    const printWindow = window.open('', '_blank', 'width=800,height=900,menubar=no,toolbar=no,location=no,status=no');
    if (printWindow) {
      printWindow.document.open();
      printWindow.document.write(htmlContent);
      printWindow.document.close();
      printWindow.focus();
      setTimeout(() => {
        try {
          printWindow.print();
        } catch (e) {
          console.error('Print execution failed', e);
        }
      }, 400);
    }
  }
}
