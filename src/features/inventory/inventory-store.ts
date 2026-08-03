import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

import type { Language } from '@/i18n/translations';
import { createLocalId } from '@/utils/id';

import { assertCanTransition } from './status-machine';
import type { AddCanInput, CanEvent, CanStatus, SyncOperation, UserCan } from './types';

interface UndoSnapshot {
  cans: UserCan[];
  events: CanEvent[];
  syncQueue: SyncOperation[];
  messageKey: 'can.archived' | 'can.restored';
}

interface InventoryState {
  cans: UserCan[];
  events: CanEvent[];
  syncQueue: SyncOperation[];
  language: Language;
  hydrated: boolean;
  lastUndo: UndoSnapshot | null;
  addCans: (input: AddCanInput) => string[];
  updateFill: (canId: string, fillPercent: number | null) => void;
  archiveCan: (canId: string) => void;
  restoreCan: (canId: string) => void;
  undoLastMutation: () => void;
  clearUndo: () => void;
  setLanguage: (language: Language) => void;
  markHydrated: () => void;
  acknowledgeSync: (operationIds: string[]) => void;
  markSyncError: (operationId: string, error: string) => void;
}

function canToPayload(can: UserCan): Record<string, string | number | boolean | null> {
  return {
    id: can.id,
    product_variant_id: null,
    catalog_brand_key: can.brandId,
    catalog_can_line_key: can.canLineId,
    custom_brand_name: null,
    custom_line_name: null,
    custom_color_name: can.customColorName,
    custom_color_code: can.customColorCode,
    custom_hex: can.customHex,
    volume_ml: can.volumeMl,
    estimated_fill_percent: can.estimatedFillPercent,
    fill_confidence: can.fillConfidence,
    status: can.status,
    purchase_price_cents: can.purchasePriceCents,
    currency: can.currency,
    acquired_at: can.acquiredAt,
    archived_at: can.archivedAt,
    created_at: can.createdAt,
    updated_at: can.updatedAt,
  };
}

function makeSyncOperation(can: UserCan): SyncOperation {
  return {
    id: createLocalId('sync'),
    entity: 'user_can',
    operation: 'upsert',
    payload: canToPayload(can),
    localVersion: Date.parse(can.updatedAt),
    createdAt: new Date().toISOString(),
    retryCount: 0,
    error: null,
  };
}

function makeEventSyncOperation(event: CanEvent): SyncOperation {
  return {
    id: createLocalId('sync'),
    entity: 'can_event',
    operation: 'upsert',
    payload: {
      id: event.id,
      user_can_id: event.userCanId,
      event_type: event.eventType,
      previous_state: event.previousState,
      new_state: event.newState,
      metadata: {},
      occurred_at: event.occurredAt,
    },
    localVersion: Date.parse(event.occurredAt),
    createdAt: new Date().toISOString(),
    retryCount: 0,
    error: null,
  };
}

function makeEvent(
  userCanId: string,
  eventType: CanEvent['eventType'],
  previousState: CanEvent['previousState'],
  newState: CanEvent['newState'],
): CanEvent {
  return {
    id: createLocalId('event'),
    userCanId,
    eventType,
    previousState,
    newState,
    occurredAt: new Date().toISOString(),
  };
}

