import { getCatalogBrand, getCatalogCanLine } from '../catalog/catalog.v1';
import type { UserCan } from '../inventory/types';

const HEADERS = [
  'id',
  'brand',
  'can_line',
  'color_name',
  'color_code',
  'display_hex',
  'volume_ml',
  'fill_percent_estimated',
  'status',
  'purchase_price_cents',
  'currency',
  'acquired_at',
  'archived_at',
] as const;

function escapeCsvCell(value: string | number | null): string {
  if (value === null) return '';
  const raw = String(value);
  const text = /^[=+\-@]/.test(raw) ? `'${raw}` : raw;
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}

export function createInventoryCsv(cans: readonly UserCan[]): string {
  const rows = cans.map((can) =>
    [
      can.id,
      getCatalogBrand(can.brandId)?.displayName ?? can.brandId,
      getCatalogCanLine(can.canLineId)?.displayName ?? can.canLineId,
      can.customColorName,
      can.customColorCode,
      can.customHex,
      can.volumeMl,
      can.estimatedFillPercent,
      can.status,
      can.purchasePriceCents,
      can.currency,
      can.acquiredAt,
      can.archivedAt,
    ]
      .map(escapeCsvCell)
      .join(','),
  );

  return [HEADERS.join(','), ...rows].join('\r\n');
}
