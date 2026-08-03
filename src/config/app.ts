import brand from '../../app-brand.json';

export const APP_CONFIG = {
  name: process.env.EXPO_PUBLIC_APP_NAME ?? brand.displayName,
  slug: brand.slug,
  catalogVersion: '2026.08.01-v1',
  defaultCurrency: 'EUR',
  defaultLocale: 'de-DE',
} as const;
