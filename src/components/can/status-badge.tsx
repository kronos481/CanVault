import { StyleSheet, View } from 'react-native';

import type { CanStatus } from '@/features/inventory/types';
import type { TranslationKey } from '@/i18n/translations';
import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, spacing } from '@/theme/tokens';

import { AppText } from '../ui/app-text';

const statusTranslationKeys: Record<CanStatus, TranslationKey> = {
  in_stock: 'status.in_stock',
  opened: 'status.opened',
  reserved: 'status.reserved',
  empty: 'status.empty',
  consumed: 'status.consumed',
  sold: 'status.sold',
  gifted: 'status.gifted',
  lost: 'status.lost',
  damaged: 'status.damaged',
  disposed: 'status.disposed',
  collection: 'status.collection',
  archived: 'status.archived',
};

export function StatusBadge({ status }: { status: CanStatus }) {
  const { t } = useI18n();
  return (
    <View style={styles.badge}>
      <View style={styles.dot} />
      <AppText variant="caption">{t(statusTranslationKeys[status])}</AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xxs,
    backgroundColor: colors.surfaceRaised,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing.xs,
    paddingVertical: spacing.xxs,
  },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.accent },
});
