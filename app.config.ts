import type { ExpoConfig } from 'expo/config';

import brand from './app-brand.json';

const config: ExpoConfig = {
  name: process.env.EXPO_PUBLIC_APP_NAME ?? brand.displayName,
  slug: brand.slug,
  scheme: brand.scheme,
  version: '0.1.0',
  orientation: 'portrait',
  userInterfaceStyle: 'dark',
  icon: './assets/brand/canvault-icon.png',
  ios: { supportsTablet: true, icon: './assets/expo.icon' },
  android: {
    package: 'com.canvault.app',
    versionCode: 1,
    icon: './assets/brand/canvault-icon.png',
    backgroundColor: '#090B0E',
    userInterfaceStyle: 'dark',
    permissions: ['android.permission.CAMERA'],
    blockedPermissions: ['android.permission.RECORD_AUDIO'],
    adaptiveIcon: {
      backgroundColor: '#090B0E',
      foregroundImage: './assets/brand/canvault-icon.png',
    },
    predictiveBackGestureEnabled: true,
  },
  web: { output: 'static', favicon: './assets/brand/canvault-icon.png' },
  plugins: [
    'expo-router',
    'expo-secure-store',
    'expo-sharing',
    [
      'expo-camera',
      {
        cameraPermission: '$(PRODUCT_NAME) benötigt die Kamera nur zum Scannen von Produktcodes.',
        recordAudioAndroid: false,
        barcodeScannerEnabled: true,
      },
    ],
    [
      'expo-splash-screen',
      {
        backgroundColor: '#090B0E',
        image: './assets/brand/canvault-icon.png',
        imageWidth: 180,
      },
    ],
  ],
  experiments: { typedRoutes: true, reactCompiler: true },
  extra: {
    catalogVersion: '2026.08.01-v1',
    ...(process.env.EAS_PROJECT_ID ? { eas: { projectId: process.env.EAS_PROJECT_ID } } : {}),
  },
};

export default config;
