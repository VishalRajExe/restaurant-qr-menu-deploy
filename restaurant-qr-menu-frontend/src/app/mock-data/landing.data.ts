import { PricingPlan } from '../models/pricing-plan.model';
import { Testimonial } from '../models/testimonial.model';

export const STATS_DATA = [
  { value: '500+', label: 'Elite Restaurants' },
  { value: '1.2M+', label: 'Digital Menu Views' },
  { value: '450K+', label: 'QR Tables Active' },
  { value: '18%', label: 'Avg Order Value Increase' }
];

export const CLIENT_LOGOS = [
  'Culinary Studio', 'Novikov Grill', 'Marriott Restorations', 
  'Le Petite Bistro', 'Hakkasan Lounge', 'Aqua Gastrobar'
];

export const TESTIMONIALS: Testimonial[] = [
  {
    id: 't1',
    name: 'Antoine Laurent',
    role: 'Executive Chef & Owner',
    restaurantName: 'Le Jardin de Provence',
    avatar: 'https://images.unsplash.com/photo-1577219491135-ce391730fb2c?w=120&auto=format&fit=crop&q=80',
    comment: 'Plated menu edits used to cost us hundreds of dollars in weekly re-printing. Now we tweak specials in 10 seconds. Guests absolutely adore the immersive smartphone layout.',
    rating: 5
  },
  {
    id: 't2',
    name: 'Elena Rostova',
    role: 'Director of Hospitality',
    restaurantName: 'Novikov Grill & Bar',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=120&auto=format&fit=crop&q=80',
    comment: 'The dashboard scans count keeps us ahead. We track exactly which dishes grab the most eyes at individual tables. It has completely modernized our dining experience.',
    rating: 5
  },
  {
    id: 't3',
    name: 'Marcus Sterling',
    role: 'Founder',
    restaurantName: 'The Sterling Steakhouse',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=80',
    comment: 'The interface feels extraordinarily high-end. It matches our brand equity perfectly. Converting diners into regular subscribers is hassle-free using the promo banner triggers.',
    rating: 5
  }
];

export const PRICING_PLANS: PricingPlan[] = [
  {
    id: 'p1',
    name: 'Bistro Starter',
    price: '$29',
    billing: 'billed monthly',
    description: 'Perfect for small local bistros and pop-up dining looking to digitalize fast.',
    features: [
      '1 Active Restaurant',
      'Up to 3 Menu Categories',
      'Up to 50 Food Items',
      'Dynamic QR Code Generator',
      'Essential Analytics (Scans Count)',
      'Basic Guest Interface theme'
    ],
    buttonText: 'Launch Starter Free'
  },
  {
    id: 'p2',
    name: 'Gourmet Growth',
    price: '$79',
    billing: 'billed monthly',
    description: 'Recommended for single-location premium venues and upscale gourmet bars.',
    features: [
      '1 Restaurant + Multi-Terminal',
      'Unlimited Categories',
      'Unlimited Food Items',
      'Custom Branded QR Templates',
      'Deep View & Scan Analytics',
      'Interactive Custom Theme Editor',
      'Promotional Banner Offers',
      'Priority Support (24hr Live)'
    ],
    isPopular: true,
    buttonText: 'Claim Your Growth Trial'
  },
  {
    id: 'p3',
    name: 'Grande Enterprise',
    price: '$149',
    billing: 'billed monthly',
    description: 'Bespoke features built for restaurant chains, heavy multi-branch venues and hotels.',
    features: [
      'Unlimited Restaurants / Branches',
      'Multi-user Manager Roles',
      'Deep Custom-Domain White-Label',
      'Live Table-Service Requests Integration',
      'Spring Boot Direct API Access',
      'Dedicated Customer Success Manager',
      'Premium Custom Canvas QR Styles',
      '99.9% Cloud Run Iingress SLA'
    ],
    buttonText: 'Request VIP Demo'
  }
];

export const PORTFOLIO_HOWITWORKS = [
  {
    step: '01',
    title: 'Register Restaurant',
    desc: 'Fill out your boutique venue profile, including elegant cover banners, logos, and custom operational hours.'
  },
  {
    step: '02',
    title: 'Curate Menus',
    desc: 'Input appetizers, entrees, wine lists, dietary labels (Veg/Non-Veg, Spicy levels) with stunning high-contrast food photography.'
  },
  {
    step: '03',
    title: 'Tailor QR Codes',
    desc: 'Customize QR codes matching your branding. Insert logos, edit color schemes, choose dots/rounded layouts, and export.'
  },
  {
    step: '04',
    title: 'Guests Scan & Admire',
    desc: 'Affix codes on tables. Diners instantly scan, search active promotions, scroll sticky folders, and view beautiful items.'
  }
];
