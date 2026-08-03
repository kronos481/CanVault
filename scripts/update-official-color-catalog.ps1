param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\native-android\app\src\main\java\com\canvault\app\data\OfficialCanColorCatalog.kt')
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Get-Page([string]$Url) {
    (Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 120).Content
}

function Decode([string]$Value) {
    [System.Net.WebUtility]::HtmlDecode($Value).Trim()
}

function New-Record(
    [string]$LineId,
    [string]$Name,
    [string]$ColorCode,
    [string]$ProductCode,
    [string]$Hex,
    [string]$SourceId
) {
    [pscustomobject]@{
        LineId = $LineId
        Name = (Decode $Name) -replace '[\t\r\n]+', ' '
        ColorCode = ((Decode $ColorCode) -replace '[\t\r\n]+', ' ').Trim()
        ProductCode = ((Decode $ProductCode) -replace '[\t\r\n]+', ' ').Trim()
        Hex = $Hex.ToUpperInvariant()
        SourceId = $SourceId
    }
}

function Split-MontanaTitle([string]$LineId, [string]$Title) {
    $titleValue = (Decode $Title) -replace '\s+', ' '
    $patterns = if ($LineId -eq 'montana-cans:montana-black') {
        @(
            '^(BLK TR \d+)\s+(.+)$',
            '^(BLK \d+)\s+(.+)$',
            '^(IN \d+)\s+(.+)$',
            '^(P\d+)\s+(.+)$',
            '^(Outl\. Silver)\s+(.+)$',
            '^(Silver|Copper|Gold)\s+(.+)$'
        )
    } elseif ($LineId -eq 'montana-cans:montana-gold') {
        @(
            '^(METALLIC \d+)\s+(.+)$',
            '^(CLASSIC \d+)\s+(.+)$',
            '^(SHOCK \d+)\s+(.+)$',
            '^(M \d+)\s+(.+)$',
            '^([GFT]\d+)\s+(.+)$'
        )
    } else {
        @('^(ULTRA WIDE)\s+(.+)$')
    }

    foreach ($pattern in $patterns) {
        $match = [regex]::Match($titleValue, $pattern, 'IgnoreCase')
        if ($match.Success) {
            $code = if ($match.Groups[1].Value -eq 'ULTRA WIDE') { '' } else { $match.Groups[1].Value }
            return @($match.Groups[2].Value.Trim(), $code.Trim())
        }
    }
    return @($titleValue, '')
}

