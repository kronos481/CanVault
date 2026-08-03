import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { colors, spacing } from '@/theme/tokens';
import { useI18n } from '@/i18n/use-i18n';

import { AppText } from '../ui/app-text';

export function LoadingState() {
  const { t } = useI18n();
  return (
    <View style={styles.container} accessibilityRole="progressbar">
      <ActivityIndicator color={colors.accent} size="large" />
      <AppText tone="muted">{t('common.loading')}</AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.md },
});
