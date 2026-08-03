import { describe, expect, it } from 'vitest';

import { splitBatchQuantity } from './batches';
import { estimateFillFromWeight } from './fill';
import { canTransition } from './status-machine';

describe('inventory domain', () => {
  it('estimates fill from known weights and clamps uncertainty', () => {
    expect(estimateFillFromWeight(350, 100, 600)).toBe(50);
    expect(estimateFillFromWeight(40, 100, 600)).toBe(0);
    expect(estimateFillFromWeight(700, 100, 600)).toBe(100);
  });

  it('rejects an invalid weight model', () => {
    expect(() => estimateFillFromWeight(100, 500, 500)).toThrow();
  });

  it('splits grouped stock without losing physical quantity', () => {
    const split = splitBatchQuantity(5, 2);
    expect(split).toEqual({ remaining: 3, moved: 2 });
    expect(split.remaining + split.moved).toBe(5);
  });

  it('rejects moving more cans than a batch contains', () => {
    expect(() => splitBatchQuantity(2, 3)).toThrow();
  });

  it('enforces allowed status transitions', () => {
    expect(canTransition('in_stock', 'opened')).toBe(true);
    expect(canTransition('sold', 'opened')).toBe(false);
    expect(canTransition('damaged', 'disposed')).toBe(true);
  });
});
