export type ConflictField =
  'notes' | 'favorite' | 'storage_location_id' | 'status' | 'estimated_fill_percent';

export function conflictNeedsUserDecision(field: ConflictField): boolean {
  return (
    field === 'status' || field === 'estimated_fill_percent' || field === 'storage_location_id'
  );
}

export function lastWriteWins<T>(
  local: { value: T; updatedAt: string },
  remote: { value: T; updatedAt: string },
): T {
  return Date.parse(local.updatedAt) >= Date.parse(remote.updatedAt) ? local.value : remote.value;
}
