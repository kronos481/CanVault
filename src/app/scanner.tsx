import { Ionicons } from '@expo/vector-icons';
import type { BarcodeScanningResult, BarcodeType } from 'expo-camera';
import { CameraView, useCameraPermissions } from 'expo-camera';
import * as Haptics from 'expo-haptics';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { Linking, StyleSheet, View } from 'react-native';
import Animated, {
  Easing,
  interpolate,
  useAnimatedStyle,
  useReducedMotion,
  useSharedValue,
  withRepeat,
  withTiming,
} from 'react-native-reanimated';

import { CanPreview } from '@/components/can/can-preview';
import { LoadingState } from '@/components/feedback/loading-state';
import { Screen } from '@/components/layout/screen';
import { AnimatedPressable } from '@/components/ui/animated-pressable';
import { AppText } from '@/components/ui/app-text';
import { Button } from '@/components/ui/button';
import { APP_CONFIG } from '@/config/app';
import { getCatalogBrand, getCatalogCanLine } from '@/features/catalog/catalog.v1';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { interpretScannedCode, type ScanInterpretation } from '@/features/scanner/qr-payload';
import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, shadows, spacing, touch } from '@/theme/tokens';

const BARCODE_TYPES: BarcodeType[] = [
  'qr',
  'ean13',
  'ean8',
  'upc_a',
  'upc_e',
  'code128',
  'code39',
  'code93',
  'itf14',
  'datamatrix',
  'pdf417',
  'aztec',
];

export default function ScannerScreen() {
  const [permission, requestPermission] = useCameraPermissions();
  const [result, setResult] = useState<ScanInterpretation | null>(null);
  const [torchEnabled, setTorchEnabled] = useState(false);
  const [isFocused, setIsFocused] = useState(true);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const addCans = useInventoryStore((state) => state.addCans);
  const { t } = useI18n();

  useFocusEffect(
    useCallback(() => {
      setIsFocused(true);
      return () => setIsFocused(false);
    }, []),
  );

  const onBarcodeScanned = useCallback((scan: BarcodeScanningResult) => {
    setResult((current) => {
      if (current) return current;
      void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      return interpretScannedCode(scan.data, scan.type);
    });
  }, []);

  const saveCatalogMatch = () => {
    if (result?.kind !== 'catalog_match') return;
    const ids = addCans({
      brandId: result.payload.brandId,
      canLineId: result.payload.canLineId,
      colorName: result.payload.colorName,
      colorCode: result.payload.colorCode ?? undefined,
      customHex: result.payload.customHex ?? undefined,
      quantity: 1,
      purchasePriceCents: null,
      currency: APP_CONFIG.defaultCurrency,
    });
    void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    const id = ids[0];
    if (id) router.replace(`/can/${id}`);
  };

  const scanAgain = () => {
    setCameraError(null);
    setResult(null);
  };

  if (!permission) return <LoadingState />;

  if (!permission.granted) {
    return (
      <Screen>
        <View style={styles.permissionState}>
          <View style={styles.permissionIcon}>
            <Ionicons name="camera-outline" size={36} color={colors.accent} />
          </View>
          <AppText variant="title" accessibilityRole="header" style={styles.centeredText}>
            {t('scanner.permissionTitle')}
          </AppText>
          <AppText tone="muted" style={styles.centeredText}>
            {t('scanner.permissionBody')}
          </AppText>
          <Button
            label={permission.canAskAgain ? t('scanner.allowCamera') : t('scanner.openSettings')}
            icon={permission.canAskAgain ? 'camera-outline' : 'settings-outline'}
            onPress={() => {
              if (permission.canAskAgain) void requestPermission();
              else void Linking.openSettings();
            }}
          />
          <Button label={t('common.cancel')} variant="ghost" onPress={() => router.back()} />
        </View>
      </Screen>
    );
  }

  return (
    <Screen padded={false}>
      <View style={styles.container}>
        <View style={styles.toolbar}>
          <IconButton label={t('common.back')} icon="arrow-back" onPress={() => router.back()} />
          <View style={styles.toolbarText}>
            <AppText variant="heading" accessibilityRole="header">
              {t('scanner.title')}
            </AppText>
            <AppText variant="caption" tone="muted">
              {result ? t('scanner.paused') : t('scanner.live')}
            </AppText>
          </View>
          <IconButton
            label={torchEnabled ? t('scanner.torchOff') : t('scanner.torchOn')}
            icon={torchEnabled ? 'flash' : 'flash-outline'}
            selected={torchEnabled}
            onPress={() => setTorchEnabled((value) => !value)}
          />
        </View>

        {!result ? (
          <View style={styles.cameraShell}>
            {isFocused && !cameraError ? (
              <CameraView
                style={StyleSheet.absoluteFill}
                facing="back"
                enableTorch={torchEnabled}
                barcodeScannerSettings={{ barcodeTypes: BARCODE_TYPES }}
                onBarcodeScanned={onBarcodeScanned}
                onMountError={(error) => setCameraError(error.message)}
              />
            ) : null}
            {cameraError ? (
              <View style={styles.cameraError}>
                <Ionicons name="warning-outline" size={34} color={colors.warning} />
                <AppText variant="heading">{t('scanner.cameraErrorTitle')}</AppText>
                <AppText tone="muted" style={styles.centeredText}>
                  {cameraError}
                </AppText>
                <Button label={t('common.retry')} onPress={scanAgain} />
              </View>
            ) : (
              <ScannerFrame />
            )}
          </View>
        ) : (
          <ScanResultCard result={result} onSave={saveCatalogMatch} onRetry={scanAgain} />
        )}

        <View style={styles.guidance}>
          <Ionicons name="scan-outline" size={touch.icon} color={colors.accent} />
          <View style={styles.guidanceText}>
            <AppText variant="label">{t('scanner.guidanceTitle')}</AppText>
            <AppText variant="caption" tone="muted">
              {t('scanner.guidanceBody')}
            </AppText>
          </View>
        </View>
      </View>
    </Screen>
  );
}

