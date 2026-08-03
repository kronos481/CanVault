import { FlashList } from '@shopify/flash-list';
import { router } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { CanCard } from '@/components/can/can-card';
import { EmptyState } from '@/components/feedback/empty-state';
import { Screen } from '@/components/layout/screen';
import { AppText } from '@/components/ui/app-text';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { useI18n } from '@/i18n/use-i18n';
import { spacing } from '@/theme/tokens';

export default function ArchiveScreen() {
  const cans = useInventoryStore((state) => state.cans);
  const { t } = useI18n();
  return (
    <Screen>
      <View style={styles.header}>
        <AppText variant="display" accessibilityRole="header">
          {t('archive.title')}
        </AppText>
        <AppText tone="muted">{t('archive.subtitle')}</AppText>
      </View>
      {cans.length === 0 ? (
        <EmptyState title={t('archive.emptyTitle')} body={t('archive.emptyBody')} />
      ) : (
        <FlashList
          data={cans}
          numColumns={2}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <CanCard can={item} onPress={() => router.push(`/can/${item.id}`)} />
          )}
          contentContainerStyle={styles.list}
        />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { paddingTop: spacing.md, paddingBottom: spacing.md, gap: spacing.xs },
  list: { paddingBottom: 120 },
});
