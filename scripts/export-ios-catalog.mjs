import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const dataRoot = path.join(root, 'native-android', 'app', 'src', 'main', 'java', 'com', 'canvault', 'app', 'data');
const output = path.join(root, 'native-ios', 'CANVAULT', 'Resources', 'CatalogData.json');

const officialText = fs.readFileSync(path.join(dataRoot, 'OfficialCanColorCatalog.kt'), 'utf8');
const verifiedText = fs.readFileSync(path.join(dataRoot, 'VerifiedCatalog.kt'), 'utf8');

function splitArguments(source) {
  const result = [];
  let start = 0;
  let depth = 0;
  let quote = false;
  let escaped = false;
  for (let index = 0; index < source.length; index += 1) {
    const character = source[index];
    if (quote) {
      if (escaped) escaped = false;
      else if (character === '\\') escaped = true;
      else if (character === '"') quote = false;
      continue;
    }
    if (character === '"') quote = true;
    else if (character === '(' || character === '[' || character === '{') depth += 1;
    else if (character === ')' || character === ']' || character === '}') depth -= 1;
    else if (character === ',' && depth === 0) {
      result.push(source.slice(start, index).trim());
      start = index + 1;
    }
  }
  result.push(source.slice(start).trim());
  return result;
}

function extractCalls(source, functionName) {
  const calls = [];
  const needle = `${functionName}(`;
  let cursor = 0;
  while ((cursor = source.indexOf(needle, cursor)) >= 0) {
    const previous = source[cursor - 1];
    if (previous && /[A-Za-z0-9_]/.test(previous)) {
      cursor += needle.length;
      continue;
    }
    const open = cursor + functionName.length;
    let depth = 1;
    let quote = false;
    let escaped = false;
    let index = open + 1;
    for (; index < source.length && depth > 0; index += 1) {
      const character = source[index];
      if (quote) {
        if (escaped) escaped = false;
        else if (character === '\\') escaped = true;
        else if (character === '"') quote = false;
      } else if (character === '"') quote = true;
      else if (character === '(') depth += 1;
      else if (character === ')') depth -= 1;
    }
    if (depth === 0) calls.push(source.slice(open + 1, index - 1));
    cursor = index;
  }
  return calls;
}

function value(token) {
  const trimmed = token.trim();
  if (trimmed === 'null') return null;
  if (trimmed === 'true') return true;
  if (trimmed === 'false') return false;
  if (/^-?\d+$/.test(trimmed)) return Number(trimmed);
  if (trimmed.startsWith('"') && trimmed.endsWith('"')) return JSON.parse(trimmed);
  return trimmed;
}

const sources = extractCalls(officialText, 'OfficialCanColorSource')
  .map(splitArguments)
  .filter((args) => args.length === 4 && args[0].startsWith('"'))
  .map((args) => ({
    id: value(args[0]),
    label: value(args[1]),
    url: value(args[2]),
    extractedShadeCount: value(args[3]),
  }));

const colors = [];
const rawBlockPattern = /RawBlock\("([^"]+)",\s*"([^"]+)",\s*"""([\s\S]*?)"""\.trimIndent\(\)\)/g;
for (const match of officialText.matchAll(rawBlockPattern)) {
  const [, sourceId, lineId, rows] = match;
  for (const rawLine of rows.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line) continue;
    const [colorName, colorCode, productCode, hex] = line.split('|');
    if (!colorName || !hex) throw new Error(`Invalid official color row: ${line}`);
    colors.push({
      lineId,
      colorName,
      colorCode: colorCode || null,
      productCode: productCode || null,
      hex: hex.toUpperCase(),
      sourceId,
    });
  }
}

const products = extractCalls(verifiedText, 'product')
  .map(splitArguments)
  .filter((args) => args.length >= 9 && args[0].startsWith('"'))
  .map((args) => ({
    barcode: value(args[0]),
    barcodeType: 'EAN_13',
    brandId: value(args[1]),
    lineId: value(args[2]),
    colorName: value(args[3]),
    colorCode: value(args[4]),
    customHex: value(args[5]),
    volumeMl: value(args[6]),
    regionCode: args[9] ? value(args[9]) : 'EU',
    sourceName: value(args[7]),
    sourceUrl: value(args[8]),
    verifiedAt: '2026-08-03',
  }));

const prices = extractCalls(verifiedText, 'price')
  .map(splitArguments)
  .filter((args) => args.length >= 3 && args[0].startsWith('"'))
  .map((args) => ({
    lineId: value(args[0]),
    volumeMl: value(args[1]),
    observations: args.slice(2).flatMap((argument) =>
      extractCalls(argument, 'source')
        .map(splitArguments)
        .filter((sourceArgs) => sourceArgs.length === 3)
        .map((sourceArgs) => ({
          retailerName: value(sourceArgs[0]),
          sourceUrl: value(sourceArgs[1]),
          priceEurCents: value(sourceArgs[2]),
          observedAt: '2026-08-03',
          taxIncluded: true,
          shippingIncluded: false,
        })),
    ),
  }));

const catalog = {
  version: '2026.08.03-v2',
  publishedAt: '2026-08-03',
  sources,
  colors,
  products,
  prices,
};

fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, `${JSON.stringify(catalog, null, 2)}\n`, 'utf8');
console.log(`Wrote ${colors.length} colors, ${products.length} products and ${prices.length} prices to ${output}`);
