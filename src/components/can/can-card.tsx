import { memo } from 'react';
import { StyleSheet, View } from 'react-native';

import { getCatalogBrand, getCatalogCanLine } from '@/features/catalog/catalog.v1';
import type { UserCan } from '@/features/inventory/types';
import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, shadows, spacing } from '@/theme/tokens';

import { AnimatedPressable } from '../ui/animated-pressable';
import { AppText } from '../ui/app-text';
import { CanPreview } from './can-preview';
import { FillLevelIndicator } from './fill-level-indicator';
import { StatusBadge } from './status-badge';

interface CanCardProps {
  can: UserCan;
  onPress: () => void;
}

export const CanCard = memo(function CanCard({ can, onPress }: CanCardProps) {
  const { t } = useI18n();
  const brand = getCatalogBrand(can.brandId);
  const line = getCatalogCanLine(can.canLineId);
  const fillLabel =
    can.estimatedFillPercent === null ? t('can.fillUnknown') : `${can.estimatedFillPercent} %`;
  const accessibleLabel = `${brand?.displayName ?? can.brandId}, ${line?.displayName ?? can.canLineId}, ${can.customColorName}, ${fillLabel}`;
  return (
    <AnimatedPressable
      accessibilityRole="button"
      accessibilityLabel={accessibleLabel}
      onPress={onPress}
      style={styles.card}
    >
      <View style={[styles.colorBar, { backgroundColor: can.customHex ?? colors.neutralCan }]} />
      <CanPreview accentColor={can.customHex} variantKey={can.canLineId} compact />
      <View style={styles.content}>
        <AppText variant="caption" tone="muted" numberOfLines={1}>
          {brand?.displayName ?? can.brandId}
        </AppText>
        <AppText variant="label" numberOfLines={2}>
          {line?.displayName ?? can.canLineId}
        </AppText>
        <AppText variant="heading" numberOfLines={2}>
          {can.customColorName}
        </AppText>
        <AppText variant="caption" tone="muted">
          {can.customColorCode || t('common.notAvailable')}
        </AppText>
        <FillLevelIndicator value={can.estimatedFillPercent} />
        <View style={styles.footer}>
          <AppText variant="caption" tone="muted">
            {fillLabel}
          </AppText>
          <StatusBadge status={can.status} />
        </View>
      </View>
    </AnimatedPressable>
  );
});

const styles = StyleSheet.create({
  card: {
    flex: 1,
    margin: spacing.xs,
    overflow: 'hidden',
    minWidth: 0,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    ...shadows.card,
  },
  colorBar: { height: 12 },
  content: { padding: spacing.sm, gap: spacing.xs },
  footer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.xs,
  },
});
