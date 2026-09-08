-- Run in Supabase SQL Editor. SELECT only: no user records, no changes.
-- Share the resulting schema_report cell, not any credentials.
select jsonb_pretty(jsonb_build_object(
  'columns', (select jsonb_agg(jsonb_build_object(
    'table', c.relname, 'column', a.attname,
    'type', pg_catalog.format_type(a.atttypid, a.atttypmod),
    'not_null', a.attnotnull, 'rls_enabled', c.relrowsecurity
  ) order by c.relname, a.attnum)
  from pg_catalog.pg_class c
  join pg_catalog.pg_namespace n on n.oid = c.relnamespace
  join pg_catalog.pg_attribute a on a.attrelid = c.oid
  where n.nspname = 'public' and c.relkind in ('r','p')
    and a.attnum > 0 and not a.attisdropped),
  'constraints', (select jsonb_agg(jsonb_build_object(
    'table', c.relname, 'name', con.conname,
    'definition', pg_catalog.pg_get_constraintdef(con.oid)
  ) order by c.relname, con.conname)
  from pg_catalog.pg_constraint con
  join pg_catalog.pg_class c on c.oid = con.conrelid
  join pg_catalog.pg_namespace n on n.oid = c.relnamespace
  where n.nspname = 'public'),
  'policies', (select jsonb_agg(to_jsonb(p)) from pg_catalog.pg_policies p
    where p.schemaname = 'public'),
  'triggers', (select jsonb_agg(jsonb_build_object(
    'schema', n.nspname, 'table', c.relname, 'name', t.tgname,
    'definition', pg_catalog.pg_get_triggerdef(t.oid)
  )) from pg_catalog.pg_trigger t
  join pg_catalog.pg_class c on c.oid = t.tgrelid
  join pg_catalog.pg_namespace n on n.oid = c.relnamespace
  where not t.tgisinternal and (n.nspname = 'public'
    or (n.nspname = 'auth' and c.relname = 'users')))
)) as schema_report;
