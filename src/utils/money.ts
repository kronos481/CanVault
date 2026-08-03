export function parsePriceToCents(value: string): number | null {
  const compact = value.trim().replaceAll(/\s/g, '').replace(/[€$£]/g, '');
  if (compact === '') return null;

  const comma = compact.lastIndexOf(',');
  const dot = compact.lastIndexOf('.');
  const decimalIndex = Math.max(comma, dot);
  const hasDecimalPart = decimalIndex >= 0 && compact.length - decimalIndex - 1 <= 2;
  const normalized = hasDecimalPart
    ? `${compact.slice(0, decimalIndex).replaceAll(/[.,]/g, '')}.${compact.slice(decimalIndex + 1)}`
    : compact.replaceAll(/[.,]/g, '');
  const amount = Number(normalized);
  return Number.isFinite(amount) && amount >= 0 ? Math.round(amount * 100) : null;
}

export function formatCurrency(cents: number | null, locale: string, currency: string): string {
  if (cents === null) return '—';
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(cents / 100);
}
