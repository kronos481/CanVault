import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import type { ComponentProps } from 'react';
import { Platform, StyleSheet, View } from 'react-native';

import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, touch } from '@/theme/tokens';

type IconName = ComponentProps<typeof Ionicons>['name'];

const icons: Record<string, IconName> = {
  dashboard: 'grid-outline',
  inventory: 'albums-outline',
  add: 'add',
  archive: 'time-outline',
  more: 'menu-outline',
};

export default function TabsLayout() {
  const { t } = useI18n();
  return (
    <Tabs
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textSubtle,
        tabBarStyle: styles.tabBar,
        tabBarLabelStyle: styles.label,
        tabBarIcon: ({ color, size, focused }) =>
          route.name === 'add' ? (
            <View style={[styles.addButton, focused && styles.addButtonFocused]}>
              <Ionicons name="add" size={30} color={colors.onAccent} />
            </View>
          ) : (
            <Ionicons name={icons[route.name] ?? 'ellipse-outline'} size={size} color={color} />
          ),
      })}
    >
      <Tabs.Screen name="dashboard" options={{ title: t('nav.dashboard') }} />
      <Tabs.Screen name="inventory" options={{ title: t('nav.inventory') }} />
      <Tabs.Screen name="add" options={{ title: t('nav.add') }} />
      <Tabs.Screen name="archive" options={{ title: t('nav.archive') }} />
      <Tabs.Screen name="more" options={{ title: t('nav.more') }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  tabBar: {
    height: Platform.select({ ios: 84, default: 68 }),
    backgroundColor: colors.backgroundRaised,
    borderTopColor: colors.border,
    paddingTop: 7,
  },
  label: { fontSize: 11, fontWeight: '600' },
  addButton: {
    width: touch.minimum + 8,
    height: touch.minimum + 8,
    marginTop: -18,
    borderRadius: radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.accent,
    borderWidth: 4,
    borderColor: colors.background,
  },
  addButtonFocused: { backgroundColor: colors.accentPressed },
});
