import { Restaurant } from '../models/restaurant.model';
import { Category } from '../models/category.model';
import { MenuItem } from '../models/menu-item.model';
import { Offer } from '../models/offer.model';

export const MOCK_RESTAURANTS: Restaurant[] = [
  {
    id: 'r1',
    name: 'Le Jardin de Provence',
    logo: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=200&auto=format&fit=crop&q=80',
    photo: 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400&auto=format&fit=crop&q=80',
    coverImage: 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=1600&auto=format&fit=crop&q=80',
    address: '42 Avenue de Champagne, Reims, France',
    phone: '+33 3 26 12 34 56',
    email: 'events@jardinprovence.fr',
    openingHours: '12:00 PM - 11:00 PM',
    cuisineType: 'Modern French Fine Dining',
    rating: 4.9,
    socialLinks: {
      instagram: '@jardinprovence_reims',
      facebook: 'LeJardinDeProvence'
    }
  },
  {
    id: 'r2',
    name: 'SpicyHunt Gastrobar',
    logo: 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=200&auto=format&fit=crop&q=80',
    photo: 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400&auto=format&fit=crop&q=80',
    coverImage: 'https://images.unsplash.com/photo-1559339352-11d035aa65de?w=1600&auto=format&fit=crop&q=80',
    address: '77 Soho Square, London, United Kingdom',
    phone: '+44 20 7946 0852',
    email: 'hello@spicyhuntsoho.com',
    openingHours: '11:30 AM - Midnight',
    cuisineType: 'Asian Fusion & Craft Mixology',
    rating: 4.8,
    socialLinks: {
      instagram: '@spicyhunt.london',
      facebook: 'SpicyHuntGastrobar',
      twitter: '@spicy_hunt'
    }
  },
  {
    id: 'r3',
    name: 'Mesa Verde',
    logo: 'https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=200&auto=format&fit=crop&q=80',
    coverImage: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=1600&auto=format&fit=crop&q=80',
    address: '888 Paseo del Sol, Santa Fe, USA',
    phone: '+1 505 555 1234',
    email: 'contacto@mesavedesf.com',
    openingHours: '11:00 AM - 10:00 PM',
    cuisineType: 'Artisanal Mexican & Mezcalerí­a',
    rating: 4.7,
    socialLinks: {
      instagram: '@mesaverde_santafe'
    }
  }
];

export const MOCK_CATEGORIES: Category[] = [
  // Categories for Le Jardin de Provence (r1)
  { id: 'c1', restaurantId: 'r1', name: 'Artisanal Appetizers', icon: 'Soup', sortOrder: 1 },
  { id: 'c2', restaurantId: 'r1', name: 'Signature Main Course', icon: 'Utensils', sortOrder: 2 },
  { id: 'c3', restaurantId: 'r1', name: 'Decadent Desserts', icon: 'Dessert', sortOrder: 3 },
  { id: 'c4', restaurantId: 'r1', name: 'Vintage Cellar Collection', icon: 'Wine', sortOrder: 4 },

  // Categories for SpicyHunt Gastrobar (r2)
  { id: 'c5', restaurantId: 'r2', name: 'Soups & Starters', icon: 'ChefHat', sortOrder: 1 },
  { id: 'c6', restaurantId: 'r2', name: 'Signature Mains', icon: 'Flame', sortOrder: 2 },
  { id: 'c7', restaurantId: 'r2', name: 'Gastrobar Desserts', icon: 'IceCream', sortOrder: 3 },
  { id: 'c8', restaurantId: 'r2', name: 'Tropical Mixes', icon: 'GlassWater', sortOrder: 4 }
];

