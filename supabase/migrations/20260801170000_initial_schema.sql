begin;

create extension if not exists pgcrypto;
create extension if not exists pg_trgm;

create type public.verification_status as enum (
  'unverified', 'community_submitted', 'source_verified',
  'manufacturer_verified', 'deprecated', 'disputed'
);
create type public.can_status as enum (
  'in_stock', 'opened', 'reserved', 'empty', 'consumed', 'sold',
  'gifted', 'lost', 'damaged', 'disposed', 'collection', 'archived'
);
create type public.fill_confidence as enum ('unknown', 'estimated', 'weighed');
create type public.asset_license_status as enum (
  'unknown', 'permission_required', 'approved', 'user_owned', 'public_domain', 'rejected'
);

create table public.catalog_versions (
  id text primary key,
  verification_status public.verification_status not null default 'unverified',
  notes text,
  published_at timestamptz,
  created_at timestamptz not null default now()
);

create table public.brands (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique check (slug = lower(slug) and slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  display_name text not null check (length(trim(display_name)) > 0),
  legal_name text,
  country_code text check (country_code is null or country_code ~ '^[A-Z]{2}$'),
  website_reference text,
  verification_status public.verification_status not null default 'unverified',
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.can_lines (
  id uuid primary key default gen_random_uuid(),
  brand_id uuid not null references public.brands(id) on delete restrict,
  catalog_key text not null unique,
  slug text not null check (slug = lower(slug) and slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
  display_name text not null check (length(trim(display_name)) > 0),
  default_volume_ml integer check (default_volume_ml is null or default_volume_ml > 0),
  alternative_volumes_ml integer[] not null default '{}',
  pressure_type text,
  paint_type text,
  finish text,
  standard_cap text,
  verification_status public.verification_status not null default 'unverified',
  active boolean not null default true,
  discontinued boolean not null default false,
  replacement_can_line_id uuid references public.can_lines(id) on delete set null,
  release_period text,
  source_reference text,
  last_verified_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (brand_id, slug)
);

create table public.colors (
  id uuid primary key default gen_random_uuid(),
  can_line_id uuid not null references public.can_lines(id) on delete cascade,
  official_code text,
  official_name text not null check (length(trim(official_name)) > 0),
  normalized_name text not null,
  hex_approximation text check (hex_approximation is null or hex_approximation ~ '^#[0-9A-Fa-f]{6}$'),
  rgb smallint[] check (rgb is null or cardinality(rgb) = 3),
  lab double precision[] check (lab is null or cardinality(lab) = 3),
  color_family text not null default 'unknown',
  finish text,
  is_metallic boolean not null default false,
  is_fluorescent boolean not null default false,
  is_transparent boolean not null default false,
  is_chrome boolean not null default false,
  discontinued boolean not null default false,
  alternative_names text[] not null default '{}',
  former_names text[] not null default '{}',
  verification_status public.verification_status not null default 'unverified',
  source_reference text,
  last_verified_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique nulls not distinct (can_line_id, official_code, official_name)
);

create table public.product_variants (
  id uuid primary key default gen_random_uuid(),
  can_line_id uuid not null references public.can_lines(id) on delete restrict,
  color_id uuid references public.colors(id) on delete restrict,
  volume_ml integer check (volume_ml is null or volume_ml > 0),
  manufacturer_sku text,
  packaging_version text,
  region_code text,
  verification_status public.verification_status not null default 'unverified',
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.product_barcodes (
  id uuid primary key default gen_random_uuid(),
  product_variant_id uuid references public.product_variants(id) on delete cascade,
  can_line_id uuid references public.can_lines(id) on delete cascade,
  barcode text not null check (length(trim(barcode)) between 4 and 128),
  barcode_type text not null,
  region_code text,
  confidence text not null check (confidence in ('certain', 'probable', 'uncertain', 'manual_review')),
  source_reference text,
  verified boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (product_variant_id is not null or can_line_id is not null)
);

create table public.brand_assets (
  id uuid primary key default gen_random_uuid(),
  brand_id uuid not null references public.brands(id) on delete cascade,
  asset_type text not null check (asset_type in ('logo_light', 'logo_dark', 'preview')),
  storage_path text not null,
  source_type text not null,
  source_reference text,
  license_status public.asset_license_status not null default 'unknown',
  approved_for_production boolean not null default false,
  retrieved_at date,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.can_line_assets (
  id uuid primary key default gen_random_uuid(),
  can_line_id uuid not null references public.can_lines(id) on delete cascade,
  asset_type text not null check (asset_type in ('front', 'silhouette', 'thumbnail', 'illustrative')),
  storage_path text not null,
  source_type text not null,
  source_reference text,
  license_status public.asset_license_status not null default 'unknown',
  approved_for_production boolean not null default false,
  retrieved_at date,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  avatar_path text,
  locale text not null default 'de-DE',
  currency text not null default 'EUR' check (currency ~ '^[A-Z]{3}$'),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.retailers (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null check (length(trim(name)) > 0),
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.storage_locations (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  parent_id text references public.storage_locations(id) on delete restrict,
  name text not null check (length(trim(name)) > 0),
  description text,
  photo_path text,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.purchases (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  retailer_id text references public.retailers(id) on delete set null,
  purchased_at timestamptz not null default now(),
  currency text not null default 'EUR' check (currency ~ '^[A-Z]{3}$'),
  subtotal_cents integer check (subtotal_cents is null or subtotal_cents >= 0),
  shipping_cents integer check (shipping_cents is null or shipping_cents >= 0),
  discount_cents integer check (discount_cents is null or discount_cents >= 0),
  tax_cents integer check (tax_cents is null or tax_cents >= 0),
  total_cents integer check (total_cents is null or total_cents >= 0),
  receipt_asset_path text,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.purchase_items (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  purchase_id text not null references public.purchases(id) on delete cascade,
  product_variant_id uuid references public.product_variants(id) on delete set null,
  quantity integer not null check (quantity > 0),
  unit_price_cents integer check (unit_price_cents is null or unit_price_cents >= 0),
  total_price_cents integer check (total_price_cents is null or total_price_cents >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.can_batches (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  product_variant_id uuid references public.product_variants(id) on delete set null,
  source_purchase_item_id text references public.purchase_items(id) on delete set null,
  original_quantity integer not null check (original_quantity > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.user_cans (
  id text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  product_variant_id uuid references public.product_variants(id) on delete set null,
  catalog_brand_key text references public.brands(slug) on delete restrict,
  catalog_can_line_key text references public.can_lines(catalog_key) on delete restrict,
  batch_id text references public.can_batches(id) on delete set null,
  custom_brand_name text,
  custom_line_name text,
  custom_color_name text,
  custom_color_code text,
  custom_hex text check (custom_hex is null or custom_hex ~ '^#[0-9A-Fa-f]{6}$'),
  volume_ml integer check (volume_ml is null or volume_ml > 0),
  estimated_fill_percent numeric(5,2) check (estimated_fill_percent is null or estimated_fill_percent between 0 and 100),
  fill_confidence public.fill_confidence not null default 'unknown',
  status public.can_status not null default 'in_stock',
  storage_location_id text references public.storage_locations(id) on delete set null,
  purchase_item_id text references public.purchase_items(id) on delete set null,
  purchase_price_cents integer check (purchase_price_cents is null or purchase_price_cents >= 0),
  currency text not null default 'EUR' check (currency ~ '^[A-Z]{3}$'),
  opened_at timestamptz,
  acquired_at timestamptz,
  archived_at timestamptz,
  notes text,
  favorite boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  check (
    product_variant_id is not null
    or catalog_can_line_key is not null
    or (custom_brand_name is not null and custom_line_name is not null)
  )
);

create table public.can_events (
  id text primary key,
  user_can_id text not null references public.user_cans(id) on delete cascade,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  event_type text not null,
  previous_state jsonb not null default '{}',
  new_state jsonb not null default '{}',
  metadata jsonb not null default '{}',
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table public.projects (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null check (length(trim(title)) > 0),
  project_type text not null,
  description text,
  started_at timestamptz,
  completed_at timestamptz,
  area_m2 numeric check (area_m2 is null or area_m2 >= 0),
  private_location_text text,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.project_can_usage (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  project_id text not null references public.projects(id) on delete cascade,
  user_can_id text not null references public.user_cans(id) on delete restrict,
  estimated_used_ml numeric check (estimated_used_ml is null or estimated_used_ml >= 0),
  fill_before numeric(5,2) check (fill_before is null or fill_before between 0 and 100),
  fill_after numeric(5,2) check (fill_after is null or fill_after between 0 and 100),
  usage_confidence public.fill_confidence not null default 'estimated',
  created_at timestamptz not null default now(),
  check (fill_before is null or fill_after is null or fill_after <= fill_before)
);

create table public.shopping_list_items (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  product_variant_id uuid references public.product_variants(id) on delete set null,
  custom_description text,
  reason text not null,
  desired_quantity integer not null default 1 check (desired_quantity > 0),
  priority smallint not null default 0 check (priority between 0 and 3),
  target_price_cents integer check (target_price_cents is null or target_price_cents >= 0),
  preferred_retailer_id text references public.retailers(id) on delete set null,
  project_id text references public.projects(id) on delete set null,
  note text,
  purchased_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (product_variant_id is not null or custom_description is not null)
);

create table public.minimum_stock_rules (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  product_variant_id uuid references public.product_variants(id) on delete cascade,
  can_line_id uuid references public.can_lines(id) on delete cascade,
  color_family text,
  minimum_quantity integer not null check (minimum_quantity >= 0),
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (num_nonnulls(product_variant_id, can_line_id, color_family) = 1)
);

create table public.user_favorites (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  product_variant_id uuid not null references public.product_variants(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (user_id, product_variant_id)
);

create table public.user_tags (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null check (length(trim(name)) > 0),
  created_at timestamptz not null default now(),
  unique (user_id, name)
);

create table public.can_tag_assignments (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  user_can_id text not null references public.user_cans(id) on delete cascade,
  tag_id text not null references public.user_tags(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (user_can_id, tag_id)
);

create table public.user_settings (
  user_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
  language text not null default 'de' check (language in ('de', 'en')),
  currency text not null default 'EUR' check (currency ~ '^[A-Z]{3}$'),
  area_unit text not null default 'm2',
  volume_unit text not null default 'ml',
  reduced_motion boolean,
  high_contrast boolean not null default false,
  scanner_sound boolean not null default true,
  scanner_haptics boolean not null default true,
  telemetry_opt_in boolean not null default false,
  coverage_factor_m2_per_ml numeric not null default 0.01 check (coverage_factor_m2_per_ml >= 0),
  efficiency_factor numeric not null default 0.65 check (efficiency_factor between 0 and 1),
  dashboard_widgets jsonb not null default '[]',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.catalog_corrections (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  entity_type text not null,
  entity_id text,
  proposed_changes jsonb not null,
  status public.verification_status not null default 'community_submitted',
  source_reference text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.sync_operations (
  id text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  entity_type text not null,
  entity_id text not null,
  operation_type text not null check (operation_type in ('insert', 'update', 'delete', 'upsert')),
  payload jsonb not null,
  local_version bigint not null,
  client_created_at timestamptz not null,
  retry_count integer not null default 0 check (retry_count >= 0),
  error_code text,
  processed_at timestamptz,
  created_at timestamptz not null default now()
);

create table public.notifications (
  id text primary key default gen_random_uuid()::text,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  notification_type text not null,
  title_key text not null,
  body_key text not null,
  parameters jsonb not null default '{}',
  read_at timestamptz,
  created_at timestamptz not null default now()
);

create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create or replace function public.validate_can_status_transition()
returns trigger language plpgsql as $$
declare
  allowed public.can_status[];
begin
  if old.status = new.status then return new; end if;
  allowed := case old.status
    when 'in_stock' then array['opened','reserved','empty','sold','gifted','lost','damaged','collection','archived']::public.can_status[]
    when 'opened' then array['reserved','empty','consumed','lost','damaged','archived']::public.can_status[]
    when 'reserved' then array['in_stock','opened','archived']::public.can_status[]
    when 'empty' then array['consumed','archived']::public.can_status[]
    when 'consumed' then array['archived']::public.can_status[]
    when 'sold' then array['archived']::public.can_status[]
    when 'gifted' then array['archived']::public.can_status[]
    when 'lost' then array['in_stock','archived']::public.can_status[]
    when 'damaged' then array['disposed','in_stock','archived']::public.can_status[]
    when 'disposed' then array['archived']::public.can_status[]
    when 'collection' then array['in_stock','archived']::public.can_status[]
    when 'archived' then case when old.archived_at is not null and new.archived_at is null
      then array['in_stock','opened','reserved','empty','consumed','sold','gifted','lost','damaged','disposed','collection']::public.can_status[]
      else array[]::public.can_status[] end
  end;
  if not (new.status = any(allowed)) then
    raise exception 'invalid can status transition: % -> %', old.status, new.status using errcode = '23514';
  end if;
  if new.status = 'archived' and new.archived_at is null then new.archived_at = now(); end if;
  return new;
end;
$$;

create or replace function public.prevent_event_mutation()
returns trigger language plpgsql as $$
begin
  raise exception 'can events are append-only' using errcode = '55000';
end;
$$;

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id) values (new.id) on conflict do nothing;
  insert into public.user_settings (user_id) values (new.id) on conflict do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created after insert on auth.users
for each row execute function public.handle_new_user();

create trigger validate_user_can_status before update of status on public.user_cans
for each row execute function public.validate_can_status_transition();
create trigger can_events_no_update before update or delete on public.can_events
for each row execute function public.prevent_event_mutation();

do $$
declare table_name text;
begin
  foreach table_name in array array[
    'brands','can_lines','colors','product_variants','product_barcodes','brand_assets','can_line_assets',
    'profiles','retailers','storage_locations','purchases','purchase_items','can_batches','user_cans',
    'projects','shopping_list_items','minimum_stock_rules','user_settings','catalog_corrections'
  ] loop
    execute format('create trigger %I_set_updated_at before update on public.%I for each row execute function public.set_updated_at()', table_name, table_name);
  end loop;
end;
$$;

create index brands_display_name_trgm_idx on public.brands using gin (display_name gin_trgm_ops);
create index can_lines_display_name_trgm_idx on public.can_lines using gin (display_name gin_trgm_ops);
create index colors_name_trgm_idx on public.colors using gin (normalized_name gin_trgm_ops);
create index colors_code_idx on public.colors (can_line_id, official_code);
create index product_barcodes_lookup_idx on public.product_barcodes (barcode);
create index user_cans_user_status_created_idx on public.user_cans (user_id, status, created_at desc);
create index user_cans_active_idx on public.user_cans (user_id, catalog_can_line_key, custom_color_code) where deleted_at is null and status not in ('archived','consumed','sold','gifted','disposed');
create index user_cans_location_idx on public.user_cans (user_id, storage_location_id) where deleted_at is null;
create index can_events_timeline_idx on public.can_events (user_id, user_can_id, occurred_at desc);
create index purchases_user_date_idx on public.purchases (user_id, purchased_at desc);
create index projects_user_date_idx on public.projects (user_id, started_at desc);
create index sync_operations_pending_idx on public.sync_operations (user_id, created_at) where processed_at is null;

alter table public.catalog_versions enable row level security;
alter table public.brands enable row level security;
alter table public.can_lines enable row level security;
alter table public.colors enable row level security;
alter table public.product_variants enable row level security;
alter table public.product_barcodes enable row level security;
alter table public.brand_assets enable row level security;
alter table public.can_line_assets enable row level security;

create policy catalog_versions_read on public.catalog_versions for select to anon, authenticated using (true);
create policy brands_read on public.brands for select to anon, authenticated using (true);
create policy can_lines_read on public.can_lines for select to anon, authenticated using (true);
create policy colors_read on public.colors for select to anon, authenticated using (true);
create policy product_variants_read on public.product_variants for select to anon, authenticated using (true);
create policy product_barcodes_read on public.product_barcodes for select to anon, authenticated using (true);
create policy brand_assets_read on public.brand_assets for select to anon, authenticated using (approved_for_production);
create policy can_line_assets_read on public.can_line_assets for select to anon, authenticated using (approved_for_production);

alter table public.profiles enable row level security;
create policy profiles_own on public.profiles for all to authenticated using (id = auth.uid()) with check (id = auth.uid());

do $$
declare table_name text;
begin
  foreach table_name in array array[
    'retailers','storage_locations','purchases','purchase_items','can_batches','user_cans','projects',
    'project_can_usage','shopping_list_items','minimum_stock_rules','user_favorites','user_tags',
    'can_tag_assignments','catalog_corrections','sync_operations','notifications'
  ] loop
    execute format('alter table public.%I enable row level security', table_name);
    execute format('create policy %I_own on public.%I for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid())', table_name, table_name);
  end loop;
end;
$$;

alter table public.user_settings enable row level security;
create policy user_settings_own on public.user_settings for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

alter table public.can_events enable row level security;
create policy can_events_read_own on public.can_events for select to authenticated using (user_id = auth.uid());
create policy can_events_insert_own on public.can_events for insert to authenticated with check (
  user_id = auth.uid() and exists (
    select 1 from public.user_cans where user_cans.id = can_events.user_can_id and user_cans.user_id = auth.uid()
  )
);

commit;
