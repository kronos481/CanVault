import { DarkTheme, Stack, ThemeProvider } from 'expo-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { StyleSheet } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { StatusBar } from 'expo-status-bar';

import { LoadingState } from '@/components/feedback/loading-state';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { flushSyncQueue } from '@/features/sync/sync-service';
import { colors } from '@/theme/tokens';

void SplashScreen.preventAutoHideAsync();

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
    mutations: { retry: 1 },
  },
});

const navigationTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: colors.accent,
    background: colors.background,
    card: colors.surface,
    text: colors.text,
    border: colors.border,
    notification: colors.warning,
  },
};

export default function RootLayout() {
  const hydrated = useInventoryStore((state) => state.hydrated);

  useEffect(() => {
    if (!hydrated) return;
    void SplashScreen.hideAsync();
    void flushSyncQueue();
  }, [hydrated]);

  return (
    <GestureHandlerRootView style={styles.root}>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider value={navigationTheme}>
          <StatusBar style="light" />
          {hydrated ? (
            <Stack
              screenOptions={{
                contentStyle: { backgroundColor: colors.background },
                headerStyle: { backgroundColor: colors.backgroundRaised },
                headerTintColor: colors.text,
                headerShadowVisible: false,
              }}
            >
              <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
              <Stack.Screen name="can/[id]" options={{ headerShown: false }} />
              <Stack.Screen
                name="scanner"
                options={{ headerShown: false, presentation: 'fullScreenModal' }}
              />
              <Stack.Screen
                name="qr/[id]"
                options={{ headerShown: false, presentation: 'modal' }}
              />
            </Stack>
          ) : (
            <LoadingState />
          )}
        </ThemeProvider>
      </QueryClientProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({ root: { flex: 1, backgroundColor: colors.background } });
