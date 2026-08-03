import type { CanStatus } from './types';

const allowedTransitions: Record<CanStatus, readonly CanStatus[]> = {
  in_stock: [
    'opened',
    'reserved',
    'empty',
    'sold',
    'gifted',
    'lost',
    'damaged',
    'collection',
    'archived',
  ],
  opened: ['reserved', 'empty', 'consumed', 'lost', 'damaged', 'archived'],
  reserved: ['in_stock', 'opened', 'archived'],
  empty: ['consumed', 'archived'],
  consumed: ['archived'],
  sold: ['archived'],
  gifted: ['archived'],
  lost: ['in_stock', 'archived'],
  damaged: ['disposed', 'in_stock', 'archived'],
  disposed: ['archived'],
  collection: ['in_stock', 'archived'],
  archived: [],
};

export function canTransition(from: CanStatus, to: CanStatus): boolean {
  return allowedTransitions[from].includes(to);
}

export function assertCanTransition(from: CanStatus, to: CanStatus): void {
  if (!canTransition(from, to)) {
    throw new Error(`Invalid can status transition: ${from} -> ${to}`);
  }
}
