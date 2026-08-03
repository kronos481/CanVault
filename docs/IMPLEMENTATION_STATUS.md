# Implementation status

## Implemented

- Functional local-first vertical slice from brand selection through archive/restore.
- 15 brands and 34 named can lines, all marked `unverified`.
- Separate MTN / Montana Colors and Montana Cans identities.
- Per-can physical instances, estimated fill, status validation, events and undo.
- Optional authenticated Supabase queue boundary and complete initial database contract.
- Loading, empty, error, validation, pending-sync and destructive-confirmation states.
- Dark-first design tokens, 44/48-point targets, screen-reader labels and non-color labels.
- Camera scanner for QR, EAN, UPC and common linear codes with permission, torch, failure and retry states.
- Strict CANVAULT QR payload validation, explicit confirmation and privacy-minimized QR transfer between devices.
- Unknown external codes are routed to manual review; malformed or catalog-mismatched CANVAULT codes are rejected.
- Original CANVAULT launcher/splash artwork and two project-owned neutral can illustrations.
- Inventory search, status/low-fill filters and recent/brand/fill sorting.
- Core statistics and a local all-history CSV export through the Android share sheet.
- Android application ID/versioning, camera-only permissions and EAS APK/AAB build profiles.
- Reduced-motion-aware press, screen, scanner and snackbar microanimations.
- 28 passing unit tests at the time of this document update.
- Expo's SDK compatibility check passes and the complete Android Hermes bundle exports successfully.

## Assumptions

- No verified manufacturer colors, barcodes, volumes, SKUs or images were supplied.
- Local-first use without authentication is desirable for development and offline evaluation.
- Default fill is a clearly labeled 100% estimate when a new can is captured.
- Purchase price means per-can paid price in the initial slice.

## Known limitations

- Authentication UI, storage locations, server-backed barcode mappings, multi-scan purchases, projects and shopping lists remain future modules.
- The scanner can safely decode any supported code, but only a validated CANVAULT QR payload can currently produce a catalog match because no verified GTIN/EAN mappings were supplied.
- Statistics are the MVP overview; projects, usage-based charts and area estimates are not yet connected to user workflows.
- Remote sync requires Supabase environment values and an authenticated session.
- Conflict rules exist, but the user-facing conflict resolver does not.
- AsyncStorage must move to SQLite before the 10,000-can performance target.
- Database pgTAP tests are authored but have not been executed in this environment.
- No real-device, Dynamic Type maximum-size, screen-reader or landscape run has been completed.
- Automated in-app-browser visual QA was blocked by an external Node module-mode conflict in the browser runtime; the exported HTML was still checked over HTTP and uses German document language.
- The production dependency audit reports 12 moderate Expo build/config-tool advisories and no high/critical advisory; see `SECURITY.md`.

## Asset and legal questions

All official logos and exact product images require documented rights. No official manufacturer asset is currently present. See `ASSET_SOURCES.md`.
