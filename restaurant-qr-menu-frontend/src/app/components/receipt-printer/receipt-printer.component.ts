import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Order } from '../../services/order.service';

export interface ReceiptItem {
  name: string;
  quantity: number;
  price: number;
}

export interface FormattedReceipt {
  restaurantName: string;
  headerTitle: string;
  metaLine: string;
  items: ReceiptItem[];
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  grandTotal: number;
  barcodeNumber: string;
}

@Component({
  selector: 'app-receipt-printer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './receipt-printer.component.html',
  styleUrls: ['./receipt-printer.component.css']
})
export class ReceiptPrinterComponent implements OnInit, OnChanges {
  @Input() order: Order | any = null;
  @Input() autoPrint = true;
  @Input() showCloseButton = false;
  @Output() close = new EventEmitter<void>();
  @Output() tear = new EventEmitter<void>();

  // State signals
  paperState        = signal<'retracted' | 'printing-smooth' | 'printing-classic' | 'printed' | 'tearing'>('retracted');
  currentMode       = signal<'smooth' | 'classic' | 'sample'>('smooth');
  isPrinting        = signal<boolean>(false);
  isPrinted         = signal<boolean>(false);
  soundEnabled      = signal<boolean>(true);
  bladeFlashActive  = signal<boolean>(false);
  sampleIndex       = signal<number>(0);

  statusTitle       = signal<string>('Payment Successful');
  statusSubtext     = signal<string>("You're all set—now let the receipt roll!");
  brandSubtitle     = signal<string>('THERMAL RECEIPT DISPENSER');

  // Sample Presets for Interactive Demo / fallback
  samplePresets: FormattedReceipt[] = [
    {
      restaurantName: "RestQR Gourmet Bistro",
      headerTitle: "TABLE 04 • DINE-IN RECEIPT",
      metaLine: "21ST AUG 2026 | VERIFIED QR ORDER",
      items: [
        { name: "Truffle Mushroom Burger", quantity: 2, price: 32.00 },
        { name: "Cold Brew Iced Coffee", quantity: 1, price: 4.50 },
        { name: "French Toast Brioche", quantity: 1, price: 8.50 }
      ],
      subtotal: 45.00,
      taxRate: 5,
      taxAmount: 2.25,
      grandTotal: 47.25,
      barcodeNumber: "TXN-8849204192"
    },
    {
      restaurantName: "Winged Cafe & Bakery",
      headerTitle: "TABLE 02 • BREAKFAST BILL",
      metaLine: "21ST AUG 2026 | PAID DIGITAL",
      items: [
        { name: "Smoked Salmon Croissant", quantity: 2, price: 25.00 },
        { name: "Matcha Green Tea Pancake", quantity: 1, price: 9.50 },
        { name: "Madagascar Panna Cotta", quantity: 1, price: 11.50 }
      ],
      subtotal: 46.00,
      taxRate: 8,
      taxAmount: 3.68,
      grandTotal: 49.68,
      barcodeNumber: "TXN-1011771660"
    },
    {
      restaurantName: "Aura Fine Dining",
      headerTitle: "TABLE 06 • CHEF'S TASTING",
      metaLine: "21ST AUG 2026 | SETTLED BY CARD",
      items: [
        { name: "Wagyu Beef Ribeye (250g)", quantity: 2, price: 68.00 },
        { name: "Lobster Tagliolini Pasta", quantity: 1, price: 26.00 },
        { name: "Artisanal Chocolate Fondant", quantity: 2, price: 28.00 }
      ],
      subtotal: 122.00,
      taxRate: 5,
      taxAmount: 6.10,
      grandTotal: 128.10,
      barcodeNumber: "TXN-9281485400"
    }
  ];

