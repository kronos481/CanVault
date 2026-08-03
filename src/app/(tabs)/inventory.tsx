import { FlashList } from '@shopify/flash-list';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import { CanCard } from '@/components/can/can-card';
import { EmptyState } from '@/components/feedback/empty-state';
import { Screen } from '@/components/layout/screen';
import { AppText } from '@/components/ui/app-text';
import { AnimatedPressable } from '@/components/ui/animated-pressable';
import { Button } from '@/components/ui/button';
import { getCatalogBrand, getCatalogCanLine } from '@/features/catalog/catalog.v1';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { useI18n } from '@/i18n/use-i18n';
import type { TranslationKey } from '@/i18n/translations';
import { colors, radius, spacing, touch } from '@/theme/tokens';
import { normalizeSearchTerm } from '@/utils/normalize';

type InventoryFilter = 'all' | 'in_stock' | 'opened' | 'reserved' | 'low';
type InventorySort = 'recent' | 'brand' | 'fill';

const FILTERS: { value: InventoryFilter; label: TranslationKey }[] = [
  { value: 'all', label: 'common.all' },
  { value: 'in_stock', label: 'status.in_stock' },
  { value: 'opened', label: 'status.opened' },
  { value: 'reserved', label: 'status.reserved' },
  { value: 'low', label: 'inventory.filterLow' },
];

const SORTS: { value: InventorySort; label: TranslationKey }[] = [
  { value: 'recent', label: 'inventory.sortRecent' },
  { value: 'brand', label: 'inventory.sortBrand' },
  { value: 'fill', label: 'inventory.sortFill' },
];

export default function InventoryScreen() {
  const cans = useInventoryStore((state) => state.cans);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<InventoryFilter>('all');
  const [sort, setSort] = useState<InventorySort>('recent');
  const { t, language } = useI18n();
  const active = useMemo(
    () =>
      cans.filter(
        (can) => !['archived', 'consumed', 'sold', 'gifted', 'disposed'].includes(can.status),
      ),
    [cans],
  );
  const filtered = useMemo(() => {
    const query = normalizeSearchTerm(search);
    const matches = active.filter((can) => {
      const matchesQuery =
        !query ||
        normalizeSearchTerm(
          [
            getCatalogBrand(can.brandId)?.displayName,
            getCatalogCanLine(can.canLineId)?.displayName,
            can.customColorName,
            can.customColorCode,
          ]
            .filter(Boolean)
            .join(' '),
        ).includes(query);
      const matchesFilter =
        filter === 'all' ||
        (filter === 'low'
          ? can.estimatedFillPercent !== null && can.estimatedFillPercent <= 25
          : can.status === filter);
      return matchesQuery && matchesFilter;
    });

    const collator = new Intl.Collator(language === 'de' ? 'de-DE' : 'en-US');
    return matches.toSorted((left, right) => {
      if (sort === 'brand') {
        return collator.compare(
          getCatalogBrand(left.brandId)?.displayName ?? left.brandId,
          getCatalogBrand(right.brandId)?.displayName ?? right.brandId,
        );
      }
      if (sort === 'fill') {
        return (left.estimatedFillPercent ?? 101) - (right.estimatedFillPercent ?? 101);
      }
      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });
  }, [active, filter, language, search, sort]);

  const cycleSort = () => {
    const currentIndex = SORTS.findIndex((option) => option.value === sort);
    const next = SORTS[(currentIndex + 1) % SORTS.length];
    if (next) setSort(next.value);
  };
  const sortLabel = SORTS.find((option) => option.value === sort)?.label ?? 'inventory.sortRecent';

  return (
    <Screen>
      <View style={styles.header}>
        <AppText variant="display" accessibilityRole="header">
          {t('inventory.title')}
        </AppText>
        <AppText tone="muted">{t('inventory.subtitle', { count: active.length })}</AppText>
      </View>
      <View style={styles.search}>
        <Ionicons name="search" size={touch.icon} color={colors.textMuted} />
        <TextInput
          accessibilityLabel={t('inventory.searchPlaceholder')}
          placeholder={t('inventory.searchPlaceholder')}
          placeholderTextColor={colors.textSubtle}
          value={search}
          onChangeText={setSearch}
          style={styles.input}
        />
      </View>
      <View style={styles.controls}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filters}
        >
          {FILTERS.map((option) => {
            const selected = filter === option.value;
            return (
              <AnimatedPressable
                key={option.value}
                accessibilityRole="radio"
                accessibilityState={{ selected }}
                onPress={() => setFilter(option.value)}
                style={[styles.filterChip, selected && styles.filterChipSelected]}
              >
                <AppText variant="caption" tone={selected ? 'accent' : 'muted'}>
                  {t(option.label)}
                </AppText>
              </AnimatedPressable>
            );
          })}
        </ScrollView>
        <Button
          label={t(sortLabel)}
          accessibilityHint={t('inventory.sortHint')}
          icon="swap-vertical-outline"
          variant="ghost"
          style={styles.sortButton}
          onPress={cycleSort}
        />
      </View>
      {active.length === 0 ? (
        <EmptyState
          title={t('inventory.emptyTitle')}
          body={t('inventory.emptyBody')}
          actionLabel={t('inventory.emptyAction')}
          onAction={() => router.push('/add')}
        />
      ) : filtered.length === 0 ? (
        <EmptyState title={t('inventory.noResultsTitle')} body={t('inventory.noResultsBody')} />
      ) : (
        <FlashList
          data={filtered}
          numColumns={2}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <CanCard can={item} onPress={() => router.push(`/can/${item.id}`)} />
          )}
          contentContainerStyle={styles.list}
          keyboardShouldPersistTaps="handled"
        />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { paddingTop: spacing.md, paddingBottom: spacing.md, gap: spacing.xs },
  search: {
    minHeight: touch.minimum,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    paddingHorizontal: spacing.sm,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
  },
  input: { flex: 1, minHeight: touch.minimum, color: colors.text, fontSize: 16 },
  controls: { gap: spacing.xs, paddingVertical: spacing.sm },
  filters: { gap: spacing.xs },
  filterChip: {
    minHeight: 40,
    justifyContent: 'center',
    paddingHorizontal: spacing.sm,
    borderRadius: radius.pill,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  filterChipSelected: {
    backgroundColor: 'rgba(88,228,194,0.08)',
    borderColor: colors.accent,
  },
  sortButton: { alignSelf: 'flex-end' },
  list: { paddingHorizontal: 0, paddingBottom: 120 },
});
