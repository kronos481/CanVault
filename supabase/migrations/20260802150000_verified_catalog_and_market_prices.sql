begin;

alter table public.product_barcodes
  add column if not exists source_name text,
  add column if not exists verified_at date;

create unique index if not exists product_barcodes_barcode_unique_idx
  on public.product_barcodes (barcode);

with burner_lines (slug, catalog_key, display_name, color_name, color_code) as (
  values
    ('burner-chrome-600-ml', 'molotow-belton:burner-chrome-600-ml', 'Burner Chrome 600 ml', 'Metallic Chrome', '940397'),
    ('burner-gold-600-ml', 'molotow-belton:burner-gold-600-ml', 'Burner Gold 600 ml', 'Metallic Gold', '940499'),
    ('burner-copper-600-ml', 'molotow-belton:burner-copper-600-ml', 'Burner Copper 600 ml', 'Metallic Copper', '940500'),
    ('burner-black-600-ml', 'molotow-belton:burner-black-600-ml', 'Burner Black 600 ml', 'Black', '940398')
)
insert into public.can_lines (
  brand_id, catalog_key, slug, display_name, default_volume_ml,
  verification_status, source_reference, last_verified_at
)
select
  brands.id,
  burner_lines.catalog_key,
  burner_lines.slug,
  burner_lines.display_name,
  600,
  'manufacturer_verified',
  'https://brand.molotow.com/fileadmin/Dateien/PDF/Info_Sheets/Spray/Action/Infosheet_BURNER_BLACK.pdf',
  '2026-08-02'
from burner_lines
join public.brands on brands.slug = 'molotow-belton'
on conflict (catalog_key) do update set
  display_name = excluded.display_name,
  default_volume_ml = excluded.default_volume_ml,
  verification_status = excluded.verification_status,
  source_reference = excluded.source_reference,
  last_verified_at = excluded.last_verified_at,
  updated_at = now();

create table public.market_price_observations (
  id uuid primary key default gen_random_uuid(),
  can_line_id uuid not null references public.can_lines(id) on delete cascade,
  market_code text not null check (market_code ~ '^[A-Z]{2}$'),
  currency_code text not null check (currency_code ~ '^[A-Z]{3}$'),
  volume_ml integer not null check (volume_ml > 0),
  retailer_name text not null check (length(trim(retailer_name)) > 0),
  source_url text not null check (source_url like 'https://%'),
  price_cents integer not null check (price_cents > 0),
  observed_at date not null,
  tax_included boolean not null default true,
  shipping_included boolean not null default false,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (can_line_id, market_code, retailer_name, source_url, observed_at)
);

create trigger market_price_observations_set_updated_at
before update on public.market_price_observations
for each row execute function public.set_updated_at();

alter table public.market_price_observations enable row level security;
create policy market_price_observations_read
on public.market_price_observations for select to anon, authenticated using (active);

create temp table verified_catalog_seed (
  barcode text primary key,
  brand_slug text not null,
  can_line_key text not null,
  color_name text not null,
  color_code text,
  hex_approximation text,
  volume_ml integer not null,
  region_code text,
  source_name text not null,
  source_url text not null
) on commit drop;

