# Security and privacy

## Implemented controls

- Strict environment validation; the app never embeds Supabase credentials.
- Native auth sessions use SecureStore; web falls back to AsyncStorage.
- Every user-owned PostgreSQL table has RLS.
- Catalog clients receive read-only policies; asset reads require production approval.
- `can_events` is append-only.
- Database trigger rejects invalid status transitions.
- Account-owned rows cascade from `auth.users`.
- No tracking SDK, public profile or public location exists.
- Sync errors are length-limited and no analytics payload contains color, note, location or barcode content.
- Scanned CANVAULT payloads are strict, length-bounded and catalog-validated before they can create a can.
- Unknown product codes require manual review and no scan automatically persists data.
- CSV export neutralizes spreadsheet-formula prefixes in user-entered values.

## Required before production

- Run pgTAP policies against a local and hosted Supabase project.
- Add storage bucket policies, EXIF stripping and receipt-data retention rules.
- Implement authenticated account export and deletion jobs, including storage objects and backups.
- Add rate limits and admin authorization for catalog workflows.
- Perform dependency and mobile penetration review.
- Document GDPR legal basis, retention, controller contact and consent behavior.

## Dependency audit (2026-08-01)

`npm audit --omit=dev` reports 12 moderate, 0 high and 0 critical advisories. All reported paths are inside Expo configuration/build tooling, including `expo-sharing → @expo/config-plugins` and an `xcode → uuid` path. npm's automatic remediation proposes downgrading Expo to SDK 46 / Expo modules to SDK 55-era versions, which is incompatible with the required SDK 57 stack and was therefore not applied. Track upstream SDK 57 patches and rerun the audit before each release.

The anon key is designed to be public and is safe only together with validated RLS. The service-role key must never enter the app.
