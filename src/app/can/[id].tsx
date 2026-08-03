import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import type { Href } from 'expo-router';
import { Alert, Pressable, ScrollView, StyleSheet, View } from 'react-native';

import { CanPreview } from '@/components/can/can-preview';
import { FillLevelIndicator } from '@/components/can/fill-level-indicator';
import { StatusBadge } from '@/components/can/status-badge';
import { EmptyState } from '@/components/feedback/empty-state';
import { UndoSnackbar } from '@/components/feedback/undo-snackbar';
import { Screen } from '@/components/layout/screen';
import { AppText } from '@/components/ui/app-text';
import { Button } from '@/components/ui/button';
import { getCatalogBrand, getCatalogCanLine } from '@/features/catalog/catalog.v1';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { useI18n } from '@/i18n/use-i18n';
import type { TranslationKey } from '@/i18n/translations';
import { colors, radius, spacing, touch } from '@/theme/tokens';
import { formatCurrency } from '@/utils/money';

const fillOptions = [100, 90, 75, 50, 25, 10, 0] as const;
const eventKeys: Record<string, TranslationKey> = {
  created: 'timeline.created',
  fill_changed: 'timeline.fill_changed',
  archived: 'timeline.archived',
  restored: 'timeline.restored',
};

export default function CanDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const can = useInventoryStore((state) => state.cans.find((item) => item.id === id));
  const events = useInventoryStore((state) =>
    state.events.filter((event) => event.userCanId === id),
  );
  const updateFill = useInventoryStore((state) => state.updateFill);
  const archiveCan = useInventoryStore((state) => state.archiveCan);
  const restoreCan = useInventoryStore((state) => state.restoreCan);
  const { t, language } = useI18n();

  if (!can)
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
  const brand = getCatalogBrand(can.brandId);
  const line = getCatalogCanLine(can.canLineId);
  const locale = language === 'de' ? 'de-DE' : 'en-US';

  const confirmArchive = () =>
    Alert.alert(t('can.archiveConfirmTitle'), t('can.archiveConfirmBody'), [
      { text: t('common.cancel'), style: 'cancel' },
      { text: t('can.archive'), style: 'destructive', onPress: () => archiveCan(can.id) },
    ]);

  return (
    <Screen padded={false}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.topBar}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={t('common.back')}
            onPress={() => router.back()}
            style={styles.back}
          >
            <Ionicons name="arrow-back" size={touch.icon} color={colors.text} />
          </Pressable>
          <AppText variant="label">{t('can.detailTitle')}</AppText>
          <View style={styles.back} />
        </View>
        <View style={[styles.colorBar, { backgroundColor: can.customHex ?? colors.neutralCan }]} />
        <CanPreview accentColor={can.customHex} variantKey={can.canLineId} />
        <View style={styles.identity}>
          <AppText variant="caption" tone="muted">
            {brand?.displayName ?? can.brandId}
          </AppText>
          <AppText variant="title">{line?.displayName ?? can.canLineId}</AppText>
          <AppText variant="display">{can.customColorName}</AppText>
          <AppText tone="muted">{can.customColorCode || t('common.notAvailable')}</AppText>
          <StatusBadge status={can.status} />
        </View>
        <AppText variant="caption" tone="muted">
          {t('can.colorDisclaimer')}
        </AppText>

        <Button
          label={t('can.showQr')}
          icon="qr-code-outline"
          variant="secondary"
          onPress={() => router.push(`/qr/${can.id}` as Href)}
        />

        <View style={styles.section}>
          <AppText variant="heading" accessibilityRole="header">
            {t('can.updateFill')}
          </AppText>
          <FillLevelIndicator value={can.estimatedFillPercent} />
          <AppText tone="muted">{t('can.fillEstimate')}</AppText>
          <View style={styles.fillGrid}>
            {fillOptions.map((value) => {
              const selected = can.estimatedFillPercent === value;
              return (
                <Pressable
                  key={value}
                  accessibilityRole="radio"
                  accessibilityLabel={`${value} %`}
                  accessibilityState={{ selected }}
                  onPress={() => updateFill(can.id, value)}
                  style={[styles.fillButton, selected && styles.fillSelected]}
                >
                  <AppText variant="label" tone={selected ? 'accent' : 'default'}>
                    {value} %
                  </AppText>
                </Pressable>
              );
            })}
          </View>
        </View>

        <View style={styles.section}>
          <InfoRow
            label={t('can.purchasePrice')}
            value={formatCurrency(can.purchasePriceCents, locale, can.currency)}
          />
          <InfoRow
            label={t('can.acquiredAt')}
            value={new Intl.DateTimeFormat(locale).format(new Date(can.acquiredAt))}
          />
          <InfoRow
            label={t('can.volume')}
            value={can.volumeMl ? `${can.volumeMl} ml` : t('common.notAvailable')}
          />
          <InfoRow label={t('can.status')} value={t(`status.${can.status}` as TranslationKey)} />
        </View>

        <View style={styles.section}>
          <AppText variant="heading" accessibilityRole="header">
            {t('can.history')}
          </AppText>
          {events.map((event) => (
            <View key={event.id} style={styles.timelineRow}>
              <View style={styles.timelineDot} />
              <View style={styles.timelineText}>
                <AppText variant="label">
                  {t(eventKeys[event.eventType] ?? 'timeline.created')}
                </AppText>
                <AppText variant="caption" tone="muted">
                  {new Intl.DateTimeFormat(locale, {
                    dateStyle: 'medium',
                    timeStyle: 'short',
                  }).format(new Date(event.occurredAt))}
                </AppText>
              </View>
            </View>
          ))}
        </View>

        {can.status === 'archived' ? (
          <Button label={t('can.restore')} onPress={() => restoreCan(can.id)} icon="refresh" />
        ) : (
          <Button
            label={t('can.archive')}
            onPress={confirmArchive}
            icon="archive-outline"
            variant="danger"
          />
        )}
      </ScrollView>
      <UndoSnackbar />
    </Screen>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.infoRow}>
      <AppText tone="muted">{label}</AppText>
      <AppText variant="label" style={styles.infoValue}>
        {value}
      </AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: spacing.md, paddingBottom: 120, gap: spacing.md },
  topBar: {
    minHeight: touch.minimum,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.xs,
  },
  back: {
    width: touch.minimum,
    height: touch.minimum,
    alignItems: 'center',
    justifyContent: 'center',
  },
  colorBar: { height: 14, borderRadius: radius.pill },
  identity: { gap: spacing.xs },
  section: {
    gap: spacing.sm,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    backgroundColor: colors.surface,
  },
  fillGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs },
  fillButton: {
    minWidth: touch.minimum + 12,
    minHeight: touch.minimum,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.borderStrong,
  },
  fillSelected: { borderColor: colors.accent, backgroundColor: 'rgba(88,228,194,0.08)' },
  infoRow: {
    minHeight: touch.minimum,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.md,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  infoValue: { flex: 1, textAlign: 'right' },
  timelineRow: { flexDirection: 'row', gap: spacing.sm },
  timelineDot: {
    width: 10,
    height: 10,
    marginTop: 6,
    borderRadius: 5,
    backgroundColor: colors.accent,
  },
  timelineText: { flex: 1, paddingBottom: spacing.sm },
});