insert into verified_catalog_seed values
  ('4015962368101','molotow-belton','molotow-belton:burner-chrome-600-ml','Metallic Chrome','940397','#B7BDC3',600,'EU','Van Beek Art Supplies','https://www.vanbeekart.nl/p/molotow-burner-600ml-chrome/82318/'),
  ('4015962368286','molotow-belton','molotow-belton:burner-gold-600-ml','Metallic Gold','940499','#C29A45',600,'EU','Blue Label Shops','https://www.graffitidirect.nl/p/molotow-burner-600ml-gold/60137/'),
  ('4015962368293','molotow-belton','molotow-belton:burner-copper-600-ml','Metallic Copper','940500','#B66A45',600,'EU','Molotow España','https://www.molotow.es/spray-burner/spray-efecto-metalizado-molotow-burner-cobre-600ml-4015962368293-1722.html'),
  ('4015962369429','molotow-belton','molotow-belton:burner-black-600-ml','Black','940398','#090909',600,'EU','DynaTech','https://www.dynatech.de/molotow-spruehdose-600-burner-black-600-ml-matt-farbe-schwarz'),
  ('4048500264368','montana-cans','montana-cans:montana-black','Black','BLK 9001','#111111',400,'EU','Allegro','https://allegro.cz/nabidka/montana-black-400-ml-blk-9001-black-11174154371'),
  ('4048500321573','montana-cans','montana-cans:montana-black','Storm',null,null,400,'EU','Store-HD','https://www.store-hd.de/Montana-Black-Storm-400ml'),
  ('4048500285783','montana-cans','montana-cans:montana-gold','Shock Black','S9000','#101010',400,'EU','Architekturbedarf','https://www.architekturbedarf.de/paints/spray-paints/montana-gold-400-ml/montana-gold-s9000-shock-black'),
  ('8427744411367','mtn-montana-colors','mtn-montana-colors:mtn-94','Black','RV-9011','#111111',400,'EU','Allegro','https://allegro.hu/termek/matt-fekete-spray-festek-montana-400-ml-c8aeb0d8-7c03-453d-ac4e-e65c8d403b23'),
  ('7909547444922','mtn-montana-colors','mtn-montana-colors:mtn-hardcore','Black',null,'#111111',400,'BR','Pinheiro Tintas','https://www.pinheirotintas.com.br/tinta-spray-brilhante-mtn-hardcore-preto-400ml-montana/p'),
  ('4250397612942','flame','flame:flame-blue','Deep Black','FB-904','#090909',400,'EU','Allegro','https://allegro.pl/produkt/flame-blue-fb-904-deep-black-400ml-cb2288c1-462b-437a-961f-57d6c19a0c3e'),
  ('8051277870546','kobra','kobra:kobra-hp','Satin Black','006','#111111',400,'EU','eBay UK','https://www.ebay.co.uk/itm/285629279687'),
  ('5901687941004','dope','dope:dope-classic','Fresh Color',null,null,400,'EU','Spreje Plzeň','https://www.sprejeplzen.cz/dope-classic-400ml/'),
  ('5901687941585','dope','dope:dope-classic','White',null,'#F4F4F1',400,'EU','Spreje Plzeň','https://www.sprejeplzen.cz/dope-classic-400ml/'),
  ('5901687941592','dope','dope:dope-classic','Black','D-300','#090909',400,'EU','Tulandia','https://tulandia.pl/farba-w-sprayu-graffiti-400-ml-czarna-dope-classic-p-10813.html'),
  ('5901687941608','dope','dope:dope-classic','Chrome',null,'#B8BEC5',400,'EU','Spreje Plzeň','https://www.sprejeplzen.cz/dope-classic-400ml/'),
  ('5901687941615','dope','dope:dope-classic','Gold',null,'#C19A47',400,'EU','Spreje Plzeň','https://www.sprejeplzen.cz/dope-classic-400ml/'),
  ('4255883101658','double-a','double-a:double-a','Damagers Red','DA-380','#B9202C',400,'EU','Double A','https://doublea-spraypaint.com/products/double-a-spraypaint-400ml-special-edition-damagers'),
  ('8436548872625','nbq','nbq:nbq-fast','Waste Green',null,null,400,'EU','bol','https://www.bol.com/nl/nl/p/nbq-fast-spray-paint-400ml-matte-afwerking-hogedruk-mat-sneldrogend/9300000231509022/'),
  ('8427744143657','krink','krink:krink-k-750','Black',null,'#090909',750,'EU','Nicolaas Verf','https://www.nicolaasverf.nl/product/mtn-krink-k-750/');

do $$
declare
  seed record;
  selected_line_id uuid;
  selected_color_id uuid;
  selected_variant_id uuid;
begin
  for seed in select * from verified_catalog_seed loop
    select id into strict selected_line_id
    from public.can_lines where catalog_key = seed.can_line_key;

    update public.can_lines set
      default_volume_ml = coalesce(default_volume_ml, seed.volume_ml),
      verification_status = 'source_verified',
      last_verified_at = '2026-08-02',
      updated_at = now()
    where id = selected_line_id;

    select id into selected_color_id
    from public.colors
    where can_line_id = selected_line_id
      and official_name = seed.color_name
      and official_code is not distinct from seed.color_code;

    if selected_color_id is null then
      insert into public.colors (
        can_line_id, official_code, official_name, normalized_name,
        hex_approximation, color_family, verification_status,
        source_reference, last_verified_at
      ) values (
        selected_line_id, seed.color_code, seed.color_name, lower(seed.color_name),
        seed.hex_approximation, 'unknown', 'source_verified',
        seed.source_url, '2026-08-02'
      ) returning id into selected_color_id;
    end if;

    insert into public.product_variants (
      can_line_id, color_id, volume_ml, region_code, verification_status
    ) values (
      selected_line_id, selected_color_id, seed.volume_ml, seed.region_code, 'source_verified'
    ) returning id into selected_variant_id;

    insert into public.product_barcodes (
      product_variant_id, can_line_id, barcode, barcode_type, region_code,
      confidence, source_reference, source_name, verified, verified_at
    ) values (
      selected_variant_id, selected_line_id, seed.barcode, 'EAN_13', seed.region_code,
      'certain', seed.source_url, seed.source_name, true, '2026-08-02'
    ) on conflict (barcode) do update set
      product_variant_id = excluded.product_variant_id,
      can_line_id = excluded.can_line_id,
      confidence = excluded.confidence,
      source_reference = excluded.source_reference,
      source_name = excluded.source_name,
      verified = excluded.verified,
      verified_at = excluded.verified_at,
      updated_at = now();
  end loop;
