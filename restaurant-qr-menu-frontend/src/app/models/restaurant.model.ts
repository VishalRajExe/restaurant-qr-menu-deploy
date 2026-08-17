export interface Restaurant {
  id: string;
  name: string;
  slug?: string;
  tagline?: string;
  logo: string;
  logoUrl?: string;
  description?: string;
  photo?: string;
  coverImage?: string;
  banner?: string;
  address: string;
  phone: string;
  email?: string;
  openingHours?: string;
  cuisineType?: string;
  rating?: number;
  reviewCount?: number;
  currency?: string;
  tableCount?: number;
  isPublished?: boolean;
  socialLinks?: {
    instagram?: string;
    facebook?: string;
    twitter?: string;
  };
}
