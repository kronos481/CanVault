import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';

import type { UserCan } from '@/features/inventory/types';

import { createInventoryCsv } from './inventory-csv';

export async function shareInventoryCsv(cans: readonly UserCan[], dialogTitle: string) {
  if (!(await Sharing.isAvailableAsync())) {
    throw new Error('sharing_unavailable');
  }

  const day = new Date().toISOString().slice(0, 10);
  const file = new File(Paths.cache, `canvault-inventory-${day}.csv`);
  file.create({ overwrite: true, intermediates: true });
  file.write(`\uFEFF${createInventoryCsv(cans)}`);

  await Sharing.shareAsync(file.uri, {
    dialogTitle,
    mimeType: 'text/csv',
    UTI: 'public.comma-separated-values-text',
  });

  return file.uri;
}
