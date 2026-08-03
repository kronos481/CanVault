import { z } from 'zod';

const optionalSupabaseEnvSchema = z
  .object({
    url: z.string().url().optional(),
    anonKey: z.string().min(20).optional(),
  })
  .refine((value) => Boolean(value.url) === Boolean(value.anonKey), {
    message: 'Supabase URL and anon key must be configured together.',
  });

const parsed = optionalSupabaseEnvSchema.safeParse({
  url: process.env.EXPO_PUBLIC_SUPABASE_URL || undefined,
  anonKey: process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY || undefined,
});

export const supabaseEnvironment =
  parsed.success && parsed.data.url && parsed.data.anonKey
    ? { url: parsed.data.url, anonKey: parsed.data.anonKey }
    : null;

export const environmentError = parsed.success
  ? null
  : parsed.error.issues.map((issue) => issue.message).join(' ');
