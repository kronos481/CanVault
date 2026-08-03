# Prioritized roadmap

## P0 — completed vertical slice

- Expo/TypeScript architecture and design tokens
- Versioned 15-brand / 34-line unverified seed
- Manual wizard with validation and physical-instance creation
- Offline persistence, event history, sync queue and optional Supabase boundary
- Inventory grid, details, estimated fill updates, archive/restore/undo
- German/English translation structure
- PostgreSQL schema, RLS, indexes, state trigger and pgTAP tests
- Pure domain tests and documentation

## P0 — next

- Supabase authentication screens, password reset and profile lifecycle
- Barcode camera flow, permission states and local/backend match confirmation
- SQLite normalized cache and connectivity-driven retry UI
- Storage locations and editing
- CSV export/import
- Run database tests and generate Supabase TypeScript types
- Screen-level accessibility tests and Maestro device flows

## P1

- Dashboard warnings/widgets, filters/sorting/grouping, inventory mode
- Purchases, archive statistics, projects and shopping list
- Multi-scan purchase aggregation
- Image upload with EXIF removal and rights registry workflow

## P2

- OCR/photo-recognition provider interfaces and confirmed suggestions
- LAB/Delta E palette tools, color wall, gap finder and comparison
- Admin catalog review UI and catalog rollback

No production-readiness claim is allowed before real-device testing, hosted RLS verification, privacy review and asset-rights clearance.
