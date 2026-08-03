# Offline sync

## Guarantees in the current slice

- Manual creation, inventory reads, detail reads, fill changes, archives and restores complete locally first.
- Each operation survives app restart through AsyncStorage.
- UI never waits for the network.
- Queue processing is ordered and stops on failure.

## Operation envelope

Each `SyncOperation` contains a local ID, entity, operation, serializable payload, local version, creation time, retry count and redacted error. Server-side `sync_operations` can later provide idempotency receipts across devices.

## Conflict policy

- Low-risk scalar fields such as favorite flags may use last-write-wins.
- Status, fill level and storage location require a user decision or event merge.
- Can events are append-only and merged by event ID/time.
- No status or fill conflict may be silently overwritten.

## Recovery

Pending operations remain visible in the header. A later slice must add connectivity listeners, exponential backoff with jitter, a retry UI, conflict-resolution sheets and authenticated background resumption.

AsyncStorage is acceptable for the vertical slice but not the final 10,000-can target. The planned production cache is normalized SQLite with migrations and indexed local search.
