import { describe, expect, it } from 'vitest';

import type { UserCan } from '../inventory/types';
import { estimateAreaM2, estimateRemainingVolumeMl } from './calculations';

const baseCan: UserCan = {
  id: 'can-test',
  brandId: 'brand',
  canLineId: 'line',
  customColorName: 'Test',
  customColorCode: null,
  customHex: null,
  volumeMl: 400,
  estimatedFillPercent: 50,
  fillConfidence: 'estimated',
  status: 'in_stock',
  statusBeforeArchive: null,
  purchasePriceCents: null,
  currency: 'EUR',
  acquiredAt: '2026-08-01T00:00:00.000Z',
  archivedAt: null,
  createdAt: '2026-08-01T00:00:00.000Z',
  updatedAt: '2026-08-01T00:00:00.000Z',
};

describe('inventory estimates', () => {
  it('calculates remaining known volume and ignores unknown values', () => {
    expect(
      estimateRemainingVolumeMl([baseCan, { ...baseCan, id: 'unknown', volumeMl: null }]),
    ).toBe(200);
  });

  it('calculates an explicitly estimated area', () => {
    expect(estimateAreaM2(400, 0.01, 0.65)).toBeCloseTo(2.6);
  });

  it('rejects negative area inputs', () => {
    expect(() => estimateAreaM2(-1, 0.01, 0.65)).toThrow();
  });
});
