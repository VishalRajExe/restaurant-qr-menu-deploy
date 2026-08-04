export interface ScanActivity {
  id: string;
  tableNumber: string;
  time: string;
  action: string;
  device: string;
  status: 'Completed' | 'Requesting Assistance' | 'Viewing';
}

export const OWNER_KPI_SUMMARY = {
  totalMenuItems: 148,
  totalCategories: 18,
  todayScans: 462,
  popularItem: 'Truffle Butter King Scallops',
  activeOffers: 4,
  totalScansThisMonth: 12450
};

export const RECENT_SCAN_ACTIVITIES: ScanActivity[] = [
  { id: 'sc1', tableNumber: 'Table 4', time: '10:42 AM', action: 'Scanned QR Code', device: 'iPhone 15 Pro, Safari', status: 'Viewing' },
  { id: 'sc2', tableNumber: 'Table 12', time: '10:39 AM', action: 'Tapped "Assistance Required"', device: 'Samsung Galaxy S24, Chrome', status: 'Requesting Assistance' },
  { id: 'sc3', tableNumber: 'Table 9', time: '10:15 AM', action: 'Viewed Champagne Cellar', device: 'Google Pixel 8, Firefox', status: 'Completed' },
  { id: 'sc4', tableNumber: 'Table 2', time: '10:02 AM', action: 'Scanned QR Code', device: 'Xiaomi 14, Opera', status: 'Completed' },
  { id: 'sc5', tableNumber: 'Table 15', time: '09:54 AM', action: 'Scanned QR Code', device: 'iPad Air, Safari', status: 'Completed' }
];

export const MONTHLY_ANALYTICS_CHART = [
  { label: 'Mon', scans: 240, views: 350 },
  { label: 'Tue', scans: 310, views: 420 },
  { label: 'Wed', scans: 290, views: 400 },
  { label: 'Thu', scans: 380, views: 510 },
  { label: 'Fri', scans: 520, views: 680 },
  { label: 'Sat', scans: 610, views: 820 },
  { label: 'Sun', scans: 580, views: 790 }
];

export const POPULAR_DISHES_ANALYTICS = [
  { name: 'Truffle Butter King Scallops', scans: 145, percentage: 38 },
  { name: 'Bordeaux Tenderloin Mignon', scans: 110, percentage: 28 },
  { name: 'Champagne-Braised Salmon Fillet', scans: 85, percentage: 22 },
  { name: 'Grand Marnier Soufflé', scans: 60, percentage: 12 }
];