$sources = @(
    [pscustomobject]@{ Id='mtn-94'; Line='mtn-montana-colors:mtn-94'; Label='MTN Shop Deutschland'; Url='https://www.mtn-shop.de/mtn-94-ex0140241m'; Parser='Mtn' },
    [pscustomobject]@{ Id='mtn-hardcore'; Line='mtn-montana-colors:mtn-hardcore'; Label='MTN Shop Deutschland'; Url='https://www.mtn-shop.de/mtn-hardcore'; Parser='Mtn' },
    [pscustomobject]@{ Id='mtn-water-based'; Line='mtn-montana-colors:mtn-water-based-400'; Label='MTN Shop Deutschland'; Url='https://www.mtn-shop.de/mtn-water-based-400'; Parser='Mtn' },
    [pscustomobject]@{ Id='mtn-mega'; Line='mtn-montana-colors:mtn-mega'; Label='MTN Shop Deutschland'; Url='https://www.mtn-shop.de/mtn-mega-colors'; Parser='Mtn' },
    [pscustomobject]@{ Id='mtn-vice'; Line='mtn-montana-colors:mtn-vice'; Label='MTN Shop Deutschland'; Url='https://www.mtn-shop.de/mtn-vice-ex014vi0009'; Parser='Mtn' },
    [pscustomobject]@{ Id='montana-black'; Line='montana-cans:montana-black'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-BLACK-400ml/263507'; Parser='Montana' },
    [pscustomobject]@{ Id='montana-black-infra'; Line='montana-cans:montana-black'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-BLACK-400ml-Infra-Colors/352249'; Parser='Montana' },
    [pscustomobject]@{ Id='montana-gold-colors'; Line='montana-cans:montana-gold'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-GOLD-400ml-Colors/284502'; Parser='Montana' },
    [pscustomobject]@{ Id='montana-gold-transparent'; Line='montana-cans:montana-gold'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-GOLD-400ml-Transparent-Colors/419362'; Parser='Montana' },
    [pscustomobject]@{ Id='montana-gold-fluorescent'; Line='montana-cans:montana-gold'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-GOLD-400ml-Fluorescent-Colors/521409'; Parser='Montana' },
    [pscustomobject]@{ Id='montana-gold-chrome'; Line='montana-cans:montana-gold'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-GOLD-400ml-Chrome-Effect-Colors/285936'; Parser='Montana' },
    [pscustomobject]@{ Id='montana-gold-metallic'; Line='montana-cans:montana-gold'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-GOLD-400ml-Metallic-Colors/369759'; Parser='Montana' },
    [pscustomobject]@{ Id='montana-ultra-wide'; Line='montana-cans:montana-ultra-wide'; Label='Montana Cans'; Url='https://www.montana-cans.com/de/Montana-ULTRA-WIDE-750ml/486968'; Parser='Montana' },
    [pscustomobject]@{ Id='molotow-premium'; Line='molotow-belton:molotow-premium'; Label='Molotow Onlineshop'; Url='https://shop.molotow.com/produkt/molotow-premium/'; Parser='Molotow' },
    [pscustomobject]@{ Id='molotow-burner'; Line='molotow-belton:molotow-burner'; Label='Molotow Onlineshop'; Url='https://shop.molotow.com/produkt/burner-spraydose/'; Parser='Molotow' },
    [pscustomobject]@{ Id='flame-blue'; Line='flame:flame-blue'; Label='Molotow Onlineshop'; Url='https://shop.molotow.com/produkt/flame-blue/'; Parser='Molotow' },
    [pscustomobject]@{ Id='flame-orange'; Line='flame:flame-orange'; Label='Molotow Onlineshop'; Url='https://shop.molotow.com/produkt/flame-orange/'; Parser='Molotow' },
    [pscustomobject]@{ Id='molotow-coversall'; Line='molotow-belton:molotow-coversall'; Label='Molotow Onlineshop'; Url='https://shop.molotow.com/produkt/coversall-color/'; Parser='Molotow' },
    [pscustomobject]@{ Id='loop-400'; Line='loop-colors:loop-400-ml'; Label='Loop Colors'; Url='https://loopcolors.com/product/loop-400ml/'; Parser='Loop' },
    [pscustomobject]@{ Id='loop-asphalt-600'; Line='loop-colors:loop-asphalt'; Label='Loop Colors'; Url='https://loopcolors.com/product/asphalt-600-ml/'; Parser='Loop' },
    [pscustomobject]@{ Id='loop-asphalt-400'; Line='loop-colors:loop-asphalt'; Label='Loop Colors'; Url='https://loopcolors.com/product/asphalt-400ml/'; Parser='Loop' },
    [pscustomobject]@{ Id='ironlak-400'; Line='ironlak:ironlak-400-ml'; Label='Ironlak'; Url='https://ironlak.com/product/ironlak-acrlyic-spray-paint-400ml/'; Parser='Ironlak' },
    [pscustomobject]@{ Id='double-a'; Line='double-a:double-a'; Label='Double A Spraypaint'; Url='https://doublea-spraypaint.com/products/double-a-spraypaint-400ml-143-farben'; Parser='DoubleA' }
)

$records = [System.Collections.Generic.List[object]]::new()
$sourceCounts = @{}

foreach ($source in $sources) {
    Write-Host "Lade $($source.Label): $($source.Url)"
    $html = Get-Page $source.Url
    $before = $records.Count

    switch ($source.Parser) {
        'Mtn' {
            $matches = [regex]::Matches($html, '<span class="search-item d-none">\s*([^,<]+?),\s*(#[0-9A-Fa-f]{6}),\s*([^,<]*?),\s*([^<]*?)\s*</span>', 'IgnoreCase')
            foreach ($match in $matches) {
                $records.Add((New-Record $source.Line $match.Groups[1].Value $match.Groups[4].Value $match.Groups[3].Value $match.Groups[2].Value $source.Id))
            }
        }
        'Montana' {
            $matches = [regex]::Matches($html, 'data-color-title="([^"]+)"[\s\S]{0,2000}?data-hex="(#[0-9A-Fa-f]{6})"', 'IgnoreCase')
            foreach ($match in $matches) {
                $parts = Split-MontanaTitle $source.Line $match.Groups[1].Value
                $records.Add((New-Record $source.Line $parts[0] $parts[1] '' $match.Groups[2].Value $source.Id))
            }
        }
        'Molotow' {
            $matches = [regex]::Matches($html, '<div class="subproduct-row[^>]*data-preview="(#[0-9A-Fa-f]{6})"[^>]*>[\s\S]*?<span class="art-name">([^<]+)</span>[\s\S]*?<span class="art-nr">([^<]+)</span>', 'IgnoreCase')
            foreach ($match in $matches) {
                $rawName = Decode $match.Groups[2].Value
                $nameMatch = [regex]::Match($rawName, '^(#[0-9]{3}(?:-[0-9]+)?|F[BO]-[0-9]+)\s+(.+)$', 'IgnoreCase')
                $name = if ($nameMatch.Success) { $nameMatch.Groups[2].Value } else { $rawName }
                $code = if ($nameMatch.Success) { $nameMatch.Groups[1].Value } else { '' }
                $records.Add((New-Record $source.Line $name $code $match.Groups[3].Value $match.Groups[1].Value $source.Id))
            }
        }
        'Loop' {
            $matches = [regex]::Matches($html, 'class="color-item"[\s\S]{0,800}?data-name="([^"]+)"[\s\S]{0,400}?data-code="([^"]+)"[\s\S]{0,500}?data-hex="(#[0-9A-Fa-f]{6})"', 'IgnoreCase')
            foreach ($match in $matches) {
                $records.Add((New-Record $source.Line $match.Groups[1].Value $match.Groups[2].Value '' $match.Groups[3].Value $source.Id))
            }
        }
        'Ironlak' {
            $matches = [regex]::Matches($html, 'data-rtwpvs-tooltip="([^"]+)"[\s\S]{0,500}?style="background-color:\s*(#[0-9A-Fa-f]{6});', 'IgnoreCase')
            foreach ($match in $matches) {
                $records.Add((New-Record $source.Line $match.Groups[1].Value '' '' $match.Groups[2].Value $source.Id))
            }
        }
        'DoubleA' {
            $matches = [regex]::Matches($html, 'class="variant-item"\s*style="background-color:\s*(#[0-9A-Fa-f]{6});[^"]*"\s*>\s*<p>([^<]+)</p>', 'IgnoreCase')
            foreach ($match in $matches) {
                $rawName = Decode $match.Groups[2].Value
                $nameMatch = [regex]::Match($rawName, '^(DA-[0-9]+)\s+(.+)$', 'IgnoreCase')
                if ($nameMatch.Success) {
                    $records.Add((New-Record $source.Line $nameMatch.Groups[2].Value $nameMatch.Groups[1].Value '' $match.Groups[1].Value $source.Id))
                }
            }
        }
    }

    $sourceCounts[$source.Id] = $records.Count - $before
    if ($sourceCounts[$source.Id] -eq 0) {
        throw "Keine Farbvarianten aus $($source.Url) extrahiert."
    }
}

$invalid = $records | Where-Object { $_.Hex -notmatch '^#[0-9A-F]{6}$' -or [string]::IsNullOrWhiteSpace($_.Name) }
if ($invalid.Count -gt 0) {
    throw "Der Katalog enthält $($invalid.Count) ungültige Datensätze."
}

$duplicates = $records | Group-Object LineId, ColorCode, Name | Where-Object Count -gt 1
if ($duplicates.Count -gt 0) {
    Write-Warning "$($duplicates.Count) Dublettengruppen werden anhand Linie, Code und Name zusammengeführt."
}
$records = $records | Sort-Object LineId, ColorCode, Name, SourceId -Unique
foreach ($source in $sources) {
    $sourceCounts[$source.Id] = @($records | Where-Object SourceId -eq $source.Id).Count
}

function Escape-Kotlin([string]$Value) {
    $Value.Replace('\\', '\\\\').Replace('"', '\"').Replace('$', '\$')
}

$blocks = $records | Group-Object SourceId, LineId
$builder = [System.Text.StringBuilder]::new()
[void]$builder.AppendLine('// Generated by scripts/update-official-color-catalog.ps1 on 2026-08-03.')
[void]$builder.AppendLine('// Values are exact digital swatches published in the linked manufacturer product pages.')
[void]$builder.AppendLine('package com.canvault.app.data')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('import java.text.Normalizer')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('data class OfficialCanColorSource(')
[void]$builder.AppendLine('    val id: String,')
[void]$builder.AppendLine('    val label: String,')
[void]$builder.AppendLine('    val url: String,')
[void]$builder.AppendLine('    val extractedShadeCount: Int,')
[void]$builder.AppendLine(')')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('data class OfficialCanColor(')
[void]$builder.AppendLine('    val lineId: String,')
[void]$builder.AppendLine('    val colorName: String,')
[void]$builder.AppendLine('    val colorCode: String?,')
[void]$builder.AppendLine('    val productCode: String?,')
[void]$builder.AppendLine('    val hex: String,')
[void]$builder.AppendLine('    val sourceId: String,')
[void]$builder.AppendLine(')')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('object OfficialCanColorCatalog {')
[void]$builder.AppendLine('    const val publishedAt = "2026-08-03"')
[void]$builder.AppendLine('    const val accuracyNote = "Digitaler Herstellerwert; Lack kann je nach Untergrund, Licht und Display abweichen."')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    val sources = listOf(')
foreach ($source in $sources) {
    $label = Escape-Kotlin $source.Label
    $url = Escape-Kotlin $source.Url
    [void]$builder.AppendLine(('        OfficialCanColorSource("{0}", "{1}", "{2}", {3}),' -f $source.Id, $label, $url, $sourceCounts[$source.Id]))
}
[void]$builder.AppendLine('    )')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    private data class RawBlock(val sourceId: String, val lineId: String, val rows: String)')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    private val rawBlocks = listOf(')
foreach ($block in $blocks) {
    $first = $block.Group[0]
    [void]$builder.AppendLine(('        RawBlock("{0}", "{1}", """' -f $first.SourceId, $first.LineId))
    foreach ($record in $block.Group) {
        $values = @($record.Name, $record.ColorCode, $record.ProductCode, $record.Hex) | ForEach-Object { ($_ -replace '[|\r\n]', ' ').Trim() }
        [void]$builder.AppendLine('            ' + ($values -join '|'))
    }
    [void]$builder.AppendLine('        """.trimIndent()),')
}
[void]$builder.AppendLine('    )')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    val colors: List<OfficialCanColor> by lazy {')
[void]$builder.AppendLine('        rawBlocks.flatMap { block ->')
[void]$builder.AppendLine('            block.rows.lineSequence().filter(String::isNotBlank).map { row ->')
[void]$builder.AppendLine('                val fields = row.split(''|'', limit = 4)')
[void]$builder.AppendLine('                OfficialCanColor(')
[void]$builder.AppendLine('                    lineId = block.lineId,')
[void]$builder.AppendLine('                    colorName = fields[0],')
[void]$builder.AppendLine('                    colorCode = fields[1].ifBlank { null },')
[void]$builder.AppendLine('                    productCode = fields[2].ifBlank { null },')
[void]$builder.AppendLine('                    hex = fields[3],')
[void]$builder.AppendLine('                    sourceId = block.sourceId,')
[void]$builder.AppendLine('                )')
[void]$builder.AppendLine('            }.toList()')
[void]$builder.AppendLine('        }')
[void]$builder.AppendLine('    }')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    private val colorsByLine by lazy { colors.groupBy(OfficialCanColor::lineId) }')
[void]$builder.AppendLine('    private val sourceById by lazy { sources.associateBy(OfficialCanColorSource::id) }')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    fun colorsForLine(lineId: String): List<OfficialCanColor> = colorsByLine[lineId].orEmpty()')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    fun sourceFor(color: OfficialCanColor): OfficialCanColorSource? = sourceById[color.sourceId]')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    fun find(lineId: String?, colorName: String?, colorCode: String?): OfficialCanColor? {')
[void]$builder.AppendLine('        if (lineId.isNullOrBlank()) return null')
[void]$builder.AppendLine('        val candidates = colorsForLine(lineId)')
[void]$builder.AppendLine('        if (candidates.isEmpty()) return null')
[void]$builder.AppendLine('        val normalizedCode = normalizeCode(colorCode)')
[void]$builder.AppendLine('        if (normalizedCode.isNotEmpty()) {')
[void]$builder.AppendLine('            candidates.firstOrNull { color ->')
[void]$builder.AppendLine('                normalizeCode(color.colorCode) == normalizedCode || normalizeCode(color.productCode) == normalizedCode')
[void]$builder.AppendLine('            }?.let { return it }')
[void]$builder.AppendLine('        }')
[void]$builder.AppendLine('        val normalizedName = normalizeName(colorName)')
[void]$builder.AppendLine('        if (normalizedName.isEmpty()) return null')
[void]$builder.AppendLine('        return candidates.firstOrNull { color -> color.nameAliases().any { normalizeName(it) == normalizedName } }')
[void]$builder.AppendLine('    }')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    fun search(lineId: String, query: String, limit: Int = 8): List<OfficialCanColor> {')
[void]$builder.AppendLine('        val normalizedQuery = normalizeName(query)')
[void]$builder.AppendLine('        if (normalizedQuery.isEmpty()) return colorsForLine(lineId).take(limit)')
[void]$builder.AppendLine('        return colorsForLine(lineId).asSequence().mapNotNull { color ->')
[void]$builder.AppendLine('            val aliases = color.nameAliases().map(::normalizeName)')
[void]$builder.AppendLine('            val rank = when {')
[void]$builder.AppendLine('                aliases.any { it == normalizedQuery } -> 0')
[void]$builder.AppendLine('                aliases.any { it.startsWith(normalizedQuery) } -> 1')
[void]$builder.AppendLine('                aliases.any { it.contains(normalizedQuery) } -> 2')
[void]$builder.AppendLine('                else -> return@mapNotNull null')
[void]$builder.AppendLine('            }')
[void]$builder.AppendLine('            color to rank')
[void]$builder.AppendLine('        }.sortedWith(compareBy<Pair<OfficialCanColor, Int>> { it.second }.thenBy { it.first.colorName }).take(limit).map { it.first }.toList()')
[void]$builder.AppendLine('    }')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    private fun OfficialCanColor.nameAliases(): List<String> = buildList {')
[void]$builder.AppendLine('        add(colorName)')
[void]$builder.AppendLine('        colorCode?.let { code ->')
[void]$builder.AppendLine('            add("$code $colorName")')
[void]$builder.AppendLine('            code.replace(Regex("[0-9#\\s-]+$"), "").trim().takeIf(String::isNotBlank)?.let { add("$it $colorName") }')
[void]$builder.AppendLine('        }')
[void]$builder.AppendLine('    }')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    private fun normalizeName(value: String?): String = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFKD)')
[void]$builder.AppendLine('        .replace(Regex("\\p{M}+"), "")')
[void]$builder.AppendLine('        .lowercase()')
[void]$builder.AppendLine('        .replace(Regex("[^a-z0-9]+"), " ")')
[void]$builder.AppendLine('        .trim()')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('    private fun normalizeCode(value: String?): String = value.orEmpty().lowercase().replace(Regex("[^a-z0-9#]+"), "")')
[void]$builder.AppendLine('}')

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($resolvedOutput)) | Out-Null
[System.IO.File]::WriteAllText($resolvedOutput, $builder.ToString(), [System.Text.UTF8Encoding]::new($false))

Write-Host "Geschrieben: $resolvedOutput"
Write-Host "Verifizierte Digitalfarben: $($records.Count)"
$records | Group-Object LineId | Sort-Object Name | ForEach-Object { Write-Host ("  {0}: {1}" -f $_.Name, $_.Count) }
