import { supabase } from '@/lib/supabase';

import { useInventoryStore } from '../inventory/inventory-store';

let flushPromise: Promise<void> | null = null;

async function flushInternal(): Promise<void> {
  if (!supabase) return;
  const { data } = await supabase.auth.getSession();
  if (!data.session) return;

  const operations = useInventoryStore.getState().syncQueue;
  for (const operation of operations) {
    try {
      const table = operation.entity === 'user_can' ? 'user_cans' : 'can_events';
      const { error } =
        operation.entity === 'can_event'
          ? await supabase.from(table).upsert(operation.payload, { ignoreDuplicates: true })
          : await supabase.from(table).upsert(operation.payload);
      if (error) throw error;
      useInventoryStore.getState().acknowledgeSync([operation.id]);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unknown sync error';
      useInventoryStore.getState().markSyncError(operation.id, message.slice(0, 160));
      break;
    }
  }
}

export function flushSyncQueue(): Promise<void> {
  if (!flushPromise) {
    flushPromise = flushInternal().finally(() => {
      flushPromise = null;
    });
  }
  return flushPromise;
}
