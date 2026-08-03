import { zodResolver } from '@hookform/resolvers/zod';
import * as Haptics from 'expo-haptics';
import { router } from 'expo-router';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import Animated, { FadeInRight, ReduceMotion } from 'react-native-reanimated';

import { InputField } from '@/components/forms/input-field';
import { Screen } from '@/components/layout/screen';
import { AppText } from '@/components/ui/app-text';
import { Button } from '@/components/ui/button';
import { APP_CONFIG } from '@/config/app';
import {
  catalogBrands,
  getCanLinesForBrand,
  getCatalogBrand,
  getCatalogCanLine,
} from '@/features/catalog/catalog.v1';
import { addCanSchema, type AddCanFormValues } from '@/features/inventory/add-can-schema';
import { useInventoryStore } from '@/features/inventory/inventory-store';
import { useI18n } from '@/i18n/use-i18n';
import { colors, radius, spacing, touch } from '@/theme/tokens';
import { parsePriceToCents } from '@/utils/money';
import { normalizeColorCode } from '@/utils/normalize';

const TOTAL_STEPS = 5;

export default function AddCanScreen() {
  const addCans = useInventoryStore((state) => state.addCans);
  const { t } = useI18n();
  const {
    control,
    handleSubmit,
    setValue,
    trigger,
    formState: { errors, isSubmitting },
  } = useForm<AddCanFormValues>({
    resolver: zodResolver(addCanSchema),
    defaultValues: {
      brandId: '',
      canLineId: '',
      colorName: '',
      colorCode: '',
      customHex: '',
      quantity: 1,
      purchasePrice: '',
    },
    mode: 'onBlur',
  });
  const [step, setStep] = useState(1);
  const values = {
    brandId: useWatch({ control, name: 'brandId' }),
    canLineId: useWatch({ control, name: 'canLineId' }),
    colorName: useWatch({ control, name: 'colorName' }),
    colorCode: useWatch({ control, name: 'colorCode' }),
    customHex: useWatch({ control, name: 'customHex' }),
    quantity: useWatch({ control, name: 'quantity' }),
    purchasePrice: useWatch({ control, name: 'purchasePrice' }),
  };
  const lines = getCanLinesForBrand(values.brandId);

  const goBack = () => setStep((current) => Math.max(1, current - 1));
  const continueFromColor = async () => {
    if (await trigger(['colorName', 'colorCode', 'customHex'])) setStep(4);
  };
  const continueFromDetails = async () => {
    if (await trigger(['quantity', 'purchasePrice'])) setStep(5);
  };
  const onSubmit = (form: AddCanFormValues) => {
    const ids = addCans({
      brandId: form.brandId,
      canLineId: form.canLineId,
      colorName: form.colorName,
      colorCode: form.colorCode ? normalizeColorCode(form.colorCode) : undefined,
      customHex: form.customHex || undefined,
      quantity: form.quantity,
      purchasePriceCents: parsePriceToCents(form.purchasePrice),
      currency: APP_CONFIG.defaultCurrency,
    });
    void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    const firstId = ids[0];
    if (firstId) router.replace(`/can/${firstId}`);
  };

  return (
    <Screen padded={false}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={styles.scroll}>
          <View style={styles.header}>
            <AppText variant="caption" tone="accent">
              {t('add.step', { current: step, total: TOTAL_STEPS })}
            </AppText>
            <View style={styles.progress}>
              {Array.from({ length: TOTAL_STEPS }, (_, index) => (
                <View
                  key={index}
                  style={[styles.progressSegment, index < step && styles.progressActive]}
                />
              ))}
            </View>
            <AppText variant="display" accessibilityRole="header">
              {t('add.title')}
            </AppText>
          </View>

          {step === 1 ? (
            <View style={styles.scanEntry}>
              <View style={styles.scanEntryCopy}>
                <AppText variant="heading">{t('add.scanTitle')}</AppText>
                <AppText variant="caption" tone="muted">
                  {t('add.scanBody')}
                </AppText>
              </View>
              <Button
                label={t('dashboard.scan')}
                icon="scan-outline"
                onPress={() => router.push('/scanner')}
              />
            </View>
          ) : null}

          <Animated.View
            key={step}
            entering={FadeInRight.duration(220).reduceMotion(ReduceMotion.System)}
          >
            {step === 1 ? (
              <SelectionStep title={t('add.brandTitle')} hint={t('add.brandHint')}>
                {catalogBrands.map((brand) => (
                  <SelectionRow
                    key={brand.id}
                    label={brand.displayName}
                    meta={brand.verificationStatus}
                    selected={values.brandId === brand.id}
                    onPress={() => {
                      setValue('brandId', brand.id, { shouldValidate: true });
                      setValue('canLineId', '');
                      setStep(2);
                    }}
                  />
                ))}
              </SelectionStep>
            ) : null}

            {step === 2 ? (
              <SelectionStep title={t('add.lineTitle')}>
                {lines.length ? (
                  lines.map((line) => (
                    <SelectionRow
                      key={line.id}
                      label={line.displayName}
                      meta={line.verificationStatus}
                      selected={values.canLineId === line.id}
                      onPress={() => {
                        setValue('canLineId', line.id, { shouldValidate: true });
                        setStep(3);
                      }}
                    />
                  ))
                ) : (
                  <AppText tone="muted">{t('add.noLines')}</AppText>
                )}
                <Button
                  label={t('common.back')}
                  onPress={goBack}
                  variant="ghost"
                  icon="arrow-back"
                />
              </SelectionStep>
            ) : null}

            {step === 3 ? (
              <FormStep title={t('add.colorTitle')} hint={t('add.colorHint')}>
                <Controller
                  control={control}
                  name="colorName"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <InputField
                      label={t('add.colorName')}
                      placeholder={t('add.colorNamePlaceholder')}
                      value={value}
                      onChangeText={onChange}
                      onBlur={onBlur}
                      error={errors.colorName ? t('add.errorRequired') : undefined}
                      autoCapitalize="words"
                    />
                  )}
                />
                <Controller
                  control={control}
                  name="colorCode"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <InputField
                      label={t('add.colorCode')}
                      placeholder={t('add.colorCodePlaceholder')}
                      value={value}
                      onChangeText={onChange}
                      onBlur={onBlur}
                      autoCapitalize="characters"
                    />
                  )}
                />
                <Controller
                  control={control}
                  name="customHex"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <InputField
                      label={t('add.hex')}
                      placeholder={t('add.hexPlaceholder')}
                      value={value}
                      onChangeText={onChange}
                      onBlur={onBlur}
                      error={errors.customHex ? t('add.errorHex') : undefined}
                      hint={t('add.hexHint')}
                      autoCapitalize="characters"
                      maxLength={7}
                    />
                  )}
                />
                <View style={styles.actions}>
                  <Button label={t('common.back')} onPress={goBack} variant="ghost" />
                  <Button
                    label={t('common.continue')}
                    onPress={() => void continueFromColor()}
                    icon="arrow-forward"
                  />
                </View>
              </FormStep>
            ) : null}

            {step === 4 ? (
              <FormStep title={t('add.detailsTitle')}>
                <AppText variant="label">{t('add.quantity')}</AppText>
                <Controller
                  control={control}
                  name="quantity"
                  render={({ field: { value, onChange } }) => (
                    <View style={styles.stepper}>
                      <Button
                        label="−"
                        accessibilityHint={t('add.quantity')}
                        onPress={() => onChange(Math.max(1, value - 1))}
                        variant="secondary"
                      />
                      <AppText variant="title" accessibilityLiveRegion="polite">
                        {value}
                      </AppText>
                      <Button
                        label="+"
                        accessibilityHint={t('add.quantity')}
                        onPress={() => onChange(Math.min(99, value + 1))}
                        variant="secondary"
                      />
                    </View>
                  )}
                />
                {errors.quantity ? (
                  <AppText tone="danger" variant="caption">
                    {t('add.errorQuantity')}
                  </AppText>
                ) : null}
                <Controller
                  control={control}
                  name="purchasePrice"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <InputField
                      label={t('add.price')}
                      placeholder={t('add.pricePlaceholder')}
                      value={value}
                      onChangeText={onChange}
                      onBlur={onBlur}
                      error={errors.purchasePrice ? t('add.errorPrice') : undefined}
                      keyboardType="decimal-pad"
                    />
                  )}
                />
                <View style={styles.actions}>
                  <Button label={t('common.back')} onPress={goBack} variant="ghost" />
                  <Button
                    label={t('common.continue')}
                    onPress={() => void continueFromDetails()}
                    icon="arrow-forward"
                  />
                </View>
              </FormStep>
            ) : null}

            {step === 5 ? (
              <FormStep title={t('add.summaryTitle')} hint={t('add.summaryHint')}>
                <SummaryRow
                  label={t('add.brandTitle')}
                  value={getCatalogBrand(values.brandId)?.displayName ?? '—'}
                />
                <SummaryRow
                  label={t('add.lineTitle')}
                  value={getCatalogCanLine(values.canLineId)?.displayName ?? '—'}
                />
                <SummaryRow label={t('add.colorName')} value={values.colorName} />
                <SummaryRow label={t('add.colorCode')} value={values.colorCode || '—'} />
                <SummaryRow label={t('add.quantity')} value={String(values.quantity)} />
                <SummaryRow label={t('add.price')} value={values.purchasePrice || '—'} />
                <View style={styles.actions}>
                  <Button label={t('common.back')} onPress={goBack} variant="ghost" />
                  <Button
                    label={t('add.save', { count: values.quantity })}
                    onPress={() => void handleSubmit(onSubmit)()}
                    loading={isSubmitting}
                    icon="checkmark"
                  />
                </View>
              </FormStep>
            ) : null}
          </Animated.View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}

