import { Image } from 'expo-image';
import { StyleSheet, View } from 'react-native';

import { APP_CONFIG } from '@/config/app';
import { colors, spacing } from '@/theme/tokens';

import { AppText } from '../ui/app-text';

interface AppHeaderProps {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  right?: React.ReactNode;
}

export function AppHeader({ eyebrow, title, subtitle, right }: AppHeaderProps) {
  return (
    <View style={styles.container}>
      <View style={styles.brandRow}>
        <View style={styles.brandIdentity}>
          <Image
            source={require('../../../assets/brand/canvault-icon.png')}
            style={styles.brandIcon}
            contentFit="cover"
            transition={180}
            accessibilityLabel={`${APP_CONFIG.name} Logo`}
          />
          <AppText variant="caption" tone="accent" style={styles.brand}>
            {APP_CONFIG.name}
          </AppText>
        </View>
        {right}
      </View>
      {eyebrow ? (
        <AppText variant="caption" tone="muted" style={styles.eyebrow}>
          {eyebrow}
        </AppText>
      ) : null}
      <AppText variant="display" accessibilityRole="header">
        {title}
      </AppText>
      {subtitle ? (
        <AppText tone="muted" style={styles.subtitle}>
          {subtitle}
        </AppText>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { paddingTop: spacing.md, paddingBottom: spacing.lg, gap: spacing.xs },
  brandRow: {
    minHeight: 40,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  brandIdentity: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
  brandIcon: { width: 36, height: 36, borderRadius: 11 },
  brand: { letterSpacing: 2.2 },
  eyebrow: { color: colors.textSubtle, letterSpacing: 1.4, marginTop: spacing.sm },
  subtitle: { maxWidth: 540 },
});
