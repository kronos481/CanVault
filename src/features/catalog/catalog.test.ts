import { describe, expect, it } from 'vitest';

import { catalogBrands, catalogCanLines, catalogColors, getCanLinesForBrand } from './catalog.v1';

describe('versioned seed catalog', () => {
  it('contains every requested brand and can line', () => {
    expect(catalogBrands).toHaveLength(15);
    expect(catalogCanLines).toHaveLength(34);
  });

  it('keeps MTN / Montana Colors separate from Montana Cans', () => {
    const mtn = catalogBrands.find((brand) => brand.slug === 'mtn-montana-colors');
    const montana = catalogBrands.find((brand) => brand.slug === 'montana-cans');
    expect(mtn?.id).not.toBe(montana?.id);
    expect(getCanLinesForBrand(mtn?.id ?? '').map((line) => line.displayName)).toContain('MTN 94');
    expect(getCanLinesForBrand(montana?.id ?? '').map((line) => line.displayName)).toContain(
      'Montana Black',
    );
  });

  it('does not invent unverified color data', () => {
    expect(catalogColors).toEqual([]);
    expect(catalogCanLines.every((line) => line.verificationStatus === 'unverified')).toBe(true);
  });
});
