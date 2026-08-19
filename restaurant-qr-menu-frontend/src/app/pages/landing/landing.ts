import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

interface MenuItem {
  name: string;
  price: string;
  description: string;
  image: string;
}

@Component({
  selector: 'app-landing',
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './landing.html',
  styleUrls: ['./landing.css']
})
export class Landing {
  activeMenuTab = signal<'breakfast' | 'lunch' | 'dinner'>('breakfast');

  // Menu items data
  breakfastItems: MenuItem[] = [
    { name: 'Classic French Toast', price: '$8.50', description: 'Golden brioche toast served with maple syrup and fresh berries', image: '/img/menu-1.jpg' },
    { name: 'Avocado Tartine & Poached Eggs', price: '$11.00', description: 'Artisanal sourdough topped with smashed Hass avocado and organic eggs', image: '/img/menu-2.jpg' },
    { name: 'Smoked Salmon Croissant', price: '$12.50', description: 'Flaky buttery croissant with Norwegian smoked salmon and dill cream', image: '/img/menu-3.jpg' },
    { name: 'Mediterranean Shakshuka', price: '$10.00', description: 'Spiced tomato bell pepper stew with poached eggs and toasted pita', image: '/img/menu-4.jpg' },
    { name: 'Belgian Waffle Delight', price: '$9.00', description: 'Crispy waffle with whipped mascarpone, honey, and dark chocolate drizzle', image: '/img/menu-5.jpg' },
    { name: 'Acai Superfood Bowl', price: '$8.00', description: 'Organic acai blend with chia seeds, granola, coconut flakes, and kiwi', image: '/img/menu-6.jpg' },
    { name: 'Eggs Florentine Deluxe', price: '$11.50', description: 'English muffin with baby spinach, hollandaise sauce, and micro herbs', image: '/img/menu-7.jpg' },
    { name: 'Matcha Green Tea Pancakes', price: '$9.50', description: 'Fluffy Japanese pancakes with matcha infused maple and fresh strawberries', image: '/img/menu-8.jpg' },
  ];

  lunchItems: MenuItem[] = [
    { name: 'Grilled Salmon Steak', price: '$24.50', description: 'Wild Atlantic salmon fillet with grilled asparagus and lemon herb butter', image: '/img/menu-1.jpg' },
    { name: 'Truffle Mushroom Burger', price: '$16.00', description: 'Prime beef patty with black truffle aioli, melted gruyere, and brioche', image: '/img/menu-2.jpg' },
    { name: 'Crispy Duck Confit', price: '$22.00', description: 'Slow-cooked duck leg with potato rosti, red cabbage and port wine jus', image: '/img/menu-3.jpg' },
    { name: 'Lobster Tagliolini', price: '$26.00', description: 'Handmade fresh pasta with butter-poached Maine lobster and cherry tomato', image: '/img/menu-4.jpg' },
    { name: 'Wagyu Beef Ribeye', price: '$34.00', description: '250g grilled Wagyu ribeye with roasted garlic mash and peppercorn sauce', image: '/img/menu-5.jpg' },
    { name: 'Herb-Crusted Rack of Lamb', price: '$28.00', description: 'New Zealand lamb rack with rosemary roasted potatoes and mint glaze', image: '/img/menu-6.jpg' },
    { name: 'Pan-Seared Sea Bass', price: '$23.00', description: 'Chilean sea bass with cauliflower puree, capers, and brown butter', image: '/img/menu-7.jpg' },
    { name: 'Wild Mushroom Risotto', price: '$17.50', description: 'Acquerello carnaroli rice with porcini mushrooms, parmesan, and thyme', image: '/img/menu-8.jpg' },
  ];

  dinnerItems: MenuItem[] = [
    { name: 'Chef Signature Tasting Platter', price: '$45.00', description: 'Curated 4-course sample plate featuring our finest meat & seafood creations', image: '/img/menu-1.jpg' },
    { name: 'Slow-Braised Beef Short Rib', price: '$29.00', description: '12-hour braised short rib in red wine reduction with truffle polenta', image: '/img/menu-2.jpg' },
    { name: 'Chilean Sea Bass Meuniere', price: '$32.00', description: 'Pan-fried sea bass with lemon parsley butter, roasted baby carrots', image: '/img/menu-3.jpg' },
    { name: 'Black Truffle Fettuccine', price: '$24.00', description: 'Fresh egg fettuccine with shaved Norcia black truffles and aged parmesan', image: '/img/menu-4.jpg' },
    { name: 'Prime Tomahawk Ribeye (For 2)', price: '$78.00', description: 'Dry-aged tomahawk steak with roasted marrow and smoked salt', image: '/img/menu-5.jpg' },
    { name: 'Sous-Vide Duck Breast', price: '$26.50', description: 'Spiced duck breast with orange cardamom glaze and parsnip puree', image: '/img/menu-6.jpg' },
    { name: 'Pan-Roasted Scallops', price: '$27.00', description: 'Hokkaido scallops with pea puree, pancetta crisp, and lemon foam', image: '/img/menu-7.jpg' },
    { name: 'Artisanal Chocolate Fondant', price: '$14.00', description: 'Warm Valrhona dark chocolate lava cake with Madagascar vanilla bean gelato', image: '/img/menu-8.jpg' },
  ];

  get currentMenuItems(): MenuItem[] {
    if (this.activeMenuTab() === 'breakfast') return this.breakfastItems;
    if (this.activeMenuTab() === 'lunch') return this.lunchItems;
    return this.dinnerItems;
  }

  setMenuTab(tab: 'breakfast' | 'lunch' | 'dinner') {
    this.activeMenuTab.set(tab);
  }
}
