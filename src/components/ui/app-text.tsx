import type { PropsWithChildren } from 'react';
import { StyleSheet, Text, type TextProps } from 'react-native';

import { colors, typography } from '@/theme/tokens';

type TextVariant = 'display' | 'title' | 'heading' | 'body' | 'label' | 'caption';

interface AppTextProps extends TextProps {
  variant?: TextVariant;
  tone?: 'default' | 'muted' | 'accent' | 'danger';
}

export function AppText({
  children,
  style,
  variant = 'body',
  tone = 'default',
  ...props
}: PropsWithChildren<AppTextProps>) {
  return (
    <Text
      allowFontScaling
      maxFontSizeMultiplier={2}
      style={[styles.base, styles[variant], toneStyles[tone], style]}
      {...props}
    >
      {children}
    </Text>
  );
}

const styles = StyleSheet.create({
  base: { color: colors.text },
  display: { fontSize: typography.display, lineHeight: 38, fontWeight: '800', letterSpacing: -0.8 },
  title: { fontSize: typography.title, lineHeight: 31, fontWeight: '700', letterSpacing: -0.4 },
  heading: { fontSize: typography.heading, lineHeight: 25, fontWeight: '700' },
  body: { fontSize: typography.body, lineHeight: 24, fontWeight: '400' },
  label: { fontSize: typography.label, lineHeight: 20, fontWeight: '600' },
  caption: { fontSize: typography.caption, lineHeight: 17, fontWeight: '500' },
});

const toneStyles = StyleSheet.create({
  default: { color: colors.text },
  muted: { color: colors.textMuted },
  accent: { color: colors.accent },
  danger: { color: colors.danger },
});
