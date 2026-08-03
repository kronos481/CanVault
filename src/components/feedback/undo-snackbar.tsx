import { useEffect } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import Animated, { FadeInDown, FadeOutDown, ReduceMotion } from 'react-native-reanimated';

import { useInventoryStore } from '@/features/inventory/inventory-store';
import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, spacing, touch } from '@/theme/tokens';

import { AppText } from '../ui/app-text';

export function UndoSnackbar() {
  const lastUndo = useInventoryStore((state) => state.lastUndo);
  const undo = useInventoryStore((state) => state.undoLastMutation);
  const clear = useInventoryStore((state) => state.clearUndo);
  const { t } = useI18n();

  useEffect(() => {
    if (!lastUndo) return;
    const timeout = setTimeout(clear, 5000);
    return () => clearTimeout(timeout);
  }, [clear, lastUndo]);

  if (!lastUndo) return null;
  return (
    <Animated.View
      entering={FadeInDown.duration(220).reduceMotion(ReduceMotion.System)}
      exiting={FadeOutDown.duration(150).reduceMotion(ReduceMotion.System)}
      style={styles.container}
      accessibilityLiveRegion="polite"
    >
      <AppText variant="label">{t(lastUndo.messageKey)}</AppText>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={t('common.undo')}
        onPress={undo}
        style={styles.action}
      >
        <AppText variant="label" tone="accent">
          {t('common.undo')}
        </AppText>
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    left: spacing.md,
    right: spacing.md,
    bottom: spacing.md,
    minHeight: touch.minimum,
    borderRadius: radius.md,
    backgroundColor: colors.surfaceRaised,
    borderColor: colors.borderStrong,
    borderWidth: 1,
    paddingLeft: spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  action: {
    minWidth: touch.minimum,
    minHeight: touch.minimum,
    paddingHorizontal: spacing.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