export const useInventoryStore = create<InventoryState>()(
  persist(
    (set) => ({
      cans: [],
      events: [],
      syncQueue: [],
      language: 'de',
      hydrated: false,
      lastUndo: null,

      addCans: (input) => {
        const now = new Date().toISOString();
        const cans: UserCan[] = Array.from({ length: input.quantity }, () => ({
          id: createLocalId('can'),
          brandId: input.brandId,
          canLineId: input.canLineId,
          customColorName: input.colorName.trim(),
          customColorCode: input.colorCode?.trim() || null,
          customHex: input.customHex?.toUpperCase() || null,
          volumeMl: null,
          estimatedFillPercent: 100,
          fillConfidence: 'estimated',
          status: 'in_stock',
          statusBeforeArchive: null,
          purchasePriceCents: input.purchasePriceCents,
          currency: input.currency,
          acquiredAt: now,
          archivedAt: null,
          createdAt: now,
          updatedAt: now,
        }));
        const events = cans.map((can) => makeEvent(can.id, 'created', {}, { status: can.status }));
        set((state) => ({
          cans: [...cans, ...state.cans],
          events: [...events, ...state.events],
          syncQueue: [
            ...state.syncQueue,
            ...cans.map(makeSyncOperation),
            ...events.map(makeEventSyncOperation),
          ],
          lastUndo: null,
        }));
        return cans.map((can) => can.id);
      },

      updateFill: (canId, fillPercent) => {
        if (fillPercent !== null && (fillPercent < 0 || fillPercent > 100)) {
          throw new Error('Fill percent must be between 0 and 100.');
        }
        set((state) => {
          const current = state.cans.find((can) => can.id === canId);
          if (!current) return state;
          const now = new Date().toISOString();
          const nextStatus: CanStatus = fillPercent === 0 ? 'empty' : current.status;
          if (nextStatus !== current.status) assertCanTransition(current.status, nextStatus);
          const updated: UserCan = {
            ...current,
            estimatedFillPercent: fillPercent,
            fillConfidence: fillPercent === null ? 'unknown' : 'estimated',
            status: nextStatus,
            updatedAt: now,
          };
          const event = makeEvent(
            canId,
            'fill_changed',
            { fillPercent: current.estimatedFillPercent },
            { fillPercent, status: nextStatus },
          );
          return {
            cans: state.cans.map((can) => (can.id === canId ? updated : can)),
            events: [event, ...state.events],
            syncQueue: [
              ...state.syncQueue,
              makeSyncOperation(updated),
              makeEventSyncOperation(event),
            ],
            lastUndo: null,
          };
        });
      },

      archiveCan: (canId) => {
        set((state) => {
          const current = state.cans.find((can) => can.id === canId);
          if (!current || current.status === 'archived') return state;
          assertCanTransition(current.status, 'archived');
          const now = new Date().toISOString();
          const updated: UserCan = {
            ...current,
            status: 'archived',
            statusBeforeArchive: current.status,
            archivedAt: now,
            updatedAt: now,
          };
          const event = makeEvent(
            canId,
            'archived',
            { status: current.status },
            { status: 'archived' },
          );
          return {
            cans: state.cans.map((can) => (can.id === canId ? updated : can)),
            events: [event, ...state.events],
            syncQueue: [
              ...state.syncQueue,
              makeSyncOperation(updated),
              makeEventSyncOperation(event),
            ],
            lastUndo: {
              cans: state.cans,
              events: state.events,
              syncQueue: state.syncQueue,
              messageKey: 'can.archived',
            },
          };
        });
      },

      restoreCan: (canId) => {
        set((state) => {
          const current = state.cans.find((can) => can.id === canId);
          if (!current || current.status !== 'archived') return state;
          const restoredStatus = current.statusBeforeArchive ?? 'in_stock';
          const now = new Date().toISOString();
          const updated: UserCan = {
            ...current,
            status: restoredStatus,
            statusBeforeArchive: null,
            archivedAt: null,
            updatedAt: now,
          };
          const event = makeEvent(
            canId,
            'restored',
            { status: 'archived' },
            { status: restoredStatus },
          );
          return {
            cans: state.cans.map((can) => (can.id === canId ? updated : can)),
            events: [event, ...state.events],
            syncQueue: [
              ...state.syncQueue,
              makeSyncOperation(updated),
              makeEventSyncOperation(event),
            ],
            lastUndo: {
              cans: state.cans,
              events: state.events,
              syncQueue: state.syncQueue,
              messageKey: 'can.restored',
            },
          };
        });
      },

      undoLastMutation: () =>
        set((state) =>
          state.lastUndo
            ? {
                cans: state.lastUndo.cans,
                events: state.lastUndo.events,
                syncQueue: state.lastUndo.syncQueue,
                lastUndo: null,
              }
            : state,
        ),
      clearUndo: () => set({ lastUndo: null }),
      setLanguage: (language) => set({ language }),
      markHydrated: () => set({ hydrated: true }),
      acknowledgeSync: (operationIds) =>
        set((state) => ({
          syncQueue: state.syncQueue.filter((operation) => !operationIds.includes(operation.id)),
        })),
      markSyncError: (operationId, error) =>
        set((state) => ({
          syncQueue: state.syncQueue.map((operation) =>
            operation.id === operationId
              ? { ...operation, retryCount: operation.retryCount + 1, error }
              : operation,
          ),
        })),
    }),
    {
      name: 'canvault-inventory-v1',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({
        cans: state.cans,
        events: state.events,
        syncQueue: state.syncQueue,
        language: state.language,
      }),
      onRehydrateStorage: () => (state) => state?.markHydrated(),
    },
  ),
);

export const selectActiveCans = (state: InventoryState) =>
  state.cans.filter(
    (can) => !['archived', 'consumed', 'sold', 'gifted', 'disposed'].includes(can.status),
  );
