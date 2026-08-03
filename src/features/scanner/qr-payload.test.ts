import { describe, expect, it } from 'vitest';

import type { UserCan } from '@/features/inventory/types';

import { encodeCanvaultQrPayload, interpretScannedCode } from './qr-payload';

const can: UserCan = {
  id: 'can-1',
  brandId: 'mtn-montana-colors',
  canLineId: 'mtn-montana-colors:mtn-94',
  customColorName: 'Test Mint',
  customColorCode: 'RV-001',
  customHex: '#58E4C2',
  volumeMl: 400,
  estimatedFillPercent: 100,
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

describe('CANVAULT QR payloads', () => {
  it('round-trips a valid can without private or transactional fields', () => {
    const encoded = encodeCanvaultQrPayload(can);
    const result = interpretScannedCode(encoded, 'qr');

    expect(result.kind).toBe('catalog_match');
    expect(encoded).not.toContain(can.id);
    expect(encoded).not.toContain('purchasePriceCents');
    expect(encoded).not.toContain('acquiredAt');
  });

  it('routes an external barcode to manual review', () => {
    expect(interpretScannedCode('4006381333931', 'ean13')).toMatchObject({
      kind: 'manual_review',
      reason: 'external_code',
    });
  });

  it('rejects malformed CANVAULT codes instead of accepting them as products', () => {
    expect(
      interpretScannedCode(JSON.stringify({ app: 'canvault', version: 1 }), 'qr'),
    ).toMatchObject({ kind: 'invalid', reason: 'malformed_canvault_code' });
  });

  it('rejects a can line that does not belong to the encoded brand', () => {
    const encoded = JSON.stringify({
      app: 'canvault',
      version: 1,
      kind: 'can',
      brandId: 'montana-cans',
      canLineId: 'mtn-montana-colors:mtn-94',
      colorName: 'Mismatch',
      colorCode: null,
      customHex: null,
    });

    expect(interpretScannedCode(encoded, 'qr')).toMatchObject({
      kind: 'invalid',
      reason: 'catalog_mismatch',
    });
  });
});