export const MOCK_MENU_ITEMS: MenuItem[] = [
  // Le Jardin Appetizers (r1)
  {
    id: 'm1',
    categoryId: 'c1',
    name: 'Truffle Butter King Scallops',
    price: 34.00,
    description: 'Sautéed diver scallops nestled on a bed of delicate black truffle risotto, drizzled with sweet aged fig coulis and microsand greens.',
    image: 'https://images.unsplash.com/photo-1650288016253-c1ec87f7c0ea?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8N3x8c2NhbGxvcHN8ZW58MHx8MHx8fDA%3D',
    isAvailable: true,
    isVeg: false,
    isPopular: true
  },
  {
    id: 'm2',
    categoryId: 'c1',
    name: 'Burrata au Pistache',
    price: 26.00,
    description: 'Ultra-creamy pugliese burrata filled with basil essence, served over sliced ripe heirloom tomatoes, coated in raw Sicilian pistachio paste and honey drips.',
    image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: true
  },
  // Le Jardin Mains
  {
    id: 'm3',
    categoryId: 'c2',
    name: 'Champagne-Braised Salmon Fillet',
    price: 48.00,
    description: 'Wild Atlantic salmon lightly pan-seared and simmered in high-vintage Ruinart champagne cream, tossed with blanched baby leeks and royal caviar heaps.',
    image: 'https://images.unsplash.com/photo-1485962398705-ef6a13c41e8f?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: false,
    isPopular: true
  },
  {
    id: 'm4',
    categoryId: 'c2',
    name: 'Bordeaux Tenderloin Mignon',
    price: 58.00,
    description: 'Slices of Prime Angus tenderloin marinated in heavy red Bordeaux wine reduction, seared to standard medium-rare, accompanied by roasted garlic potato puree.',
    image: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: false
  },
  {
    id: 'm5',
    categoryId: 'c2',
    name: 'Provencal Artichoke Gnocchi',
    price: 38.00,
    description: 'Hand-rolled soft potato pillows glazed in local olive oil, cooked with braised Provencal artichoke hearts, sundried tomatoes, and graded Pecorino Romano.',
    image: 'https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: true
  },
  // Le Jardin Desserts
  {
    id: 'm6',
    categoryId: 'c3',
    name: 'Grand Marnier Soufflé',
    price: 18.00,
    description: 'Light, fluffy classic French soufflé infused with orange-flavored Grand Marnier liqueur, served oven-hot with vanilla bean gelato spoon.',
    image: 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: true,
    isPopular: true
  },
  // Le Jardin Drinks
  {
    id: 'm7',
    categoryId: 'c4',
    name: 'Dom Pérignon Vintage 2012',
    price: 110.00,
    description: 'Crisp bubbles, dry structure, boasting vibrant golden profiles of brioche, candied lemon zest, and mineral notes. Served per glass.',
    image: 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: true
  },

  // SpicyHunt Starters (r2)
  {
    id: 'm8',
    categoryId: 'c5',
    name: 'Hot & Sour Laksa Dumplings',
    price: 15.50,
    description: 'Minced wild mushrooms and ginger dumplings steamed and submerged in a thick, steaming spiced lemongrass laksa broth. Highly soothing.',
    image: 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: true,
    spicyLevel: 2
  },
  {
    id: 'm9',
    categoryId: 'c5',
    name: 'Szechuan Glass Noodle Bowl',
    price: 14.50,
    description: 'Translucent bean threads tossed in house dark garlic chili paste, roasted peanuts, sprigs of fresh cilantro, and crispy shallots.',
    image: 'https://images.unsplash.com/photo-1585032226651-759b368d7246?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: true,
    spicyLevel: 3,
    isPopular: true
  },
  // SpicyHunt Mains
  {
    id: 'm10',
    categoryId: 'c6',
    name: 'Gastro Spicy Kimchi Burger',
    price: 24.00,
    description: 'Thick flame-grilled beef chuck patty, double Cheddar layers, loaded with fiery house-fermented cabbage kimchi, wasabi mayonnaise, crispy frites bundle.',
    image: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=80',
    isAvailable: true,
    isVeg: false,
    spicyLevel: 2,
    isPopular: true
  },
  {
    id: 'm11',
    categoryId: 'c6',
    name: 'Crispy Basil Duck Claypot',
    price: 32.50,
    description: 'Succulent roasted duck breast with crisp skin, stir-fried with burning hot red bird-eye chilies, sweet Thai basil, long beans, and dark sweet soy.',
    image: 'https://images.unsplash.com/photo-1642231877874-ce3e205f39c0?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8cm9hc3RlZCUyMGR1Y2t8ZW58MHx8MHx8fDA%3D ',
    isAvailable: true,
    isVeg: false,
    spicyLevel: 3
  }
];

export const MOCK_OFFERS: Offer[] = [
  {
    id: 'o1',
    restaurantId: 'r1',
    title: 'Midweek Ruinart Champagne Celebration',
    description: 'Order any two signature entrees and get 50% off on premium Champagne cellars. Exclusive for table scanning visitors.',
    discountCode: 'CHAMPAGNE50',
    discountPercentage: 50,
    bannerColor: 'from-[#be935a] to-[#d6b785]'
  },
  {
    id: 'o2',
    restaurantId: 'r2',
    title: 'Late Night Soho Mixology hour',
    description: 'Buy two signature Asian-crafted Tropical Fusion drinks and grab one complimentary artisanal dessert. Dynamic trigger code verified by waiter.',
    discountCode: 'SOHOHOUR',
    discountPercentage: 33,
    bannerColor: 'from-[#ff455b] to-[#ff2b47]'
  }
];
