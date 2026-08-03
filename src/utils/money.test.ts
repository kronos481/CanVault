import { describe, expect, it } from 'vitest';

import { parsePriceToCents } from './money';

describe('localized price parsing', () => {
  it.each([
    ['4,50', 450],
    ['4.50', 450],
    ['1.234,56 €', 123456],
    ['1,234.56', 123456],
    ['5', 500],
  ])('parses %s', (value, expected) => expect(parsePriceToCents(value)).toBe(expected));

  it('rejects invalid and negative prices', () => {
    expect(parsePriceToCents('spray')).toBeNull();
    expect(parsePriceToCents('-1,00')).toBeNull();
  });
});
