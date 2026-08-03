begin;
create extension if not exists pgtap with schema extensions;
select plan(15);

select has_table('public', 'user_cans', 'user_cans exists');
select has_table('public', 'can_events', 'append-only can_events exists');
select has_table('public', 'sync_operations', 'offline sync operations exist');

select is((select count(*) from public.brands), 15::bigint, 'seed contains all 15 requested brands');
select is((select count(*) from public.can_lines), 38::bigint, 'catalog contains all 38 requested can lines including Burner 600 variants');
select is((select count(*) from public.product_barcodes where verified), 19::bigint, 'verified catalog contains 19 source-cited GTIN mappings');
select is((select count(*) from public.market_price_observations where active), 40::bigint, 'price catalog contains 40 dated active observations');
select isnt(
  (select id from public.brands where slug = 'mtn-montana-colors'),
  (select id from public.brands where slug = 'montana-cans'),
  'MTN / Montana Colors and Montana Cans use distinct records'
);
select is(
  (select count(*) from public.colors),
  0::bigint,
  'unverified color palettes are not invented'
);

select is(
  (
    select count(*)
    from pg_class
    join pg_namespace on pg_namespace.oid = pg_class.relnamespace
    where pg_namespace.nspname = 'public'
      and pg_class.relname = any(array[
        'profiles','retailers','storage_locations','purchases','purchase_items','can_batches',
        'user_cans','can_events','projects','project_can_usage','shopping_list_items',
        'minimum_stock_rules','user_favorites','user_tags','can_tag_assignments','user_settings',
        'catalog_corrections','sync_operations','notifications'
      ])
      and not pg_class.relrowsecurity
  ),
  0::bigint,
  'RLS is enabled on every user-owned table'
);

select is(
  (
    select count(*) from pg_policies
    where schemaname = 'public' and tablename = 'can_events' and cmd in ('UPDATE', 'DELETE', 'ALL')
  ),
  0::bigint,
  'can_events exposes no mutation policy'
);

insert into auth.users (id, email)
values
  ('11111111-1111-1111-1111-111111111111', 'rls-one@example.invalid'),
  ('22222222-2222-2222-2222-222222222222', 'rls-two@example.invalid');

insert into public.user_cans (
  id, user_id, custom_brand_name, custom_line_name, custom_color_name, status
)
values
  ('rls-can-one', '11111111-1111-1111-1111-111111111111', 'Custom', 'Line', 'Color one', 'in_stock'),
  ('rls-can-two', '22222222-2222-2222-2222-222222222222', 'Custom', 'Line', 'Color two', 'in_stock');

set local role authenticated;
select set_config(
  'request.jwt.claims',
  '{"sub":"11111111-1111-1111-1111-111111111111","role":"authenticated"}',
  true
);

select is((select count(*) from public.user_cans), 1::bigint, 'user sees only own cans');
select is((select id from public.user_cans), 'rls-can-one', 'the visible can belongs to the JWT user');

select is(
  (with updated as (
    update public.user_cans set status = 'opened' where id = 'rls-can-two' returning 1
  ) select count(*) from updated),
  0::bigint,
  'user cannot update another user can'
);

select throws_ok(
  $$update public.user_cans set status = 'opened' where id = 'rls-can-one'; update public.user_cans set status = 'sold' where id = 'rls-can-one'$$,
  '23514',
  'invalid can status transition: opened -> sold',
  'invalid status transitions are rejected'
);

select * from finish();
rollback;
