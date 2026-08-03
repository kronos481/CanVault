import { Ionicons } from '@expo/vector-icons';
import type { ComponentProps } from 'react';
import { ActivityIndicator, StyleSheet, type StyleProp, type ViewStyle } from 'react-native';

import { colors, radius, spacing, touch } from '@/theme/tokens';

import { AnimatedPressable } from './animated-pressable';
import { AppText } from './app-text';

type IconName = ComponentProps<typeof Ionicons>['name'];

interface ButtonProps {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  icon?: IconName;
  disabled?: boolean;
  loading?: boolean;
  accessibilityHint?: string;
  style?: StyleProp<ViewStyle>;
}

export function Button({
  label,
  onPress,
  variant = 'primary',
  icon,
  disabled = false,
  loading = false,
  accessibilityHint,
  style,
}: ButtonProps) {
  const isDisabled = disabled || loading;
  return (
    <AnimatedPressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityHint={accessibilityHint}
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      onPress={onPress}
      style={[styles.base, variantStyles[variant], style, isDisabled && styles.disabled]}
    >
      {loading ? (
        <ActivityIndicator color={variant === 'primary' ? colors.onAccent : colors.text} />
      ) : null}
      {!loading && icon ? (
        <Ionicons
          name={icon}
          size={touch.icon}
          color={
            variant === 'primary'
              ? colors.onAccent
              : variant === 'danger'
                ? colors.danger
                : colors.text
          }
        />
      ) : null}
      <AppText
        variant="label"
        style={[
          styles.label,
          variant === 'primary' && styles.primaryLabel,
          variant === 'danger' && styles.dangerLabel,
        ]}
      >
        {label}
      </AppText>
    </AnimatedPressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: touch.minimum,
    paddingHorizontal: spacing.md,
    borderRadius: radius.md,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    borderWidth: 1,
  },
  label: { textAlign: 'center' },
  primaryLabel: { color: colors.onAccent },
  dangerLabel: { color: colors.danger },
  disabled: { opacity: 0.42 },
});

const variantStyles = StyleSheet.create({
  primary: { backgroundColor: colors.accent, borderColor: colors.accent },
  secondary: { backgroundColor: colors.surfaceRaised, borderColor: colors.borderStrong },
  ghost: { backgroundColor: 'transparent', borderColor: colors.border },
  danger: { backgroundColor: 'rgba(255,123,123,0.08)', borderColor: colors.danger },
});
