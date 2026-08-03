import { describe, expect, it } from 'vitest';

import { normalizeBarcode, normalizeColorCode, normalizeSearchTerm } from './normalize';

describe('normalization', () => {
  it('normalizes umlauts, punctuation and whitespace for search', () => {
    expect(normalizeSearchTerm('  Türkis-Grün  ')).toBe('turkis grun');
  });

  it('keeps only plausible barcode digits', () => {
    expect(normalizeBarcode('40 12345-67890 1')).toBe('4012345678901');
    expect(normalizeBarcode('1234')).toBeNull();
  });

  it('normalizes color codes without merging semantic separators', () => {
    expect(normalizeColorCode(' rv  30-a ')).toBe('RV 30-A');
  });
});
