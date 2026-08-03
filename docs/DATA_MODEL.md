# Data model

## Catalog vs user data

Global catalog tables (`brands`, `can_lines`, `colors`, `product_variants`, `product_barcodes`, asset tables) are readable but not client-writable. User corrections land in `catalog_corrections`; they never mutate verified catalog data directly.

Every catalog entity carries verification state. The v1 seed intentionally includes no colors or codes because no complete verified source was provided.

## Physical cans

`user_cans` represents one physical can. UI grouping is a projection only. `can_batches` records a shared purchase origin, while instances retain individual status, fill level and events.

Offline-friendly text IDs are accepted for user-owned entities. `catalog_brand_key` and `catalog_can_line_key` retain referential integrity against stable catalog keys even before a full product variant is known.

## History

`can_events` is append-only at policy and trigger level. Status changes, fill changes, archives and restoration produce events instead of rewriting history. User-can rows remain efficient current-state projections.

## Money and estimates

- Money is stored as integer minor units with an ISO 4217 currency code.
- Fill values are 0–100 and always include confidence.
- Volume and area estimates permit unknowns; they are never silently replaced with invented defaults.
- Area uses `used_paint_ml × coverage_factor_m2_per_ml × efficiency_factor` and must be labeled estimated.

## Deletion

User-owned rows cascade from `auth.users` for account deletion. `user_cans.deleted_at` supports recoverable user deletion while status/archive history is retained. Storage objects require a separate, authenticated deletion job before production launch.