function ScannerFrame() {
  const progress = useSharedValue(0);
  const reduceMotion = useReducedMotion();
  const animatedLine = useAnimatedStyle(() => ({
    opacity: interpolate(progress.get(), [0, 0.5, 1], [0.45, 1, 0.45]),
    transform: [{ translateY: interpolate(progress.get(), [0, 1], [-112, 112]) }],
  }));

  useEffect(() => {
    if (reduceMotion) {
      progress.set(0.5);
      return;
    }
    progress.set(
      withRepeat(withTiming(1, { duration: 1800, easing: Easing.inOut(Easing.quad) }), -1, true),
    );
  }, [progress, reduceMotion]);

  return (
    <View style={styles.cameraOverlay} pointerEvents="none">
      <View style={styles.scanFrame}>
        <View style={[styles.corner, styles.cornerTopLeft]} />
        <View style={[styles.corner, styles.cornerTopRight]} />
        <View style={[styles.corner, styles.cornerBottomLeft]} />
        <View style={[styles.corner, styles.cornerBottomRight]} />
        <Animated.View style={[styles.scanLine, animatedLine]} />
      </View>
      <View style={styles.scanPill}>
        <View style={styles.liveDot} />
        <AppText variant="caption">{useI18n().t('scanner.alignCode')}</AppText>
      </View>
    </View>
  );
}

function ScanResultCard({
  result,
  onSave,
  onRetry,
}: {
  result: ScanInterpretation;
  onSave: () => void;
  onRetry: () => void;
}) {
  const { t } = useI18n();

  if (result.kind === 'catalog_match') {
    const brand = getCatalogBrand(result.payload.brandId);
    const line = getCatalogCanLine(result.payload.canLineId);
    return (
      <View style={styles.resultCard}>
        <View style={styles.resultVisual}>
          <CanPreview
            accentColor={result.payload.customHex}
            variantKey={result.payload.canLineId}
            compact
          />
        </View>
        <View style={styles.verifiedPill}>
          <Ionicons name="checkmark-circle" size={18} color={colors.success} />
          <AppText variant="caption" tone="accent">
            {t('scanner.canvaultCode')}
          </AppText>
        </View>
        <AppText variant="title" accessibilityRole="header">
          {result.payload.colorName}
        </AppText>
        <AppText tone="muted">
          {brand?.displayName} · {line?.displayName}
        </AppText>
        <View style={styles.resultMeta}>
          <AppText variant="caption" tone="muted">
            {t('add.colorCode')}
          </AppText>
          <AppText variant="label">{result.payload.colorCode || t('common.notAvailable')}</AppText>
        </View>
        <AppText variant="caption" tone="muted">
          {t('scanner.confirmHint')}
        </AppText>
        <Button label={t('scanner.saveCan')} icon="add-circle-outline" onPress={onSave} />
        <Button label={t('scanner.scanAgain')} variant="ghost" onPress={onRetry} />
      </View>
    );
  }

  const invalid = result.kind === 'invalid';
  return (
    <View style={styles.resultCard}>
      <View style={[styles.resultStatusIcon, invalid && styles.resultStatusIconDanger]}>
        <Ionicons
          name={invalid ? 'close-circle-outline' : 'help-circle-outline'}
          size={36}
          color={invalid ? colors.danger : colors.warning}
        />
      </View>
      <AppText variant="title" accessibilityRole="header">
        {invalid ? t('scanner.invalidTitle') : t('scanner.unknownTitle')}
      </AppText>
      <AppText tone="muted">
        {invalid ? t('scanner.invalidBody') : t('scanner.unknownBody')}
      </AppText>
      <View style={styles.rawCode}>
        <AppText variant="caption" tone="muted">
          {result.barcodeType.toLocaleUpperCase()}
        </AppText>
        <AppText variant="caption" numberOfLines={3}>
          {result.rawValue}
        </AppText>
      </View>
      {!invalid ? (
        <Button
          label={t('scanner.addManual')}
          icon="create-outline"
          onPress={() => router.replace('/add')}
        />
      ) : null}
      <Button label={t('scanner.scanAgain')} variant="ghost" onPress={onRetry} />
    </View>
  );
}

