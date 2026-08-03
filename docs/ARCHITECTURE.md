# Architecture

## Scope

CANVAULT is an Expo Router application with an offline-first local domain and an optional Supabase synchronization boundary. The current vertical slice works without network or credentials; remote persistence activates only for an authenticated Supabase session.

## Layers

- `src/app/`: navigation and screen composition only.
- `src/components/`: reusable visual, form, layout and feedback primitives.
- `src/features/catalog/`: versioned read-only catalog and lookup helpers.
- `src/features/inventory/`: schemas, state machine, persisted inventory and events.
- `src/features/sync/`: conflict rules, queue flushing and remote boundary.
- `src/features/statistics/`: pure calculations, explicitly labeled as estimates.
- `src/lib/`: configured infrastructure clients.
- `supabase/`: append-only migrations, versioned seed and pgTAP tests.

## Data flow

1. Zod validates the manual entry form.
2. A single action creates one `UserCan` per physical can plus a creation event.
3. Zustand commits immediately and persists through AsyncStorage.
4. The same action appends idempotent upsert work to the sync queue.
5. If Supabase is configured and authenticated, the queue is flushed sequentially.
6. Failed work remains visible as pending/error state and can be retried later.

Screens never contain catalog constants or persistence rules. Business logic remains in feature modules and pure functions.

## State ownership

- Server/catalog cache: TanStack Query (prepared; remote catalog queries are a later slice).
- Durable offline inventory, events, queue and language: Zustand persisted storage.
- Ephemeral form state: React Hook Form.
- Ephemeral screen state such as search and wizard step: component state.

## Error handling

- Invalid form values are shown adjacent to their fields.
- Missing local records produce a recoverable empty/error screen.
- Remote failures are redacted to 160 characters and retained only on the operation.
- Sync stops after the first failed operation to preserve ordering.
- Destructive archive operations are confirmed and support a five-second undo.

## Performance boundaries

- FlashList virtualizes inventory and archive grids.
- Catalog helpers are pure; screen filtering is memoized.
- Images are not loaded in lists; the MVP uses a code-rendered neutral can silhouette.
- PostgreSQL has partial active-inventory, timeline, barcode and trigram search indexes.
- Cursor pagination and server-side filtering are required before claiming 10,000-row production performance.

## Name configuration

`app-brand.json` is the single checked-in brand source. `EXPO_PUBLIC_APP_NAME` can override display name per build without changing domain code.
