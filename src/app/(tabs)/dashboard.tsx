import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useMemo } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import Animated, { FadeInDown, ReduceMotion } from 'react-native-reanimated';

import { CanCard } from '@/components/can/can-card';
import { SyncStatus } from '@/components/feedback/sync-status';
import { AppHeader } from '@/components/layout/app-header';
import { Screen } from '@/components/layout/screen';
import { StatCard } from '@/components/statistics/stat-card';
import { AppText } from '@/components/ui/app-text';
import { Button } from '@/components/ui/button';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { estimateRemainingVolumeMl } from '@/features/statistics/calculations';
import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, shadows, spacing } from '@/theme/tokens';

export default function DashboardScreen() {
  const cans = useInventoryStore((state) => state.cans);
  const { t } = useI18n();
  const active = useMemo(
    () =>
      cans.filter(
        (can) => !['archived', 'consumed', 'sold', 'gifted', 'disposed'].includes(can.status),
      ),
    [cans],
  );
  const recent = active.slice(0, 2);
  const colorCount = new Set(
    active.map(
      (can) => `${can.canLineId}:${can.customColorCode ?? can.customColorName.toLocaleLowerCase()}`,
    ),
  ).size;
  const lineCount = new Set(active.map((can) => can.canLineId)).size;
  const volume = estimateRemainingVolumeMl(active);

  return (
    <Screen padded={false}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <AppHeader
          eyebrow={t('dashboard.eyebrow')}
          title={t('dashboard.title')}
          subtitle={t('dashboard.subtitle')}
          right={<SyncStatus />}
        />
        <Animated.View
          entering={FadeInDown.duration(260).reduceMotion(ReduceMotion.System)}
          style={styles.scanHero}
        >
          <View style={styles.scanHeroCopy}>
            <View style={styles.scanPill}>
              <AppText variant="caption" tone="accent">
                {t('dashboard.scanPill')}
              </AppText>
            </View>
            <AppText variant="title">{t('dashboard.scanHeroTitle')}</AppText>
            <AppText variant="caption" tone="muted">
              {t('dashboard.scanHeroBody')}
            </AppText>
            <Button
              label={t('dashboard.scan')}
              icon="scan-outline"
              style={styles.heroButton}
              onPress={() => router.push('/scanner')}
            />
          </View>
          <Image
            source={require('../../../assets/cans/generic-mint.png')}
            style={styles.scanHeroImage}
            contentFit="cover"
            contentPosition="center"
            transition={180}
            accessibilityLabel={t('can.previewIllustration')}
          />
        </Animated.View>
        <Animated.View
          entering={FadeInDown.delay(40).duration(260).reduceMotion(ReduceMotion.System)}
          style={styles.stats}
        >
          <StatCard label={t('dashboard.stock')} value={String(active.length)} />
          <StatCard
            label={t('dashboard.volume')}
            value={volume > 0 ? `${volume} ml` : '—'}
            hint={t('common.estimated')}
          />
          <StatCard label={t('dashboard.colors')} value={String(colorCount)} />
          <StatCard label={t('dashboard.lines')} value={String(lineCount)} />
        </Animated.View>
        <Animated.View
          entering={FadeInDown.delay(80).duration(260).reduceMotion(ReduceMotion.System)}
          style={styles.section}
        >
          <AppText variant="heading" accessibilityRole="header">
            {t('dashboard.quickActions')}
          </AppText>
          <View style={styles.actions}>
            <Button
              label={t('dashboard.scan')}
              icon="scan-outline"
              style={styles.actionButton}
              onPress={() => router.push('/scanner')}
            />
            <Button
              label={t('dashboard.addManual')}
              icon="add-circle-outline"
              variant="secondary"
              style={styles.actionButton}
              onPress={() => router.push('/add')}
            />
          </View>
        </Animated.View>
        <Animated.View
          entering={FadeInDown.delay(120).duration(260).reduceMotion(ReduceMotion.System)}
          style={styles.section}
        >
          <AppText variant="heading" accessibilityRole="header">
            {t('dashboard.recent')}
          </AppText>
          {recent.length ? (
            <View style={styles.recent}>
              {recent.map((can) => (
                <CanCard key={can.id} can={can} onPress={() => router.push(`/can/${can.id}`)} />
              ))}
            </View>
          ) : (
            <AppText tone="muted">{t('dashboard.emptyRecent')}</AppText>
          )}
        </Animated.View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: spacing.md, paddingBottom: 120 },
  scanHero: {
    minHeight: 224,
    flexDirection: 'row',
    overflow: 'hidden',
    marginBottom: spacing.md,
    borderRadius: radius.lg,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    ...shadows.card,
  },
  scanHeroCopy: {
    zIndex: 1,
    flex: 1,
    justifyContent: 'center',
    gap: spacing.sm,
    padding: spacing.lg,
  },
  scanPill: {
    alignSelf: 'flex-start',
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: radius.pill,
    backgroundColor: 'rgba(88,228,194,0.10)',
  },
  heroButton: { alignSelf: 'flex-start', marginTop: spacing.xs },
  scanHeroImage: { width: 124, height: '100%' },
  stats: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  section: { marginTop: spacing.xl, gap: spacing.md },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  actionButton: { flexGrow: 1 },
  recent: { flexDirection: 'row', marginHorizontal: -spacing.xs },
});
