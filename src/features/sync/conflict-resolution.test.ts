import { describe, expect, it } from 'vitest';

import { conflictNeedsUserDecision, lastWriteWins } from './conflict-resolution';

describe('sync conflict policy', () => {
  it('requires a user decision for high-impact fields', () => {
    expect(conflictNeedsUserDecision('status')).toBe(true);
    expect(conflictNeedsUserDecision('estimated_fill_percent')).toBe(true);
    expect(conflictNeedsUserDecision('notes')).toBe(false);
  });

  it('uses timestamps only for low-risk last-write-wins fields', () => {
    expect(
      lastWriteWins(
        { value: 'local', updatedAt: '2026-08-01T12:00:00Z' },
        { value: 'remote', updatedAt: '2026-08-01T11:00:00Z' },
      ),
    ).toBe('local');
  });
});