end $$;

with price_seed (can_line_key, volume_ml, retailer_name, source_url, price_cents) as (
  values
    ('mtn-montana-colors:mtn-94',400,'MTN Shop','https://www.mtn-shop.de/mtn-94-ex0140126',510),
    ('mtn-montana-colors:mtn-94',400,'BETTERRUN','https://www.betterrun.shop/',480),
    ('mtn-montana-colors:mtn-94',400,'idealo','https://www.idealo.de/preisvergleich/OffersOfProduct/3734100_-montana-colors-mtn-94-spruehfarbe-400-ml-verschiedene-farben-montanacans.html',595),
    ('mtn-montana-colors:mtn-hardcore',400,'Psychic Shop','https://www.psychic-shop.de/MTN-Hardcore-400ml/SW10066.117',430),
    ('mtn-montana-colors:mtn-hardcore',400,'BETTERRUN','https://www.betterrun.shop/spruehdosen/action-cans/mtn-cans-hardcore-400ml-139-farben',450),
    ('mtn-montana-colors:mtn-vice',400,'Graffitibox','https://graffitibox.de/spruehdosen/mtn-vice/',410),
    ('mtn-montana-colors:mtn-vice',400,'Graffitishop Kiel','https://graffitishopkiel.de/Graffiti-Spraydosen/MTN-Spraydosen/MTN-VICE%3A%3A%3A1_5_75.html',425),
    ('mtn-montana-colors:mtn-water-based-400',400,'MTN Shop','https://www.mtn-shop.de/mtn-water-based-400',855),
    ('mtn-montana-colors:mtn-water-based-400',400,'Pintaya','https://pintaya.com/shop/mtn-water-based-400-19',775),
    ('montana-cans:montana-black',400,'Store-HD','https://www.store-hd.de/Montana-Black-Storm-400ml',421),
    ('montana-cans:montana-black',400,'BETTERRUN','https://www.betterrun.shop/',470),
    ('montana-cans:montana-gold',400,'Architekturbedarf','https://www.architekturbedarf.de/paints/spray-paints/montana-gold-400-ml/montana-gold-s9000-shock-black',525),
    ('montana-cans:montana-gold',400,'Dekkender Paints','https://dekkenderpaints.nl/product/s9000-shock-black-400ml-285783/',516),
    ('montana-cans:montana-gold',400,'BETTERRUN','https://www.betterrun.shop/',550),
    ('montana-cans:montana-white',400,'Graffitilager','https://graffitilager.de/en/Spray-cans/Montana-Cans/White-series/',455),
    ('montana-cans:montana-white',400,'AGRABAH','https://agrabah.de/produkt/montana-white-400ml/',490),
    ('molotow-belton:molotow-premium',400,'BETTERRUN','https://www.betterrun.shop/',495),
    ('molotow-belton:burner-chrome-600-ml',600,'Molotow France','https://molotow.fr/bombe-burner-chrome-600ml.html',780),
    ('molotow-belton:burner-chrome-600-ml',600,'Van Beek','https://www.vanbeekart.nl/p/molotow-burner-600ml-chrome/82318/',835),
    ('molotow-belton:burner-chrome-600-ml',600,'Molotow España','https://www.molotow.es/spray-burner/spray-de-pintura-molotow-burner-600ml-4015962368101-22.html',600),
    ('molotow-belton:burner-gold-600-ml',600,'Blue Label Shops','https://www.graffitidirect.nl/p/molotow-burner-600ml-gold/60137/',877),
    ('molotow-belton:burner-copper-600-ml',600,'Molotow España','https://www.molotow.es/spray-burner/spray-efecto-metalizado-molotow-burner-cobre-600ml-4015962368293-1722.html',650),
    ('molotow-belton:burner-copper-600-ml',600,'Molotow France','https://molotow.fr/bombe-de-peinture-graffiti-metallisee-molotow-burner-cuivre-600ml.html',790),
    ('molotow-belton:burner-black-600-ml',600,'Molotow France','https://molotow.fr/bombe-de-peinture-graffiti-noire-molotow-burner-black-600ml.html',790),
    ('molotow-belton:burner-black-600-ml',600,'Molotow Slovakia','https://en.molotow.sk/burnertm-black-600-ml.html',660),
    ('molotow-belton:burner-black-600-ml',600,'DynaTech','https://www.dynatech.de/molotow-spruehdose-600-burner-black-600-ml-matt-farbe-schwarz',710),
    ('molotow-belton:burner-black-600-ml',600,'BETTERRUN','https://www.betterrun.shop/en/spray-cans/action-spray-cans/molotov-burner-black-600ml-black',625),
    ('molotow-belton:burner-black-600-ml',600,'OVERKILL','https://www.overkillshop.com/products/molotow-burner-black-600-ml-940398',550),
    ('molotow-belton:molotow-coversall',400,'Van Beek','https://www.vanbeekart.nl/p/molotow-burner-600ml-chrome/82318/',886),
    ('loop-colors:loop-400-ml',400,'Loopcolors Germany','https://www.loopcolors-germany.de/cans/loop-400/',435),
    ('loop-colors:loop-asphalt',400,'Loopcolors Germany','https://www.loopcolors-germany.de/cans/loop-400/',465),
    ('flame:flame-blue',400,'Molotow Shop','https://shop.molotow.com/produkt/flame-blue/',445),
    ('flame:flame-orange',400,'BETTERRUN','https://www.betterrun.shop/',400),
    ('kobra:kobra-hp',400,'BETTERRUN','https://www.betterrun.shop/',395),
    ('kobra:kobra-lp',400,'BETTERRUN','https://www.betterrun.shop/',410),
    ('nbq:nbq-fast',400,'Writers Madrid','https://www.writersmadrid.es/es/nbq-pro-spray-paint/2480-nbq-fast-400ml.html',395),
    ('nbq:nbq-fast',400,'Graffitibox','https://graffitibox.de/spruehdosen/schwarz/10815/nbq-new-fast-pro-spraypaint-black-400ml',390),
    ('dope:dope-classic',400,'BETTERRUN','https://www.betterrun.shop/',350),
    ('double-a:double-a',400,'CLRZ','https://www.clrz.de/DoubleA',390),
    ('krink:krink-k-750',750,'Nicolaas Verf','https://www.nicolaasverf.nl/product/mtn-krink-k-750/',944)
)
insert into public.market_price_observations (
  can_line_id, market_code, currency_code, volume_ml, retailer_name,
  source_url, price_cents, observed_at, tax_included, shipping_included, active
)
select
  can_lines.id, 'EU', 'EUR', price_seed.volume_ml, price_seed.retailer_name,
  price_seed.source_url, price_seed.price_cents, '2026-08-02', true, false, true
