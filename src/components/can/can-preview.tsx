import { Image } from 'expo-image';
import { StyleSheet, View } from 'react-native';

import { colors, radius, shadows } from '@/theme/tokens';

const CAN_IMAGES = [
  require('../../../assets/cans/generic-mint.png'),
  require('../../../assets/cans/generic-violet.png'),
] as const;

interface CanPreviewProps {
  accentColor: string | null;
  compact?: boolean;
  variantKey?: string;
}

function imageIndexForKey(key: string): number {
  return Array.from(key).reduce((total, character) => total + character.charCodeAt(0), 0) % 2;
}

export function CanPreview({ accentColor, compact = false, variantKey = '' }: CanPreviewProps) {
  const accent = accentColor ?? colors.neutralCan;
  const source = CAN_IMAGES[imageIndexForKey(variantKey)] ?? CAN_IMAGES[0];
  return (
    <View style={[styles.window, compact && styles.windowCompact]} accessible={false}>
      <Image
        source={source}
        style={StyleSheet.absoluteFill}
        contentFit="cover"
        contentPosition="center"
        transition={180}
        cachePolicy="memory-disk"
        accessible={false}
      />
      <View style={styles.scrim} />
      <View style={[styles.accentChip, { backgroundColor: accent }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  window: {
    height: 184,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'flex-end',
    backgroundColor: colors.backgroundRaised,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    ...shadows.card,
  },
  windowCompact: { height: 124 },
  scrim: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(9,11,14,0.04)',
  },
  accentChip: {
    position: 'absolute',
    right: 10,
    bottom: 10,
    width: 28,
    height: 8,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.54)',
  },
});
