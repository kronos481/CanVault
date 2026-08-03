import { StyleSheet, TextInput, View, type TextInputProps } from 'react-native';

import { colors, radius, spacing, touch } from '@/theme/tokens';

import { AppText } from '../ui/app-text';

interface InputFieldProps extends TextInputProps {
  label: string;
  error?: string;
  hint?: string;
}

export function InputField({ label, error, hint, style, ...props }: InputFieldProps) {
  return (
    <View style={styles.container}>
      <AppText variant="label">{label}</AppText>
      <TextInput
        accessibilityLabel={label}
        accessibilityHint={hint}
        aria-invalid={Boolean(error)}
        placeholderTextColor={colors.textSubtle}
        selectionColor={colors.accent}
        style={[styles.input, error && styles.inputError, style]}
        {...props}
      />
      {hint && !error ? (
        <AppText variant="caption" tone="muted">
          {hint}
        </AppText>
      ) : null}
      {error ? (
        <AppText variant="caption" tone="danger" accessibilityRole="alert">
          {error}
        </AppText>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.xs },
  input: {
    minHeight: touch.minimum,
    color: colors.text,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    fontSize: 16,
  },
  inputError: { borderColor: colors.danger },
});