  activeReceipt = computed<FormattedReceipt>(() => {
    if (this.currentMode() === 'sample') {
      return this.samplePresets[this.sampleIndex() % this.samplePresets.length];
    }

    if (this.order) {
      const items: ReceiptItem[] = (this.order.items || []).map((i: any) => ({
        name: i.name || i.itemName || 'Menu Dish',
        quantity: Number(i.quantity || i.qty || 1),
        price: Number(i.subtotal || ((i.price || 0) * (i.quantity || 1)))
      }));

      const total = Number(this.order.totalAmount || this.order.total || 0);
      const taxRate = 5;
      const subtotal = total > 0 ? (total / (1 + taxRate / 100)) : 0;
      const taxAmount = total - subtotal;

      const dateStr = this.order.createdAt || this.order.placedAt ? new Date(this.order.createdAt || this.order.placedAt).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' }) : 'TODAY';
      const orderNum = this.order.orderNumber || this.order.id || 'ORD-1001';
      const tableNum = this.order.tableNumber ? `TABLE ${this.order.tableNumber}` : 'DINE-IN';

      return {
        restaurantName: this.order.restaurant?.name || 'RestQR Gourmet Bistro',
        headerTitle: `${tableNum} • ORDER #${orderNum}`,
        metaLine: `${dateStr.toUpperCase()} | VERIFIED DIGITAL RECEIPT`,
        items: items.length > 0 ? items : [{ name: "Standard Gourmet Service", quantity: 1, price: total }],
        subtotal: subtotal,
        taxRate: taxRate,
        taxAmount: taxAmount,
        grandTotal: total,
        barcodeNumber: `TXN-${String(orderNum).replace(/\D/g, '').padEnd(10, '8')}`
      };
    }

    return this.samplePresets[0];
  });

  // Audio synthesizer context
  private audioCtx: AudioContext | null = null;

  ngOnInit() {
    if (this.autoPrint) {
      setTimeout(() => {
        this.triggerPrint();
      }, 350);
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['order'] && !changes['order'].firstChange) {
      if (this.isPrinted()) {
        this.triggerTear();
        setTimeout(() => {
          this.triggerPrint();
        }, 600);
      }
    }
  }

  private initAudio() {
    if (!this.audioCtx && typeof window !== 'undefined') {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioContextClass) {
        this.audioCtx = new AudioContextClass();
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
  }

  private playPrinterSound(mode: 'classic' | 'smooth', motionDurationMs: number) {
    if (!this.soundEnabled()) return;
    this.initAudio();
    if (!this.audioCtx) return;

    try {
      const now = this.audioCtx.currentTime;
      const duration = motionDurationMs / 1000;

      const bufferSize = Math.floor(this.audioCtx.sampleRate * duration);
      const buffer = this.audioCtx.createBuffer(1, bufferSize, this.audioCtx.sampleRate);
      const output = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        output[i] = Math.random() * 2 - 1;
      }

      const whiteNoise = this.audioCtx.createBufferSource();
      whiteNoise.buffer = buffer;

      const filter = this.audioCtx.createBiquadFilter();
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(mode === 'classic' ? 850 : 600, now);
      filter.Q.setValueAtTime(3.5, now);

      const gainNode = this.audioCtx.createGain();
      const peakGain = mode === 'classic' ? 0.07 : 0.04;
      gainNode.gain.setValueAtTime(0.001, now);
      gainNode.gain.linearRampToValueAtTime(peakGain, now + 0.08);
      gainNode.gain.setValueAtTime(peakGain, now + duration - 0.12);
      gainNode.gain.exponentialRampToValueAtTime(0.0001, now + duration);

      whiteNoise.connect(filter);
      filter.connect(gainNode);
      gainNode.connect(this.audioCtx.destination);

      whiteNoise.start(now);
      whiteNoise.stop(now + duration);

      if (mode === 'classic') {
        const stepCount = 14;
        const interval = (duration - 0.1) / stepCount;
        for (let i = 0; i < stepCount; i++) {
          const stepTime = now + (i * interval);
          const osc = this.audioCtx.createOscillator();
          const stepGain = this.audioCtx.createGain();

          osc.type = 'square';
          osc.frequency.setValueAtTime(210 + Math.random() * 60, stepTime);
          stepGain.gain.setValueAtTime(0.05, stepTime);
          stepGain.gain.exponentialRampToValueAtTime(0.001, stepTime + 0.02);

          osc.connect(stepGain);
          stepGain.connect(this.audioCtx.destination);
          osc.start(stepTime);
          osc.stop(stepTime + 0.02);
        }
      }
    } catch (e) {
      console.warn('Audio play error:', e);
    }
  }