function SelectionStep({
  title,
  hint,
  children,
}: React.PropsWithChildren<{ title: string; hint?: string }>) {
  return (
    <View style={styles.step}>
      <AppText variant="title" accessibilityRole="header">
        {title}
      </AppText>
      {hint ? <AppText tone="muted">{hint}</AppText> : null}
      <View style={styles.selectionList}>{children}</View>
    </View>
  );
}

function FormStep({
  title,
  hint,
  children,
}: React.PropsWithChildren<{ title: string; hint?: string }>) {
  return (
    <View style={styles.step}>
      <AppText variant="title" accessibilityRole="header">
        {title}
      </AppText>
      {hint ? <AppText tone="muted">{hint}</AppText> : null}
      {children}
    </View>
  );
}

function SelectionRow({
  label,
  meta,
  selected,
  onPress,
}: {
  label: string;
  meta: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="radio"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={({ pressed }) => [
        styles.selectionRow,
        selected && styles.selectionSelected,
        pressed && styles.pressed,
      ]}
    >
      <View style={styles.brandMark}>
        <AppText variant="label" tone="accent">
          {label.slice(0, 2).toLocaleUpperCase()}
        </AppText>
      </View>
      <View style={styles.selectionText}>
        <AppText variant="heading">{label}</AppText>
        <AppText variant="caption" tone="muted">
          {meta}
        </AppText>
      </View>
    </Pressable>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.summaryRow}>
      <AppText tone="muted">{label}</AppText>
      <AppText variant="label" style={styles.summaryValue}>
        {value}
      </AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  scroll: { paddingHorizontal: spacing.md, paddingBottom: 140 },
  header: { paddingTop: spacing.md, paddingBottom: spacing.lg, gap: spacing.sm },
  scanEntry: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.md,
    marginBottom: spacing.lg,
    borderRadius: radius.md,
    backgroundColor: 'rgba(88,228,194,0.08)',
    borderWidth: 1,
    borderColor: colors.accent,
  },
  scanEntryCopy: { minWidth: 180, flex: 1, gap: spacing.xxs },
  progress: { flexDirection: 'row', gap: spacing.xs },
  progressSegment: {
    flex: 1,
    height: 4,
    borderRadius: radius.pill,
    backgroundColor: colors.border,
  },
  progressActive: { backgroundColor: colors.accent },
  step: { gap: spacing.md },
  selectionList: { gap: spacing.xs },
  selectionRow: {
    minHeight: 64,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.sm,
    borderRadius: radius.md,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  selectionSelected: { borderColor: colors.accent },
  pressed: { opacity: 0.76 },
  brandMark: {
    width: touch.minimum,
    height: touch.minimum,
    borderRadius: radius.sm,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.backgroundRaised,
    borderWidth: 1,
    borderColor: colors.borderStrong,
  },
  selectionText: { flex: 1, gap: spacing.xxs },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: spacing.xs,
    marginTop: spacing.md,
  },
  stepper: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.lg,
    alignSelf: 'flex-start',
  },
  summaryRow: {
    minHeight: touch.minimum,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  summaryValue: { flex: 1, textAlign: 'right' },
});
