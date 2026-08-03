begin;

insert into public.catalog_versions (id, verification_status, notes)
values (
  '2026.08.01-v1',
  'unverified',
  'Initial names supplied in the product brief. No official color, barcode, volume, pressure, paint or asset claims.'
)
on conflict (id) do update set notes = excluded.notes;

insert into public.brands (slug, display_name, verification_status)
values
  ('mtn-montana-colors', 'MTN / Montana Colors', 'unverified'),
  ('montana-cans', 'Montana Cans', 'unverified'),
  ('molotow-belton', 'Molotow / Belton', 'unverified'),
  ('loop-colors', 'Loop Colors', 'unverified'),
  ('flame', 'Flame', 'unverified'),
  ('kobra', 'Kobra', 'unverified'),
  ('ironlak', 'Ironlak', 'unverified'),
  ('nbq', 'NBQ', 'unverified'),
  ('dope', 'Dope', 'unverified'),
  ('dang', 'Dang', 'unverified'),
  ('clash', 'Clash', 'unverified'),
  ('beat', 'Beat', 'unverified'),
  ('scribo', 'Scribo', 'unverified'),
  ('double-a', 'Double A', 'unverified'),
  ('krink', 'Krink', 'unverified')
on conflict (slug) do update set
  display_name = excluded.display_name,
  verification_status = excluded.verification_status,
  updated_at = now();

with line_data (brand_slug, line_slug, display_name) as (
  values
    ('mtn-montana-colors', 'mtn-94', 'MTN 94'),
    ('mtn-montana-colors', 'mtn-hardcore', 'MTN Hardcore'),
    ('mtn-montana-colors', 'mtn-vice', 'MTN Vice'),
    ('mtn-montana-colors', 'mtn-water-based-400', 'MTN Water Based 400'),
    ('mtn-montana-colors', 'mtn-mega', 'MTN Mega'),
    ('mtn-montana-colors', 'mtn-alien', 'MTN Alien'),
    ('montana-cans', 'montana-black', 'Montana Black'),
    ('montana-cans', 'montana-gold', 'Montana Gold'),
    ('montana-cans', 'montana-white', 'Montana White'),
    ('montana-cans', 'montana-tarblack', 'Montana Tarblack'),
    ('montana-cans', 'montana-blackout-tarblack', 'Montana Blackout Tarblack'),
    ('montana-cans', 'montana-ultra-wide', 'Montana Ultra Wide'),
    ('molotow-belton', 'molotow-premium', 'Molotow Premium'),
    ('molotow-belton', 'molotow-burner', 'Molotow Burner'),
    ('molotow-belton', 'molotow-coversall', 'Molotow CoversAll'),
    ('loop-colors', 'loop-400-ml', 'Loop 400 ml'),
    ('loop-colors', 'loop-asphalt', 'Loop Asphalt'),
    ('flame', 'flame-blue', 'Flame Blue'),
    ('flame', 'flame-orange', 'Flame Orange'),
    ('kobra', 'kobra-hp', 'Kobra HP'),
    ('kobra', 'kobra-lp', 'Kobra LP'),
    ('ironlak', 'ironlak-400-ml', 'Ironlak 400 ml'),
    ('ironlak', 'sugar-artists-acrylic', 'Sugar Artists Acrylic'),
    ('nbq', 'nbq-fast', 'NBQ Fast'),
    ('nbq', 'nbq-slow', 'NBQ Slow'),
    ('dope', 'dope-action', 'Dope Action'),
    ('dope', 'dope-classic', 'Dope Classic'),
    ('dang', 'dang-prime', 'Dang Prime'),
    ('dang', 'dang-hi-flow', 'Dang Hi-Flow'),
    ('clash', 'clash', 'Clash'),
    ('beat', 'beat', 'Beat'),
    ('scribo', 'scribo', 'Scribo'),
    ('double-a', 'double-a', 'Double A'),
    ('krink', 'krink-k-750', 'Krink K-750')
)
insert into public.can_lines (
  brand_id,
  catalog_key,
  slug,
  display_name,
  verification_status,
  source_reference
)
select
  brands.id,
  line_data.brand_slug || ':' || line_data.line_slug,
  line_data.line_slug,
  line_data.display_name,
  'unverified',
  'Initial product brief, received 2026-08-01'
from line_data
join public.brands on brands.slug = line_data.brand_slug
on conflict (catalog_key) do update set
  display_name = excluded.display_name,
  verification_status = excluded.verification_status,
  source_reference = excluded.source_reference,
  updated_at = now();

-- Deliberately no colors, product barcodes, SKUs, volumes or brand assets.
-- Import those only from a versioned, cited and verified data source.

commit;
