export interface QRTemplate {
  id: string;
  name: string;
  fgColor: string;
  bgColor: string;
  logoCenter?: string;
  style: 'square' | 'rounded' | 'dots';
}
