export function estimateFillFromWeight(
  currentGrams: number,
  emptyGrams: number,
  fullGrams: number,
): number {
  if (emptyGrams < 0 || fullGrams <= emptyGrams || currentGrams < 0) {
    throw new Error('Weights must define a positive empty-to-full range.');
  }
  const percentage = ((currentGrams - emptyGrams) / (fullGrams - emptyGrams)) * 100;
  return Math.round(Math.min(100, Math.max(0, percentage)));
}
