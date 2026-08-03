import { Platform } from 'react-native';

export const colors = {
  background: '#090B0E',
  backgroundRaised: '#0D1014',
  surface: '#14181E',
  surfaceRaised: '#1A2028',
  surfacePressed: '#242C36',
  border: '#303944',
  borderStrong: '#475362',
  text: '#F5F7FA',
  textMuted: '#AEB8C4',
  textSubtle: '#7F8A98',
  accent: '#58E4C2',
  accentPressed: '#36C9A8',
  onAccent: '#061713',
  success: '#65D69B',
  warning: '#F5BF60',
  danger: '#FF7B7B',
  info: '#78B7FF',
  scrim: 'rgba(0, 0, 0, 0.60)',
  neutralCan: '#68727F',
  white: '#FFFFFF',
  black: '#000000',
} as const;

export const spacing = { xxs: 4, xs: 8, sm: 12, md: 16, lg: 24, xl: 32, xxl: 48 } as const;
export const radius = { sm: 8, md: 14, lg: 20, pill: 999 } as const;
export const typography = {
  display: 32,
  title: 24,
  heading: 18,
  body: 16,
  label: 14,
  caption: 12,
} as const;
export const touch = {
  minimum: Platform.select({ ios: 44, default: 48 }) ?? 48,
  icon: 24,
} as const;
export const motion = { fast: 150, normal: 220, slow: 300 } as const;

export const shadows = {
  card: {
    shadowColor: colors.black,
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.28,
    shadowRadius: 18,
    elevation: 6,
  },
} as const;
