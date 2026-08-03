export function normalizeSearchTerm(value: string): string {
  return value
    .normalize('NFKD')
    .replaceAll(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('de-DE')
    .replaceAll(/[^a-z0-9]+/g, ' ')
    .trim();
}

export function normalizeBarcode(value: string): string | null {
  const digits = value.replaceAll(/\D/g, '');
  return digits.length >= 8 && digits.length <= 14 ? digits : null;
}

export function normalizeColorCode(value: string): string {
  return value.trim().toLocaleUpperCase('de-DE').replaceAll(/\s+/g, ' ');
}
