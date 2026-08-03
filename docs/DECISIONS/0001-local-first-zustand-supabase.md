# ADR 0001: Local-first state with a Supabase boundary

Status: accepted for vertical slice

The UI commits to a persisted local Zustand store and queues remote upserts. This makes the key flow usable without credentials and prevents network latency or failure from losing a capture. Supabase remains the authoritative multi-device service once authenticated.

AsyncStorage is intentionally temporary. SQLite replaces it before large-inventory release because indexed local queries and transactional migrations are required.
