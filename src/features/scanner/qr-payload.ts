import { z } from 'zod';

import { getCatalogBrand, getCatalogCanLine } from '../catalog/catalog.v1';
import type { UserCan } from '../inventory/types';

const MAX_CODE_LENGTH = 4096;

export const canvaultQrPayloadSchema = z
  .object({
    app: z.literal('canvault'),
    version: z.literal(1),
    kind: z.literal('can'),
    brandId: z.string().trim().min(1).max(120),
    canLineId: z.string().trim().min(1).max(180),
    colorName: z.string().trim().min(1).max(120),
    colorCode: z.string().trim().max(80).nullable(),
    customHex: z
      .string()
      .regex(/^#[0-9A-F]{6}$/i)
      .nullable(),
  })
  .strict();

export type CanvaultQrPayload = z.infer<typeof canvaultQrPayloadSchema>;

export type ScanInterpretation =
  | {
      kind: 'catalog_match';
      barcodeType: string;
      rawValue: string;
      payload: CanvaultQrPayload;
    }
  | {
      kind: 'manual_review';
      barcodeType: string;
      rawValue: string;
      reason: 'external_code';
    }
  | {
      kind: 'invalid';
      barcodeType: string;
      rawValue: string;
      reason: 'too_long' | 'malformed_canvault_code' | 'catalog_mismatch';
    };

export function createCanvaultQrPayload(can: UserCan): CanvaultQrPayload {
  return {
    app: 'canvault',
    version: 1,
    kind: 'can',
    brandId: can.brandId,
    canLineId: can.canLineId,
    colorName: can.customColorName,
    colorCode: can.customColorCode,
    customHex: can.customHex,
  };
}

export function encodeCanvaultQrPayload(can: UserCan): string {
  return JSON.stringify(createCanvaultQrPayload(can));
}

export function interpretScannedCode(rawValue: string, barcodeType: string): ScanInterpretation {
  const value = rawValue.trim();

  if (!value || value.length > MAX_CODE_LENGTH) {
    return { kind: 'invalid', barcodeType, rawValue: value, reason: 'too_long' };
  }

  let decoded: unknown;
  try {
    decoded = JSON.parse(value);
  } catch {
    return {
      kind: 'manual_review',
      barcodeType,
      rawValue: value,
      reason: 'external_code',
    };
  }

  const looksLikeCanvault =
    typeof decoded === 'object' &&
    decoded !== null &&
    'app' in decoded &&
    decoded.app === 'canvault';
  const parsed = canvaultQrPayloadSchema.safeParse(decoded);

  if (!parsed.success) {
    return looksLikeCanvault
      ? {
          kind: 'invalid',
          barcodeType,
          rawValue: value,
          reason: 'malformed_canvault_code',
        }
      : {
          kind: 'manual_review',
          barcodeType,
          rawValue: value,
          reason: 'external_code',
        };
  }

  const brand = getCatalogBrand(parsed.data.brandId);
  const canLine = getCatalogCanLine(parsed.data.canLineId);
  if (!brand || !canLine || canLine.brandId !== brand.id) {
    return {
      kind: 'invalid',
      barcodeType,
      rawValue: value,
      reason: 'catalog_mismatch',
    };
  }

  return {
    kind: 'catalog_match',
    barcodeType,
    rawValue: value,
    payload: parsed.data,
  };
}
