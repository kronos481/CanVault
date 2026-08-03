import { Alert, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { useMemo, useState } from 'react';

import { AppHeader } from '@/components/layout/app-header';
import { Screen } from '@/components/layout/screen';
import { AppText } from '@/components/ui/app-text';
import { Button } from '@/components/ui/button';
import { StatCard } from '@/components/statistics/stat-card';
import { APP_CONFIG } from '@/config/app';
import { shareInventoryCsv } from '@/features/export/share-inventory-csv';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { estimateRemainingVolumeMl } from '@/features/statistics/calculations';
import { useI18n } from '@/i18n/use-i18n';
import type { Language } from '@/i18n/translations';
import { colors, radius, spacing, touch } from '@/theme/tokens';

export default function MoreScreen() {
  const language = useInventoryStore((state) => state.language);
  const cans = useInventoryStore((state) => state.cans);
  const setLanguage = useInventoryStore((state) => state.setLanguage);
  const [exporting, setExporting] = useState(false);
  const { t } = useI18n();
  const locale = language === 'de' ? 'de-DE' : 'en-US';
  const stats = useMemo(() => {
    const active = cans.filter(
      (can) => !['archived', 'consumed', 'sold', 'gifted', 'disposed'].includes(can.status),
    );
    return {
      active: active.length,
      low: active.filter(
        (can) => can.estimatedFillPercent !== null && can.estimatedFillPercent <= 25,
      ).length,
      archived: cans.filter((can) => can.status === 'archived').length,
      volume: estimateRemainingVolumeMl(active),
      spent: cans.reduce((total, can) => total + (can.purchasePriceCents ?? 0), 0),
    };
  }, [cans]);

  const exportCsv = async () => {
    try {
      setExporting(true);
      await shareInventoryCsv(cans, t('more.exportDialogTitle'));
    } catch {
      Alert.alert(t('more.exportErrorTitle'), t('more.exportErrorBody'));
    } finally {
      setExporting(false);
    }
  };
  return (
    <Screen padded={false}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <AppHeader title={t('more.title')} subtitle={t('more.subtitle')} />
        <Section title={t('more.statistics')}>
          <View style={styles.statsGrid}>
            <StatCard label={t('dashboard.stock')} value={String(stats.active)} />
            <StatCard label={t('more.lowStock')} value={String(stats.low)} />
            <StatCard label={t('more.archived')} value={String(stats.archived)} />
            <StatCard
              label={t('dashboard.volume')}
              value={`${stats.volume} ml`}
              hint={t('common.estimated')}
            />
          </View>
          <View style={styles.spentRow}>
            <AppText tone="muted">{t('more.totalSpent')}</AppText>
            <AppText variant="heading">
              {new Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR' }).format(
                stats.spent / 100,
              )}
            </AppText>
          </View>
        </Section>
        <Section title={t('more.language')}>
          <View style={styles.languageRow}>
            <LanguageButton
              language="de"
              current={language}
              label={t('more.languageDe')}
              onPress={setLanguage}
            />
            <LanguageButton
              language="en"
              current={language}
              label={t('more.languageEn')}
              onPress={setLanguage}
            />
          </View>
        </Section>
        <Section title={t('more.catalog')}>
          <AppText>{t('more.catalogValue', { version: APP_CONFIG.catalogVersion })}</AppText>
        </Section>
        <Section title={t('more.export')}>
          <AppText tone="muted">{t('more.exportHint')}</AppText>
          <Button
            label={t('more.exportCsv')}
            icon="share-outline"
            loading={exporting}
            disabled={cans.length === 0}
            onPress={() => void exportCsv()}
          />
        </Section>
        <Section title={t('more.legalAssets')}>
          <AppText tone="muted">{t('more.legalAssetsHint')}</AppText>
        </Section>
      </ScrollView>
    </Screen>
  );
}

function Section({ title, children }: React.PropsWithChildren<{ title: string }>) {
  return (
    <View style={styles.section}>
      <AppText variant="heading" accessibilityRole="header">
        {title}
      </AppText>
      {children}
    </View>
  );
}

function LanguageButton({
  language,
  current,
  label,
  onPress,
}: {
  language: Language;
  current: Language;
  label: string;
  onPress: (language: Language) => void;
}) {
  const selected = language === current;
  return (
    <Pressable
      accessibilityRole="radio"
      accessibilityState={{ selected }}
      onPress={() => onPress(language)}
      style={[styles.languageButton, selected && styles.languageSelected]}
    >
      <AppText variant="label" tone={selected ? 'accent' : 'default'}>
        {label}
      </AppText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: spacing.md, paddingBottom: 120 },
  section: {
    gap: spacing.sm,
    padding: spacing.md,
    marginBottom: spacing.md,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
  },
  languageRow: { flexDirection: 'row', gap: spacing.xs },
  statsGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs },
  spentRow: {
    minHeight: touch.minimum,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.md,
    paddingTop: spacing.xs,
  },
  languageButton: {
    minHeight: touch.minimum,
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  languageSelected: { borderColor: colors.accent, backgroundColor: 'rgba(88,228,194,0.08)' },
});
