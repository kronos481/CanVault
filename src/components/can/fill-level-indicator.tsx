import { StyleSheet, View } from 'react-native';

import { colors, radius } from '@/theme/tokens';

interface FillLevelIndicatorProps {
  value: number | null;
}

export function FillLevelIndicator({ value }: FillLevelIndicatorProps) {
  const normalized = value ?? 0;
  const fillColor = normalized <= 25 ? colors.warning : colors.accent;
  return (
    <View
      style={styles.track}
      accessibilityRole="progressbar"
      accessibilityValue={{ min: 0, max: 100, now: value ?? undefined }}
    >
      <View style={[styles.fill, { width: `${normalized}%`, backgroundColor: fillColor }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    height: 6,
    width: '100%',
    overflow: 'hidden',
    borderRadius: radius.pill,
    backgroundColor: colors.border,
  },
  fill: { height: '100%', borderRadius: radius.pill },
});