  private playTearSound() {
    if (!this.soundEnabled()) return;
    this.initAudio();
    if (!this.audioCtx) return;

    try {
      const now = this.audioCtx.currentTime;
      const duration = 0.35;
      const bufferSize = Math.floor(this.audioCtx.sampleRate * duration);
      const buffer = this.audioCtx.createBuffer(1, bufferSize, this.audioCtx.sampleRate);
      const output = buffer.getChannelData(0);

      for (let i = 0; i < bufferSize; i++) {
        output[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.audioCtx.sampleRate * 0.06));
      }

      const noise = this.audioCtx.createBufferSource();
      noise.buffer = buffer;

      const filter = this.audioCtx.createBiquadFilter();
      filter.type = 'highpass';
      filter.frequency.setValueAtTime(1400, now);

      const gain = this.audioCtx.createGain();
      gain.gain.setValueAtTime(0.22, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + duration);

      noise.connect(filter);
      filter.connect(gain);
      gain.connect(this.audioCtx.destination);

      noise.start(now);
      noise.stop(now + duration);
    } catch (e) {
      console.warn('Tear audio error:', e);
    }
  }

  setMode(mode: 'smooth' | 'classic' | 'sample') {
    if (this.isPrinting()) return;
    this.currentMode.set(mode);

    if (mode === 'sample') {
      this.sampleIndex.update(idx => (idx + 1) % this.samplePresets.length);
      this.statusTitle.set("Sample Preset Loaded");
      this.statusSubtext.set(`Loaded: "${this.activeReceipt().restaurantName}"`);
      if (!this.isPrinted()) {
        this.triggerPrint();
      }
    } else if (mode === 'classic') {
      this.statusTitle.set("Classic Stepper Mode");
      this.statusSubtext.set("Realistic mechanical stepper motor print animation");
    } else {
      this.statusTitle.set("Smooth Easing Mode");
      this.statusSubtext.set("Fluid continuous motion print animation");
    }
  }

  toggleSound() {
    this.soundEnabled.update(s => !s);
    if (this.soundEnabled()) {
      this.initAudio();
    }
  }

  onPrintClick() {
    if (this.isPrinted()) {
      this.paperState.set('retracted');
      this.isPrinted.set(false);
      setTimeout(() => {
        this.triggerPrint();
      }, 250);
    } else {
      this.triggerPrint();
    }
  }

  triggerPrint() {
    if (this.isPrinting()) return;

    this.isPrinting.set(true);
    this.bladeFlashActive.set(false);
    this.statusTitle.set("Printing Receipt...");
    this.statusSubtext.set("Dispensing verified digital guest receipt");

    const mode = this.currentMode() === 'classic' ? 'classic' : 'smooth';
    const animDuration = 2500;

    this.playPrinterSound(mode, animDuration);
    this.paperState.set(mode === 'classic' ? 'printing-classic' : 'printing-smooth');

    setTimeout(() => {
      this.paperState.set('printed');
      this.isPrinting.set(false);
      this.isPrinted.set(true);

      this.statusTitle.set("Payment & Receipt Verified");
      this.statusSubtext.set("Your thermal receipt is ready! Tear or save a copy.");
    }, animDuration);
  }

  triggerTear() {
    if (!this.isPrinted() || this.isPrinting()) return;

    this.playTearSound();
    this.bladeFlashActive.set(true);
    this.paperState.set('tearing');
    this.statusTitle.set("Receipt Torn Off");
    this.statusSubtext.set("Paper safely cut and detached from thermal slot");
    this.tear.emit();

    setTimeout(() => {
      this.paperState.set('retracted');
      this.bladeFlashActive.set(false);
      this.isPrinted.set(false);
      this.statusTitle.set("Ready to Print");
      this.statusSubtext.set("Click print to dispense a fresh receipt copy");
    }, 550);
  }

  printPhysical() {
    if (typeof window !== 'undefined') {
      window.print();
    }
  }

  closeModal() {
    this.close.emit();
  }
}
