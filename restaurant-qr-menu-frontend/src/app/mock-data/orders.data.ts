// ── Order types ─────────────────────────────────────────────────────────────
export interface OrderItem {
  name: string;
  qty: number;
  note?: string;
}

export interface Order {
  id: string;
  tableNumber: number;
  items: OrderItem[];
  status: 'pending' | 'preparing' | 'done';
  placedAt: Date;
  specialRequest?: string;
}

// ── Support Ticket ───────────────────────────────────────────────────────────
export interface SupportTicket {
  id: string;
  restaurantId: string;
  restaurantName: string;
  ownerName: string;
  ownerEmail: string;
  subject: string;
  message: string;
  status: 'open' | 'resolved';
  createdAt: Date;
  priority: 'low' | 'medium' | 'high';
}

// ── Mock Orders ──────────────────────────────────────────────────────────────
export const MOCK_ORDERS: Order[] = [
  {
    id: 'ord-001',
    tableNumber: 4,
    items: [
      { name: 'Truffle Fries', qty: 2 },
      { name: 'Grilled Sea Bass', qty: 1, note: 'No lemon please' },
      { name: 'Sparkling Water', qty: 2 },
    ],
    status: 'pending',
    placedAt: new Date(Date.now() - 3 * 60 * 1000),
    specialRequest: 'Allergy: No nuts'
  },
  {
    id: 'ord-002',
    tableNumber: 7,
    items: [
      { name: 'Caesar Salad', qty: 1 },
      { name: 'Beef Tenderloin', qty: 2, note: 'Medium rare' },
    ],
    status: 'preparing',
    placedAt: new Date(Date.now() - 12 * 60 * 1000),
  },
  {
    id: 'ord-003',
    tableNumber: 2,
    items: [
      { name: 'Mushroom Risotto', qty: 1 },
      { name: 'Tiramisu', qty: 2 },
      { name: 'Espresso', qty: 2 },
    ],
    status: 'preparing',
    placedAt: new Date(Date.now() - 18 * 60 * 1000),
  },
  {
    id: 'ord-004',
    tableNumber: 11,
    items: [
      { name: 'Soup of the Day', qty: 3 },
      { name: 'Garlic Bread', qty: 2 },
    ],
    status: 'done',
    placedAt: new Date(Date.now() - 35 * 60 * 1000),
  },
];

// ── Mock Restaurants (Super Admin) ───────────────────────────────────────────
export const MOCK_RESTAURANTS_ADMIN = [
  {
    id: 'r1',
    name: 'Le Jardin de Provence',
    owner: 'Antoine Laurent',
    ownerEmail: 'antoine@jardinprovence.fr',
    ownerPhone: '+33 3 26 12 34 56',
    plan: 'Pro',
    totalScans: 1284,
    totalItems: 38,
    status: 'active',
    joinedDate: '2024-11-03',
    location: 'Paris, France',
    address: '42 Avenue de Champagne, Reims, France',
    cuisine: 'Modern French Fine Dining',
    gstin: '27AAPFU0939F1ZV',
    photo: 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=600&auto=format&fit=crop&q=80',
    description: 'Award-winning fine dining in the heart of Reims, specializing in modern French cuisine with seasonal ingredients.'
  },
  {
    id: 'r2',
    name: 'SpicyHunt Gastrobar',
    owner: 'Priya Malhotra',
    ownerEmail: 'priya@spicyhunt.in',
    ownerPhone: '+91 98765 43210',
    plan: 'Starter',
    totalScans: 762,
    totalItems: 24,
    status: 'active',
    joinedDate: '2025-01-15',
    location: 'Mumbai, India',
    address: 'B-12, Bandra Kurla Complex, Mumbai, Maharashtra',
    cuisine: 'Asian Fusion & Craft Mixology',
    gstin: '27BBBFF1234C1ZP',
    photo: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600&auto=format&fit=crop&q=80',
    description: 'Vibrant gastrobar serving Asian fusion plates with craft cocktails and live music every weekend.'
  },
  {
    id: 'r3',
    name: 'The Olive Branch',
    owner: 'Marco Rossi',
    ownerEmail: 'marco@olivebranchrome.it',
    ownerPhone: '+39 06 1234 5678',
    plan: 'Pro',
    totalScans: 2103,
    totalItems: 56,
    status: 'active',
    joinedDate: '2024-09-20',
    location: 'Rome, Italy',
    address: 'Via della Conciliazione 14, 00193 Roma RM, Italy',
    cuisine: 'Authentic Italian Trattoria',
    gstin: '06CCCGG5678D2ZQ',
    photo: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&auto=format&fit=crop&q=80',
    description: 'Authentic Roman trattoria in the shadow of St. Peter\'s Basilica, serving homemade pasta and wood-fired pizza.'
  },
  {
    id: 'r4',
    name: 'Sakura Ramen House',
    owner: 'Yuki Tanaka',
    ownerEmail: 'yuki@sakuraramen.jp',
    ownerPhone: '+81 3-1234-5678',
    plan: 'Starter',
    totalScans: 421,
    totalItems: 18,
    status: 'inactive',
    joinedDate: '2025-03-08',
    location: 'Tokyo, Japan',
    address: '2-1 Dogenzaka, Shibuya City, Tokyo 150-0043, Japan',
    cuisine: 'Traditional Japanese Ramen',
    gstin: '13DDDII9012E3ZR',
    photo: 'https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=600&auto=format&fit=crop&q=80',
    description: 'Traditional Shibuya ramen house offering rich tonkotsu broth perfected over 30 years of family craft.'
  },
  {
    id: 'r5',
    name: 'Burger Republic',
    owner: 'James O\'Brien',
    ownerEmail: 'james@burgerrepublic.ie',
    ownerPhone: '+353 1 234 5678',
    plan: 'Pro',
    totalScans: 3560,
    totalItems: 42,
    status: 'active',
    joinedDate: '2024-07-12',
    location: 'Dublin, Ireland',
    address: '14 Grafton Street, Dublin 2, D02 HV63, Ireland',
    cuisine: 'Gourmet Burgers & Craft Beer',
    gstin: '29EEEKKK3456F4ZS',
    photo: 'https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=600&auto=format&fit=crop&q=80',
    description: 'Dublin\'s most popular gourmet burger spot on Grafton Street, known for grass-fed beef and 40+ craft beers on tap.'
  },
];

