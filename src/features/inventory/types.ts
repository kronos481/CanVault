import type { Language } from '@/i18n/translations';

export type CanStatus =
  | 'in_stock'
  | 'opened'
  | 'reserved'
  | 'empty'
  | 'consumed'
  | 'sold'
  | 'gifted'
  | 'lost'
  | 'damaged'
  | 'disposed'
  | 'collection'
  | 'archived';

export type CanEventType = 'created' | 'fill_changed' | 'archived' | 'restored';
export type JsonValue =
  string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue };

export interface UserCan {
  id: string;
  brandId: string;
  canLineId: string;
  customColorName: string;
  customColorCode: string | null;
  customHex: string | null;
  volumeMl: number | null;
  estimatedFillPercent: number | null;
  fillConfidence: 'estimated' | 'unknown';
  status: CanStatus;
  statusBeforeArchive: CanStatus | null;
  purchasePriceCents: number | null;
  currency: string;
  acquiredAt: string;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CanEvent {
  id: string;
  userCanId: string;
  eventType: CanEventType;
  previousState: Record<string, JsonValue>;
  newState: Record<string, JsonValue>;
  occurredAt: string;
}

export interface SyncOperation {
  id: string;
  entity: 'user_can' | 'can_event';
  operation: 'upsert';
  payload: Record<string, JsonValue>;
  localVersion: number;
  createdAt: string;
  retryCount: number;
  error: string | null;
}

export interface AddCanInput {
  brandId: string;
  canLineId: string;
  colorName: string;
  colorCode?: string;
  customHex?: string;
  quantity: number;
  purchasePriceCents: number | null;
  currency: string;
}

export interface PersistedInventoryState {
  cans: UserCan[];
  events: CanEvent[];
  syncQueue: SyncOperation[];
  language: Language;
}
