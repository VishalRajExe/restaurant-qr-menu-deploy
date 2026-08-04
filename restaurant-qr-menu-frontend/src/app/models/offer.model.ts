export interface Offer {
  id: string;
  restaurantId: string;
  title: string;
  description: string;
  discountCode?: string;
  code?: string;
  badge?: string;
  validUntil?: string;
  isActive?: boolean;
  imageUrl?: string;
  discountPercentage?: number;
  bannerColor?: string;
}