function IconButton({
  label,
  icon,
  selected = false,
  onPress,
}: {
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  selected?: boolean;
  onPress: () => void;
}) {
  return (
    <AnimatedPressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[styles.iconButton, selected && styles.iconButtonSelected]}
    >
      <Ionicons name={icon} size={touch.icon} color={selected ? colors.onAccent : colors.text} />
    </AnimatedPressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: spacing.md, paddingBottom: spacing.md, gap: spacing.md },
  toolbar: {
    minHeight: 64,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  toolbarText: { flex: 1, alignItems: 'center' },
  iconButton: {
    width: touch.minimum,
    height: touch.minimum,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  iconButtonSelected: { backgroundColor: colors.accent, borderColor: colors.accent },
  cameraShell: {
    flex: 1,
    minHeight: 420,
    overflow: 'hidden',
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    backgroundColor: colors.backgroundRaised,
    ...shadows.card,
  },
  cameraOverlay: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.12)',
  },
  scanFrame: {
    width: 264,
    height: 264,
    alignItems: 'center',
    justifyContent: 'center',
  },
  corner: { position: 'absolute', width: 42, height: 42, borderColor: colors.accent },
  cornerTopLeft: { top: 0, left: 0, borderTopWidth: 4, borderLeftWidth: 4 },
  cornerTopRight: { top: 0, right: 0, borderTopWidth: 4, borderRightWidth: 4 },
  cornerBottomLeft: { bottom: 0, left: 0, borderBottomWidth: 4, borderLeftWidth: 4 },
  cornerBottomRight: { bottom: 0, right: 0, borderBottomWidth: 4, borderRightWidth: 4 },
  scanLine: {
    width: 224,
    height: 2,
    borderRadius: radius.pill,
    backgroundColor: colors.accent,
    shadowColor: colors.accent,
    shadowOpacity: 0.8,
    shadowRadius: 8,
  },
  scanPill: {
    position: 'absolute',
    bottom: spacing.lg,
    minHeight: 38,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    paddingHorizontal: spacing.sm,
    borderRadius: radius.pill,
    backgroundColor: 'rgba(9,11,14,0.86)',
  },
  liveDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.success },
  cameraError: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.md,
    padding: spacing.lg,
  },
  centeredText: { textAlign: 'center' },
  guidance: {
    minHeight: 72,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.md,
    borderRadius: radius.md,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  guidanceText: { flex: 1, gap: spacing.xxs },
  permissionState: {
    flex: 1,
    justifyContent: 'center',
    gap: spacing.md,
    paddingHorizontal: spacing.lg,
  },
  permissionIcon: {
    width: 72,
    height: 72,
    alignSelf: 'center',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.lg,
    backgroundColor: 'rgba(88,228,194,0.10)',
    borderWidth: 1,
    borderColor: colors.accent,
  },
  resultCard: {
    flex: 1,
    minHeight: 420,
    justifyContent: 'center',
    gap: spacing.md,
    padding: spacing.lg,
    borderRadius: radius.lg,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.borderStrong,
    ...shadows.card,
  },
  resultVisual: { height: 148 },
  verifiedPill: {
    alignSelf: 'flex-start',
    minHeight: 32,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xxs,
    paddingHorizontal: spacing.sm,
    borderRadius: radius.pill,
    backgroundColor: 'rgba(101,214,155,0.10)',
  },
  resultMeta: {
    minHeight: touch.minimum,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  resultStatusIcon: {
    width: 64,
    height: 64,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.lg,
    backgroundColor: 'rgba(245,191,96,0.10)',
  },
  resultStatusIconDanger: { backgroundColor: 'rgba(255,123,123,0.10)' },
  rawCode: {
    gap: spacing.xs,
    padding: spacing.sm,
    borderRadius: radius.sm,
    backgroundColor: colors.backgroundRaised,
    borderWidth: 1,
    borderColor: colors.border,
  },
});
