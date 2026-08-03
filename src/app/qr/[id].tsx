import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import QRCode from 'react-native-qrcode-svg';

import { CanPreview } from '@/components/can/can-preview';
import { EmptyState } from '@/components/feedback/empty-state';
import { Screen } from '@/components/layout/screen';
import { AppText } from '@/components/ui/app-text';
import { Button } from '@/components/ui/button';
import { getCatalogBrand, getCatalogCanLine } from '@/features/catalog/catalog.v1';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { encodeCanvaultQrPayload } from '@/features/scanner/qr-payload';
import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, shadows, spacing } from '@/theme/tokens';

export default function CanQrScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const can = useInventoryStore((state) => state.cans.find((item) => item.id === id));
  const { t } = useI18n();

  if (!can) {
    return (
      <Screen>
        <EmptyState
          title={t('can.notFoundTitle')}
          body={t('can.notFoundBody')}
          actionLabel={t('common.back')}
          onAction={() => router.back()}
        />
      </Screen>
    );
  }

  const brand = getCatalogBrand(can.brandId);
  const line = getCatalogCanLine(can.canLineId);
  const value = encodeCanvaultQrPayload(can);

  return (
    <Screen>
      <View style={styles.header}>
        <Button label={t('common.back')} variant="ghost" icon="arrow-back" onPress={router.back} />
        <AppText variant="display" accessibilityRole="header">
          {t('qr.title')}
        </AppText>
        <AppText tone="muted">{t('qr.subtitle')}</AppText>
      </View>

      <View style={styles.card}>
        <View style={styles.canVisual}>
          <CanPreview accentColor={can.customHex} variantKey={can.canLineId} compact />
        </View>
        <View style={styles.productText}>
          <AppText variant="caption" tone="muted">
            {brand?.displayName} · {line?.displayName}
          </AppText>
          <AppText variant="title">{can.customColorName}</AppText>
          <AppText tone="muted">{can.customColorCode || t('common.notAvailable')}</AppText>
        </View>

        <View
          accessible
          accessibilityRole="image"
          accessibilityLabel={t('qr.accessibilityLabel', { color: can.customColorName })}
          style={styles.qrSurface}
        >
          <QRCode
            value={value}
            size={224}
            color={colors.background}
            backgroundColor={colors.white}
            quietZone={12}
            ecl="M"
          />
        </View>

        <View style={styles.privacyRow}>
          <Ionicons name="shield-checkmark-outline" size={22} color={colors.accent} />
          <AppText variant="caption" tone="muted" style={styles.privacyText}>
            {t('qr.privacy')}
          </AppText>
        </View>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { gap: spacing.sm, paddingBottom: spacing.lg },
  card: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.md,
    padding: spacing.lg,
    borderRadius: radius.lg,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    ...shadows.card,
  },
  canVisual: { width: 104, height: 124 },
  productText: { alignItems: 'center', gap: spacing.xxs },
  qrSurface: {
    padding: spacing.sm,
    borderRadius: radius.md,
    backgroundColor: colors.white,
  },
  privacyRow: {
    maxWidth: 320,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  privacyText: { flex: 1 },
});
