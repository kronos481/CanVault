import { APP_CONFIG } from '../../config/app';

import type { CatalogBrand, CatalogCanLine, CatalogColor } from './types';

const brandDefinitions = [
  [
    'mtn-montana-colors',
    'MTN / Montana Colors',
    ['MTN 94', 'MTN Hardcore', 'MTN Vice', 'MTN Water Based 400', 'MTN Mega', 'MTN Alien'],
  ],
  [
    'montana-cans',
    'Montana Cans',
    [
      'Montana Black',
      'Montana Gold',
      'Montana White',
      'Montana Tarblack',
      'Montana Blackout Tarblack',
      'Montana Ultra Wide',
    ],
  ],
  [
    'molotow-belton',
    'Molotow / Belton',
    ['Molotow Premium', 'Molotow Burner', 'Molotow CoversAll'],
  ],
  ['loop-colors', 'Loop Colors', ['Loop 400 ml', 'Loop Asphalt']],
  ['flame', 'Flame', ['Flame Blue', 'Flame Orange']],
  ['kobra', 'Kobra', ['Kobra HP', 'Kobra LP']],
  ['ironlak', 'Ironlak', ['Ironlak 400 ml', 'Sugar Artists Acrylic']],
  ['nbq', 'NBQ', ['NBQ Fast', 'NBQ Slow']],
  ['dope', 'Dope', ['Dope Action', 'Dope Classic']],
  ['dang', 'Dang', ['Dang Prime', 'Dang Hi-Flow']],
  ['clash', 'Clash', ['Clash']],
  ['beat', 'Beat', ['Beat']],
  ['scribo', 'Scribo', ['Scribo']],
  ['double-a', 'Double A', ['Double A']],
  ['krink', 'Krink', ['Krink K-750']],
] as const;

function toSlug(value: string): string {
  return value
    .toLowerCase()
    .replaceAll(/[^a-z0-9]+/g, '-')
    .replaceAll(/^-|-$/g, '');
}

export const CATALOG_VERSION = APP_CONFIG.catalogVersion;

export const catalogBrands: CatalogBrand[] = brandDefinitions.map(([slug, displayName]) => ({
  id: slug,
  slug,
  displayName,
  legalName: null,
  verificationStatus: 'unverified',
}));

export const catalogCanLines: CatalogCanLine[] = brandDefinitions.flatMap(
  ([brandId, , lineNames]) =>
    lineNames.map((displayName) => ({
      id: `${brandId}:${toSlug(displayName)}`,
      brandId,
      slug: toSlug(displayName),
      displayName,
      defaultVolumeMl: null,
      pressureType: null,
      paintType: null,
      finish: null,
      verificationStatus: 'unverified' as const,
    })),
);

// No complete manufacturer palette has been verified. Colors are deliberately imported separately.
export const catalogColors: CatalogColor[] = [];

export function getCatalogBrand(brandId: string): CatalogBrand | undefined {
  return catalogBrands.find((brand) => brand.id === brandId);
}

export function getCatalogCanLine(canLineId: string): CatalogCanLine | undefined {
  return catalogCanLines.find((line) => line.id === canLineId);
}

export function getCanLinesForBrand(brandId: string): CatalogCanLine[] {
  return catalogCanLines.filter((line) => line.brandId === brandId);
}