// ── Mock Support Tickets ─────────────────────────────────────────────────────
export const MOCK_SUPPORT_TICKETS: SupportTicket[] = [
  {
    id: 'tkt-001',
    restaurantId: 'r2',
    restaurantName: 'SpicyHunt Gastrobar',
    ownerName: 'Priya Malhotra',
    ownerEmail: 'priya@spicyhunt.in',
    subject: 'QR Code not scanning properly',
    message: 'Hi, our printed QR codes are not scanning correctly on some Android devices. Customers are getting a 404 error when they scan. We have already tried reprinting but the issue persists. Please help urgently as dinner service starts in 2 hours.',
    status: 'open',
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000),
    priority: 'high'
  },
  {
    id: 'tkt-002',
    restaurantId: 'r4',
    restaurantName: 'Sakura Ramen House',
    ownerName: 'Yuki Tanaka',
    ownerEmail: 'yuki@sakuraramen.jp',
    subject: 'Request to upgrade plan to Pro',
    message: 'We would like to upgrade our subscription from Starter to Pro plan. Our menu has grown to 35+ items and we need the analytics features. Please advise on the billing process and when the upgrade takes effect.',
    status: 'open',
    createdAt: new Date(Date.now() - 6 * 60 * 60 * 1000),
    priority: 'medium'
  },
  {
    id: 'tkt-003',
    restaurantId: 'r1',
    restaurantName: 'Le Jardin de Provence',
    ownerName: 'Antoine Laurent',
    ownerEmail: 'antoine@jardinprovence.fr',
    subject: 'Menu images not loading after upload',
    message: 'Several dish images uploaded yesterday are not showing in the guest menu view. They appear fine in the owner dashboard but guests see a broken image placeholder. This is affecting our customer experience significantly.',
    status: 'resolved',
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
    priority: 'high'
  },
  {
    id: 'tkt-004',
    restaurantId: 'r3',
    restaurantName: 'The Olive Branch',
    ownerName: 'Marco Rossi',
    ownerEmail: 'marco@olivebranchrome.it',
    subject: 'Add seasonal menu feature request',
    message: 'We change our menu seasonally and would love a feature to schedule menu items visibility by date range. Currently we have to manually toggle availability each time. A scheduled visibility feature would save us a lot of time.',
    status: 'open',
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000),
    priority: 'low'
  },
];
