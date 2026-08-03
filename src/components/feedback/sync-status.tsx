import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, View } from 'react-native';

import { useInventoryStore } from '@/features/inventory/inventory-store';
import { useI18n } from '@/i18n/use-i18n';
import { colors, spacing } from '@/theme/tokens';

import { AppText } from '../ui/app-text';

export function SyncStatus() {
  const queue = useInventoryStore((state) => state.syncQueue);
  const { t } = useI18n();
  const hasError = queue.some((operation) => operation.error);
  const label = hasError
    ? t('sync.error')
    : queue.length > 0
      ? t('sync.pending', { count: queue.length })
      : t('sync.synced');
  return (
    <View style={styles.row} accessibilityLabel={label}>
      <Ionicons
        name={
          hasError
            ? 'cloud-offline-outline'
            : queue.length
              ? 'cloud-upload-outline'
              : 'cloud-done-outline'
        }
        size={18}
        color={hasError ? colors.danger : colors.textMuted}
      />
      <AppText variant="caption" tone={hasError ? 'danger' : 'muted'}>
        {label}
      </AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.xxs },
});
