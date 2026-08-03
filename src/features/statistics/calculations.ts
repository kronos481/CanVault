import type { UserCan } from '@/features/inventory/types';

export function estimateRemainingVolumeMl(cans: readonly UserCan[]): number {
  return Math.round(
    cans.reduce((total, can) => {
      if (can.volumeMl === null || can.estimatedFillPercent === null) return total;
      return total + can.volumeMl * (can.estimatedFillPercent / 100);
    }, 0),
  );
}

export function estimateAreaM2(
  usedPaintMl: number,
  coverageFactor: number,
  efficiencyFactor: number,
): number {
  if ([usedPaintMl, coverageFactor, efficiencyFactor].some((value) => value < 0)) {
    throw new Error('Area estimation inputs must be non-negative.');
  }
  return usedPaintMl * coverageFactor * efficiencyFactor;
}
