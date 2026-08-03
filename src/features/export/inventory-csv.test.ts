import { describe, expect, it } from 'vitest';

import type { UserCan } from '@/features/inventory/types';

import { createInventoryCsv } from './inventory-csv';

const can: UserCan = {
  id: 'can-1',
  brandId: 'mtn-montana-colors',
  canLineId: 'mtn-montana-colors:mtn-94',
  customColorName: 'Blue, "Special"',
  customColorCode: 'RV-30',
  customHex: '#1122AA',
  volumeMl: 400,
  estimatedFillPercent: 75,
  fillConfidence: 'estimated',
  status: 'in_stock',
  statusBeforeArchive: null,
  purchasePriceCents: 450,
  currency: 'EUR',
  acquiredAt: '2026-08-01T10:00:00.000Z',
  archivedAt: null,
  createdAt: '2026-08-01T10:00:00.000Z',
  updatedAt: '2026-08-01T10:00:00.000Z',
};

describe('inventory CSV export', () => {
  it('exports stable headers and safely escapes spreadsheet cells', () => {
    const csv = createInventoryCsv([can]);

    expect(csv).toContain('brand,can_line,color_name');
    expect(csv).toContain('MTN / Montana Colors');
    expect(csv).toContain('"Blue, ""Special"""');
  });

  it('exports an empty inventory as headers only', () => {
    expect(createInventoryCsv([]).split('\r\n')).toHaveLength(1);
  });

  it('neutralizes spreadsheet formulas from user-entered fields', () => {
    const csv = createInventoryCsv([{ ...can, customColorName: '=HYPERLINK("bad")' }]);

    expect(csv).toContain('"\'=HYPERLINK(""bad"")"');
  });
});
