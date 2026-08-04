# Design.md — Visual Design System

### 1. Design Principles
- **Clarity first**: menu must be scannable in under 5 seconds — customers are often hungry, in a hurry, or in low light.
- **Restaurant-brand friendly**: the platform's default theme should be neutral enough that a restaurant's logo/colors don't clash.
- **Consistency between Admin and Public apps**: same design tokens, different density (admin denser/data-heavy, public airy/visual).

### 2. Color Palette

#### Public Menu (Light Mode — default)
| Token | Hex | Usage |
|---|---|---|
| `--color-bg` | #FFFFFF | Page background |
| `--color-surface` | #F7F7F5 | Cards, item tiles |
| `--color-text-primary` | #1A1A1A | Headings, item names |
| `--color-text-secondary` | #6B6B6B | Descriptions |
| `--color-primary` | #E85D28 | Primary accent (CTA, active tab) — warm, appetite-associated orange |
| `--color-veg` | #2E7D32 | Veg badge |
| `--color-nonveg` | #C62828 | Non-veg badge |
| `--color-offer` | #FFB300 | Offer/discount badges |
| `--color-border` | #E5E5E2 | Dividers, card borders |

#### Public Menu (Dark Mode)
| Token | Hex | Usage |
|---|---|---|
| `--color-bg` | #121212 | Page background |
| `--color-surface` | #1E1E1E | Cards |
| `--color-text-primary` | #F2F2F2 | Headings |
| `--color-text-secondary` | #A0A0A0 | Descriptions |
| `--color-primary` | #FF7A45 | Primary accent (slightly brighter for contrast) |
| `--color-veg` | #4CAF50 | Veg badge |
| `--color-nonveg` | #EF5350 | Non-veg badge |
| `--color-border` | #2C2C2C | Dividers |

#### Admin Panel (Light Mode — default, denser/utility feel)
| Token | Hex | Usage |
|---|---|---|
| `--color-bg` | #F5F6F8 | App background |
| `--color-surface` | #FFFFFF | Panels, tables, cards |
| `--color-primary` | #2F6FED | Primary actions, links, active nav |
| `--color-success` | #22A06B | Success states, "Available" |
| `--color-warning` | #F0A93C | Pending/low-stock states |
| `--color-danger` | #E5484D | Delete, destructive actions |
| `--color-text-primary` | #101828 | Headings |
| `--color-text-secondary` | #667085 | Table labels, helper text |
| `--color-border` | #E4E7EC | Table/card borders |

### 3. Typography
- **Font family:** `Inter` (or `Poppins` for a slightly warmer public-menu feel) — system-ui fallback.
- **Public menu:**
  - Restaurant name / H1: 28–32px, semi-bold (600).
  - Category headers: 20px, semi-bold.
  - Item name: 16px, medium (500).
  - Item description: 13–14px, regular, secondary color.
  - Price: 16px, bold, primary color.
- **Admin panel:**
  - Page title: 22px, semi-bold.
  - Section headers: 16px, semi-bold.
  - Table text: 14px, regular.
  - Labels/captions: 12px, medium, secondary color.
- Line height: 1.4–1.6 for body text; 1.2 for headings.

### 4. Spacing & Layout
- Base spacing unit: 4px (use multiples: 4, 8, 12, 16, 24, 32, 48).
- Public menu: generous vertical rhythm (16–24px between item cards) to keep it airy and appetizing.
- Admin panel: tighter spacing (8–12px) to maximize data density in tables/dashboards.
- Border radius: 12px for cards/buttons on public menu (soft, friendly); 8px for admin panel (crisp, functional).
- Max content width: 480px centered for public menu on desktop (mobile-first, doesn't need to stretch wide); 1280px for admin panel.

### 5. Components
- **Item Card (public):** image (rounded top corners, 4:3 or 1:1), name + price row, description (2-line clamp), veg/non-veg dot badge top-left of image.
- **Category Tabs (public):** horizontal scrollable pill tabs, active tab filled with `--color-primary`.
- **Offer Banner (public):** full-width carousel/banner strip below restaurant header, `--color-offer` accent.
- **Admin Data Table:** zebra-free, border-based rows, sticky header, inline row actions (edit/delete icons), status as colored pill (success/warning/danger).
- **Admin Dashboard Cards:** stat cards (label + big number + small trend indicator) using `--color-surface` with subtle shadow (`0 1px 3px rgba(16,24,40,0.08)`).

### 6. Iconography
- Line-style icon set (e.g., Lucide/Feather-style) — consistent stroke width (1.5–2px) across admin and public apps.
- Veg/non-veg indicator uses the standard square-with-dot convention (green square + green dot = veg; red/brown square + dot = non-veg) for instant recognizability.

### 7. Dark/Light Mode Behavior
- Toggle persists per browser session (public menu) via local state (no login required).
- Admin panel: dark mode optional, lower priority than public menu dark mode.
- All colors must be defined as CSS custom properties (tokens above) so theme switching is a single class/attribute toggle (`data-theme="dark"`), never hardcoded hex values in components.

### 8. Multi-Language Support (Visual Considerations)
- Layout must tolerate 30–40% text length variation (e.g., German, Arabic) without breaking card layouts — use flexible widths, avoid fixed-height text containers for names/descriptions.
- RTL language support (Arabic, Hebrew) should mirror layout direction, not just flip text.

### 9. Accessibility
- Minimum contrast ratio 4.5:1 for body text against background in both light and dark themes.
- Tap targets minimum 44x44px on public menu (mobile-first).
- Veg/non-veg and offer badges must not rely on color alone — include icon/shape distinction for color-blind users.
