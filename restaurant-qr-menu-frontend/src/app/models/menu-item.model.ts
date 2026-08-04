export interface MenuItem {

     id: string;
  categoryId: string;
  name: string;
  price: number;
  description: string;
  image: string;
  isAvailable: boolean;
  isVeg: boolean;
  isPopular?: boolean;
  spicyLevel?: number;
  calories?: number;
}
