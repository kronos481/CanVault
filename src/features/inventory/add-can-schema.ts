import { z } from 'zod';

import { parsePriceToCents } from '@/utils/money';

export const addCanSchema = z.object({
  brandId: z.string().min(1),
  canLineId: z.string().min(1),
  colorName: z.string().trim().min(1),
  colorCode: z.string().trim().max(64),
  customHex: z
    .string()
    .trim()
    .refine((value) => value === '' || /^#[0-9A-F]{6}$/i.test(value)),
  quantity: z.number().int().min(1).max(99),
  purchasePrice: z
    .string()
    .refine((value) => value.trim() === '' || parsePriceToCents(value) !== null),
});

export type AddCanFormValues = z.infer<typeof addCanSchema>;