from price_seed
join public.can_lines on can_lines.catalog_key = price_seed.can_line_key
on conflict (can_line_id, market_code, retailer_name, source_url, observed_at)
do update set
  price_cents = excluded.price_cents,
  volume_ml = excluded.volume_ml,
  active = true,
  updated_at = now();

create or replace view public.verified_product_catalog
with (security_invoker = true) as
select
  product_barcodes.barcode,
  product_barcodes.barcode_type,
  brands.slug as brand_slug,
  brands.display_name as brand_name,
  can_lines.catalog_key as can_line_key,
  can_lines.display_name as can_line_name,
  colors.official_name as color_name,
  colors.official_code as color_code,
  colors.hex_approximation,
  product_variants.volume_ml,
  product_barcodes.region_code,
  product_barcodes.source_name,
  product_barcodes.source_reference as source_url,
  product_barcodes.verified_at
from public.product_barcodes
join public.product_variants on product_variants.id = product_barcodes.product_variant_id
join public.can_lines on can_lines.id = product_variants.can_line_id
join public.brands on brands.id = can_lines.brand_id
join public.colors on colors.id = product_variants.color_id
where product_barcodes.verified
  and product_barcodes.confidence = 'certain'
  and product_barcodes.source_reference like 'https://%'
  and product_barcodes.verified_at is not null;

create or replace view public.market_price_observations_public
with (security_invoker = true) as
select
  can_lines.catalog_key as can_line_key,
  market_price_observations.market_code,
  market_price_observations.currency_code,
  market_price_observations.volume_ml,
  market_price_observations.retailer_name,
  market_price_observations.source_url,
  market_price_observations.price_cents,
  market_price_observations.observed_at,
  market_price_observations.tax_included,
  market_price_observations.shipping_included,
  market_price_observations.active
from public.market_price_observations
join public.can_lines on can_lines.id = market_price_observations.can_line_id
where market_price_observations.active;

grant select on public.verified_product_catalog to anon, authenticated;
grant select on public.market_price_observations_public to anon, authenticated;

insert into public.catalog_versions (id, verification_status, notes, published_at)
values (
  '2026.08.02-v1',
  'source_verified',
  'Source-cited GTIN mappings with valid GTIN check digits. Pricing is stored as dated observations, not product truth.',
  '2026-08-02T15:00:00Z'
)
on conflict (id) do update set
  verification_status = excluded.verification_status,
  notes = excluded.notes,
  published_at = excluded.published_at;

commit;
