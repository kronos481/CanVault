package com.canvault.app.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class PaintRole(val displayName: String) {
    BACKGROUND("Background"),
    SECOND_OUTLINE("Second Outline"),
    OUTLINE("Outline"),
    FILL_SHADOW("Fill-Schatten"),
    FILL("Fill"),
    FILL_SECONDARY("Fill 2"),
    FILL_TERTIARY("Fill 3"),
    FILL_FADE("Fill-Fade"),
    INLINE("Inline / Highlight"),
}

data class InventoryPaintColor(
    val hex: String,
    val colorName: String,
    val productCode: String?,
    val lineId: String,
    val brandId: String,
    val effectiveMl: Int,
    val canCount: Int,
)

data class PaletteSwatch(
    val role: PaintRole,
    val hex: String,
    val label: String,
    val productCode: String?,
    val lineLabel: String?,
    val sourceLabel: String?,
    val isOwned: Boolean,
    val effectiveMl: Int,
    val canCount: Int,
)

data class ColorHarmonyPalette(
    val id: String,
    val title: String,
    val rule: String,
    val description: String,
    val scorePercent: Int,
    val minimumEdgeContrastRatio: Double,
    val swatches: List<PaletteSwatch>,
    val isBestEffort: Boolean = false,
) {
    val ownedCount: Int get() = swatches.count(PaletteSwatch::isOwned)
    val missingCount: Int get() = swatches.size - ownedCount
    val hasFillFade: Boolean get() = swatches.any { it.role == PaintRole.FILL_FADE }
    val fillColorCount: Int get() = swatches.count { swatch ->
        swatch.role == PaintRole.FILL ||
            swatch.role == PaintRole.FILL_SECONDARY ||
            swatch.role == PaintRole.FILL_TERTIARY
    }
}

data class ColorComboAnalysis(
    val inventoryColors: List<InventoryPaintColor>,
    val totalEffectiveMl: Int,
    val unresolvedCanCount: Int,
    val requestedToneCount: Int,
    val knowledgeBaseCandidateCount: Long,
    val evaluatedCandidateCount: Long,
    val palettes: List<ColorHarmonyPalette>,
)

private enum class EdgeTone { DARK, LIGHT, AUTO }

private data class ComboProfile(
    val id: String,
    val title: String,
    val rule: String,
    val description: String,
    val outlineHueOffset: Double,
    val secondOutlineHueOffset: Double = 0.0,
    val fadeHueOffset: Double,
    val backgroundHueOffset: Double,
    val inlineHueOffset: Double = 0.0,
    val edgeTone: EdgeTone,
    val neutralOutline: Boolean = false,
    val neutralSecondOutline: Boolean = true,
    val neutralBackground: Boolean = false,
    val neutralInline: Boolean = true,
    val preferLightHighlight: Boolean = true,
    val secondaryFillHueOffset: Double = fadeHueOffset,
    val tertiaryFillHueOffset: Double = -fadeHueOffset,
)

private val comboProfiles = listOf(
    ComboProfile(
        id = "blackline-pop",
        title = "Blackline Pop",
        rule = "Leuchtender Fill · dunkle Outline · helles Gegengewicht",
        description = "Klassischer, sofort lesbarer Aufbau mit neutraler dunkler Kante.",
        outlineHueOffset = 0.0,
        secondOutlineHueOffset = 205.0,
        fadeHueOffset = 24.0,
        backgroundHueOffset = 180.0,
        inlineHueOffset = 35.0,
        edgeTone = EdgeTone.DARK,
        neutralOutline = true,
    ),
    ComboProfile(
        id = "reverse-ink",
        title = "Reverse Ink",
        rule = "Tiefer Fill · helle Outline · dunkler Background",
        description = "Umgekehrte Hell-Dunkel-Hierarchie für dunkle Fills ohne dunkle Kante.",
        outlineHueOffset = 0.0,
        secondOutlineHueOffset = 160.0,
        fadeHueOffset = -22.0,
        backgroundHueOffset = 180.0,
        inlineHueOffset = -35.0,
        edgeTone = EdgeTone.LIGHT,
        neutralOutline = true,
        preferLightHighlight = false,
    ),
    ComboProfile(
        id = "warm-cool",
        title = "Warm–Kalt Punch",
        rule = "Komplementäre Kante bei 180°",
        description = "Warme und kühle Farbbereiche werden durch deutliche Helligkeit getrennt.",
        outlineHueOffset = 180.0,
        secondOutlineHueOffset = 22.0,
        fadeHueOffset = 25.0,
        backgroundHueOffset = -25.0,
        inlineHueOffset = 155.0,
        edgeTone = EdgeTone.AUTO,
        neutralSecondOutline = false,
        neutralInline = false,
        secondaryFillHueOffset = 180.0,
        tertiaryFillHueOffset = 25.0,
    ),
    ComboProfile(
        id = "analog-fade",
        title = "Analog Fade",
        rule = "Nachbartöne im Fill · Gegenpol an der Outline",
        description = "Ein ruhiger Verlauf bleibt durch eine kontrastierende Außenkante klar lesbar.",
        outlineHueOffset = 180.0,
        secondOutlineHueOffset = -30.0,
        fadeHueOffset = 32.0,
        backgroundHueOffset = -32.0,
        inlineHueOffset = 28.0,
        edgeTone = EdgeTone.AUTO,
        neutralInline = false,
    ),
    ComboProfile(
        id = "split-contrast",
        title = "Split Contrast",
        rule = "Geteilter Gegenpol bei 150°/210°",
        description = "Mehr Farbspannung als analog, aber stabiler und leichter lesbar als eine volle Triade.",
        outlineHueOffset = 150.0,
        secondOutlineHueOffset = 30.0,
        fadeHueOffset = -25.0,
        backgroundHueOffset = 210.0,
        inlineHueOffset = -42.0,
        edgeTone = EdgeTone.AUTO,
        neutralSecondOutline = false,
        neutralInline = false,
        secondaryFillHueOffset = 150.0,
        tertiaryFillHueOffset = 210.0,
    ),
    ComboProfile(
        id = "triad-balance",
        title = "Triad Balance",
        rule = "Drei Farbfamilien bei rund 120°",
        description = "Lebendige Farbrollen mit kontrollierter Helligkeit statt gleich dunkler Buntfarben.",
        outlineHueOffset = 120.0,
        secondOutlineHueOffset = 240.0,
        fadeHueOffset = -28.0,
        backgroundHueOffset = 240.0,
        inlineHueOffset = -120.0,
        edgeTone = EdgeTone.AUTO,
        neutralSecondOutline = false,
        neutralInline = false,
        secondaryFillHueOffset = 120.0,
        tertiaryFillHueOffset = 240.0,
    ),
    ComboProfile(
        id = "neutral-signal",
        title = "Neutral + Signal",
        rule = "Neutrale Flächen · ein klarer Farbakzent",
        description = "Ein kräftiger Fill bekommt ruhige neutrale Kanten und einen klaren Helligkeitsanker.",
        outlineHueOffset = 0.0,
        secondOutlineHueOffset = 180.0,
        fadeHueOffset = 18.0,
        backgroundHueOffset = 0.0,
        inlineHueOffset = 180.0,
        edgeTone = EdgeTone.AUTO,
        neutralOutline = true,
        neutralBackground = true,
        neutralInline = false,
    ),
    ComboProfile(
        id = "pastel-deep-edge",
        title = "Pastel / Deep Edge",
        rule = "Heller Farbraum · sehr tiefe Außenkante",
        description = "Weiche Töne und Fades erhalten durch eine dunkle Outline genug Zeichnung.",
        outlineHueOffset = 180.0,
        secondOutlineHueOffset = 24.0,
        fadeHueOffset = 20.0,
        backgroundHueOffset = -25.0,
        inlineHueOffset = 145.0,
        edgeTone = EdgeTone.DARK,
        neutralInline = false,
        preferLightHighlight = false,
    ),
    ComboProfile(
        id = "earth-electric",
        title = "Earth + Electric",
        rule = "Gedämpfte Fläche · elektrischer Innenakzent",
        description = "Ein erdiger Außenraum hält einen unerwartet leuchtenden Inline-Akzent kontrolliert.",
        outlineHueOffset = -18.0,
        secondOutlineHueOffset = 168.0,
        fadeHueOffset = 22.0,
        backgroundHueOffset = 72.0,
        inlineHueOffset = 168.0,
        edgeTone = EdgeTone.DARK,
        neutralOutline = true,
        neutralSecondOutline = false,
        neutralInline = false,
        secondaryFillHueOffset = 168.0,
        tertiaryFillHueOffset = -18.0,
    ),
    ComboProfile(
        id = "night-citrus",
        title = "Night Citrus",
        rule = "Tiefe Außenrollen · heller Zitrus-Stich",
        description = "Dunkle Strukturfarben rahmen eine helle, präzise Inline ein, ohne den Fill zu verschlucken.",
        outlineHueOffset = 195.0,
        secondOutlineHueOffset = 18.0,
        fadeHueOffset = 38.0,
        backgroundHueOffset = 205.0,
        inlineHueOffset = 78.0,
        edgeTone = EdgeTone.DARK,
        neutralSecondOutline = false,
        neutralInline = false,
        secondaryFillHueOffset = 78.0,
        tertiaryFillHueOffset = -35.0,
    ),
    ComboProfile(
        id = "muted-disruption",
        title = "Muted Disruption",
        rule = "Ruhiger Grund · gebrochener Gegenakzent",
        description = "Ein entsättigtes Umfeld lässt eine ungewöhnliche, aber klar getrennte Innenfarbe wirken.",
        outlineHueOffset = 35.0,
        secondOutlineHueOffset = 215.0,
        fadeHueOffset = -18.0,
        backgroundHueOffset = 105.0,
        inlineHueOffset = 215.0,
        edgeTone = EdgeTone.AUTO,
        neutralOutline = true,
        neutralBackground = false,
        neutralInline = false,
        secondaryFillHueOffset = 215.0,
        tertiaryFillHueOffset = 35.0,
    ),
    ComboProfile(
        id = "compound-shift",
        title = "Compound Shift",
        rule = "Versetzter Gegenpol · asymmetrische Farbachse",
        description = "Die Rollen folgen keinem offensichtlichen Regenbogen, bleiben an jeder Berührung aber eindeutig.",
        outlineHueOffset = 165.0,
        secondOutlineHueOffset = -48.0,
        fadeHueOffset = -24.0,
        backgroundHueOffset = 224.0,
        inlineHueOffset = 62.0,
        edgeTone = EdgeTone.AUTO,
        neutralSecondOutline = false,
        neutralInline = false,
        secondaryFillHueOffset = 165.0,
        tertiaryFillHueOffset = 224.0,
    ),
    ComboProfile(
        id = "tonal-break",
        title = "Tonal Break",
        rule = "Ton-in-Ton Fill · harter Innenbruch",
        description = "Verwandte Haupttöne bekommen durch eine weit entfernte Inline und neutrale Kante Spannung.",
        outlineHueOffset = 0.0,
        secondOutlineHueOffset = 118.0,
        fadeHueOffset = 14.0,
        backgroundHueOffset = -42.0,
        inlineHueOffset = 178.0,
        edgeTone = EdgeTone.AUTO,
        neutralOutline = true,
        neutralSecondOutline = false,
        neutralInline = false,
        secondaryFillHueOffset = 14.0,
        tertiaryFillHueOffset = -14.0,
    ),
)

private val roleOrder = listOf(
    PaintRole.BACKGROUND,
    PaintRole.SECOND_OUTLINE,
    PaintRole.OUTLINE,
    PaintRole.FILL_SHADOW,
    PaintRole.FILL,
    PaintRole.FILL_SECONDARY,
    PaintRole.FILL_TERTIARY,
    PaintRole.FILL_FADE,
    PaintRole.INLINE,
)

private val buildOrder = listOf(
    PaintRole.OUTLINE,
    PaintRole.SECOND_OUTLINE,
    PaintRole.BACKGROUND,
    PaintRole.FILL_SHADOW,
    PaintRole.FILL_SECONDARY,
    PaintRole.FILL_TERTIARY,
    PaintRole.FILL_FADE,
    PaintRole.INLINE,
)

private fun roleTemplates(toneCount: Int): List<List<PaintRole>> = when (toneCount.coerceIn(2, 7)) {
    2 -> listOf(listOf(PaintRole.FILL, PaintRole.OUTLINE))
    3 -> listOf(
        listOf(PaintRole.BACKGROUND, PaintRole.FILL, PaintRole.OUTLINE),
        listOf(PaintRole.FILL, PaintRole.OUTLINE, PaintRole.INLINE),
        listOf(PaintRole.OUTLINE, PaintRole.FILL, PaintRole.FILL_SECONDARY),
    )
    4 -> listOf(
        listOf(PaintRole.BACKGROUND, PaintRole.FILL, PaintRole.OUTLINE, PaintRole.INLINE),
        listOf(PaintRole.FILL, PaintRole.FILL_FADE, PaintRole.OUTLINE, PaintRole.SECOND_OUTLINE),
        listOf(PaintRole.BACKGROUND, PaintRole.OUTLINE, PaintRole.FILL, PaintRole.FILL_SECONDARY),
        listOf(PaintRole.OUTLINE, PaintRole.FILL, PaintRole.FILL_SECONDARY, PaintRole.INLINE),
    )
    5 -> listOf(
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.SECOND_OUTLINE,
            PaintRole.OUTLINE,
            PaintRole.FILL,
            PaintRole.INLINE,
        ),
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.OUTLINE,
            PaintRole.FILL,
            PaintRole.FILL_SECONDARY,
            PaintRole.INLINE,
        ),
        listOf(
            PaintRole.SECOND_OUTLINE,
            PaintRole.OUTLINE,
            PaintRole.FILL,
            PaintRole.FILL_SECONDARY,
            PaintRole.FILL_TERTIARY,
        ),
    )
    6 -> listOf(
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.FILL,
            PaintRole.FILL_FADE,
            PaintRole.OUTLINE,
            PaintRole.SECOND_OUTLINE,
            PaintRole.INLINE,
        ),
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.FILL_SHADOW,
            PaintRole.FILL,
            PaintRole.OUTLINE,
            PaintRole.SECOND_OUTLINE,
            PaintRole.INLINE,
        ),
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.SECOND_OUTLINE,
            PaintRole.OUTLINE,
            PaintRole.FILL,
            PaintRole.FILL_SECONDARY,
            PaintRole.INLINE,
        ),
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.OUTLINE,
            PaintRole.FILL,
            PaintRole.FILL_SECONDARY,
            PaintRole.FILL_TERTIARY,
            PaintRole.INLINE,
        ),
    )
    else -> listOf(
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.SECOND_OUTLINE,
            PaintRole.OUTLINE,
            PaintRole.FILL_SHADOW,
            PaintRole.FILL,
            PaintRole.FILL_FADE,
            PaintRole.INLINE,
        ),
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.SECOND_OUTLINE,
            PaintRole.OUTLINE,
            PaintRole.FILL,
            PaintRole.FILL_SECONDARY,
            PaintRole.FILL_TERTIARY,
            PaintRole.INLINE,
        ),
        listOf(
            PaintRole.BACKGROUND,
            PaintRole.OUTLINE,
            PaintRole.FILL_SHADOW,
            PaintRole.FILL,
            PaintRole.FILL_SECONDARY,
            PaintRole.FILL_FADE,
            PaintRole.INLINE,
        ),
    )
}

private data class CandidateColor(
    val hex: String,
    val lch: Oklch,
    val luminance: Double,
    val owned: InventoryPaintColor? = null,
    val official: OfficialCanColor? = null,
)

private data class PartialPalette(
    val assigned: Map<PaintRole, CandidateColor>,
    val fitSum: Double,
)

private data class PaletteValidation(
    val minimumEdgeContrast: Double,
    val visibilityScore: Double,
)

private data class GenerationResult(
    val palettes: List<ColorHarmonyPalette>,
    val evaluatedCount: Long,
)

private data class ScoredPalette(
    val palette: ColorHarmonyPalette,
    val score: Double,
    val profileId: String,
    val templateIndex: Int,
    val anchorHex: String,
)

object ColorHarmonyEngine {
    private const val minimumEdgeContrast = 3.2
    private const val minimumStructuralDistance = 0.13
    private const val minimumSeparatedRoleContrast = 1.8
    private const val addColorBeamWidth = 10
    private const val addColorOptionsPerRole = 7
    private const val addColorShortlistSize = 180
    private const val addColorMaxAnchors = 4
    private const val inventoryBeamWidth = 160

    private val officialCandidates: List<CandidateColor> by lazy {
        OfficialCanColorCatalog.colors
            .asSequence()
            .filterNot(::isTransparentOrClear)
            .map { color ->
                CandidateColor(
                    hex = color.hex,
                    lch = hexToOklch(color.hex),
                    luminance = relativeLuminance(color.hex),
                    official = color,
                )
            }
            .toList()
    }

    fun analyze(
        cans: List<CanItem>,
        includeMissingColors: Boolean,
        toneCount: Int = 5,
    ): ColorComboAnalysis {
        val requestedTones = toneCount.coerceIn(2, 7)
        val activePaint = cans.filter { can ->
            can.status != CanStatus.ARCHIVED && can.status != CanStatus.EMPTY && effectiveMl(can) > 0
        }
        val resolved = activePaint.mapNotNull { can ->
            resolveExactCanColorHex(can)?.let { hex -> can to hex.uppercase() }
        }
        val inventoryColors = resolved
            .groupBy { it.second }
            .map { (hex, entries) ->
                val representative = entries.maxBy { effectiveMl(it.first) }.first
                InventoryPaintColor(
                    hex = hex,
                    colorName = representative.colorName.ifBlank { "Unbenannte Farbe" },
                    productCode = entries.firstNotNullOfOrNull { it.first.colorCode?.takeIf(String::isNotBlank) },
                    lineId = representative.canLineId,
                    brandId = representative.brandId,
                    effectiveMl = entries.sumOf { effectiveMl(it.first) },
                    canCount = entries.size,
                )
            }
            .filter { it.effectiveMl > 0 }
            .sortedByDescending(InventoryPaintColor::effectiveMl)

        val generation = generatePalettes(
            inventory = inventoryColors,
            includeMissingColors = includeMissingColors,
            toneCount = requestedTones,
        )
        val uniqueCatalogColors = officialCandidates.asSequence().map(CandidateColor::hex).distinct().count()

        return ColorComboAnalysis(
            inventoryColors = inventoryColors,
            totalEffectiveMl = inventoryColors.sumOf(InventoryPaintColor::effectiveMl),
            unresolvedCanCount = activePaint.size - resolved.size,
            requestedToneCount = requestedTones,
            knowledgeBaseCandidateCount = combinationsCapped(uniqueCatalogColors, requestedTones),
            evaluatedCandidateCount = generation.evaluatedCount,
            palettes = generation.palettes,
        )
    }

    private fun generatePalettes(
        inventory: List<InventoryPaintColor>,
        includeMissingColors: Boolean,
        toneCount: Int,
    ): GenerationResult {
        if (inventory.isEmpty()) {
            return GenerationResult(emptyList(), 0)
        }

        val ownedCandidates = inventory.map { paint ->
            CandidateColor(
                hex = paint.hex,
                lch = hexToOklch(paint.hex),
                luminance = relativeLuminance(paint.hex),
                owned = paint,
            )
        }
        val ownedHex = ownedCandidates.mapTo(mutableSetOf(), CandidateColor::hex)
        val fullPool = if (includeMissingColors) {
            (ownedCandidates + officialCandidates.filterNot { it.hex in ownedHex }).distinctBy(CandidateColor::hex)
        } else {
            ownedCandidates
        }
        val anchors = ownedCandidates
            .sortedByDescending { availabilityScore(it.owned!!, inventory) + it.lch.chroma * 0.35 }
            .take(if (includeMissingColors) addColorMaxAnchors else 64)
        val beamWidth = if (includeMissingColors) addColorBeamWidth else inventoryBeamWidth
        val optionsPerRole = if (includeMissingColors) {
            addColorOptionsPerRole
        } else {
            ownedCandidates.size.coerceAtLeast(1)
        }
        val generated = mutableListOf<ScoredPalette>()
        val addColorShortlists = mutableMapOf<String, List<CandidateColor>>()
        var evaluated = 0L

        comboProfiles.forEach { profile ->
            roleTemplates(toneCount).forEachIndexed { templateIndex, template ->
                anchors.forEach { fill ->
                    var beam = listOf(
                        PartialPalette(
                            assigned = mapOf(PaintRole.FILL to fill),
                            fitSum = 0.72 + availabilityScore(fill.owned!!, inventory) * 0.28,
                        ),
                    )

                    buildOrder.filter(template::contains).forEach { role ->
                        val expanded = mutableListOf<PartialPalette>()
                        beam.forEach { partial ->
                            val target = roleTarget(profile, role, fill, partial.assigned)
                            val rolePool = if (includeMissingColors) {
                                val cacheKey = buildString {
                                    append(profile.id)
                                    append('|').append(fill.hex)
                                    append('|').append(role.name)
                                    append('|').append((target.lightness * 100).roundToInt())
                                    append('|').append((target.chroma * 100).roundToInt())
                                    append('|').append((target.hue / 5.0).roundToInt())
                                }
                                addColorShortlists.getOrPut(cacheKey) {
                                    (ownedCandidates + fullPool.asSequence()
                                        .map { option -> option to roleFit(option, target, fill, inventory) }
                                        .sortedByDescending { it.second }
                                        .take(addColorShortlistSize)
                                        .map { it.first })
                                        .distinctBy(CandidateColor::hex)
                                }
                            } else {
                                fullPool
                            }
                            rolePool.asSequence()
                                .filterNot { option -> partial.assigned.values.any { it.hex == option.hex } }
                                .filter { option -> partialRoleIsValid(profile, role, option, partial.assigned) }
                                .map { option -> option to roleFit(option, target, fill, inventory) }
                                .sortedByDescending { it.second }
                                .distinctBy { it.first.hex }
                                .take(optionsPerRole)
                                .forEach { (option, fit) ->
                                    evaluated++
                                    expanded += PartialPalette(
                                        assigned = partial.assigned + (role to option),
                                        fitSum = partial.fitSum + fit,
                                    )
                                }
                        }
                        beam = expanded
                            .sortedByDescending(PartialPalette::fitSum)
                            .distinctBy { partial ->
                                partial.assigned.entries
                                    .sortedBy { it.key.ordinal }
                                    .joinToString("|") { "${it.key}:${it.value.hex}" }
                            }
                            .take(beamWidth)
                        if (beam.isEmpty()) return@forEach
                    }

                    beam.forEach { partial ->
                        val validation = validatePalette(partial.assigned, template) ?: return@forEach
                        val swatches = template
                            .sortedBy(roleOrder::indexOf)
                            .map { role -> partial.assigned.getValue(role).toSwatch(role, fill.owned!!) }
                        if (includeMissingColors && swatches.none { !it.isOwned }) return@forEach
                        val averageFit = partial.fitSum / template.size
                        val ownedRatio = swatches.count(PaletteSwatch::isOwned).toDouble() / swatches.size
                        val rawScore = averageFit * 0.46 + validation.visibilityScore * 0.44 + ownedRatio * 0.10
                        val minContrast = validation.minimumEdgeContrast
                        val fingerprint = swatches.joinToString("-") { it.hex.drop(1) }.hashCode().toString(16)
                        val palette = ColorHarmonyPalette(
                            id = "${if (includeMissingColors) "add" else "inventory"}-${profile.id}-$templateIndex-$toneCount-$fingerprint",
                            title = "${profile.title} · ${fill.owned!!.colorName}",
                            rule = "${profile.rule} · $toneCount Töne",
                            description = buildString {
                                append(profile.description)
                                append(" Kantenkontrast mindestens ")
                                append(formatRatio(minContrast))
                                append(":1.")
                                if (PaintRole.FILL_FADE in template) append(" Mit echtem Fill-Fade.")
                                val fillLayers = template.count { role ->
                                    role == PaintRole.FILL ||
                                        role == PaintRole.FILL_SECONDARY ||
                                        role == PaintRole.FILL_TERTIARY
                                }
                                if (fillLayers > 1) append(" Mit $fillLayers aufeinander abgestimmten Fill-Farben.")
                                if (PaintRole.INLINE in template) {
                                    if (profile.preferLightHighlight) {
                                        append(" Das Highlight wird bevorzugt hell und direkt auf dem Fill klar sichtbar gesetzt.")
                                    } else {
                                        append(" Die Inline bleibt direkt auf dem Fill klar sichtbar.")
                                    }
                                }
                                if (includeMissingColors) append(" Fehlende Töne sind reale Katalogfarben.")
                            },
                            scorePercent = scorePercent(rawScore),
                            minimumEdgeContrastRatio = minContrast,
                            swatches = swatches,
                        )
                        generated += ScoredPalette(
                            palette = palette,
                            score = rawScore,
                            profileId = profile.id,
                            templateIndex = templateIndex,
                            anchorHex = fill.hex,
                        )
                    }
                }
            }
        }

        val unique = generated
            .sortedByDescending(ScoredPalette::score)
            .distinctBy { generatedPalette ->
                generatedPalette.palette.swatches.joinToString("|") { "${it.role}:${it.hex}" }
            }
        val diversified = selectDiversePalettes(unique, limit = 10)
        val completed = if (!includeMissingColors && diversified.size < 6) {
            val bestEffort = generateInventoryBestEffort(
                ownedCandidates = ownedCandidates,
                inventory = inventory,
                requestedToneCount = toneCount,
            )
            val strictFingerprints = diversified.mapTo(mutableSetOf()) { scored ->
                scored.palette.swatches.joinToString("|") { "${it.role}:${it.hex}" }
            }
            val additions = bestEffort.palettes.filter { palette ->
                palette.swatches.joinToString("|") { "${it.role}:${it.hex}" } !in strictFingerprints
            }
            GenerationResult(
                palettes = (diversified.map(ScoredPalette::palette) + additions).take(10),
                evaluatedCount = evaluated + bestEffort.evaluatedCount,
            )
        } else {
            GenerationResult(
                palettes = diversified.map(ScoredPalette::palette),
                evaluatedCount = evaluated,
            )
        }

        return completed
    }

    private fun generateInventoryBestEffort(
        ownedCandidates: List<CandidateColor>,
        inventory: List<InventoryPaintColor>,
        requestedToneCount: Int,
    ): GenerationResult {
        val usedToneCount = min(requestedToneCount, ownedCandidates.size).coerceAtLeast(1)
        val templates = if (usedToneCount == 1) {
            listOf(listOf(PaintRole.FILL))
        } else {
            roleTemplates(usedToneCount)
        }
        val anchors = ownedCandidates
            .sortedByDescending { availabilityScore(it.owned!!, inventory) + it.lch.chroma * 0.25 }
            .take(32)
        val generated = mutableListOf<ScoredPalette>()
        var evaluated = 0L

        comboProfiles.forEach { profile ->
            templates.forEachIndexed { templateIndex, template ->
                anchors.forEach { fill ->
                    var beam = listOf(
                        PartialPalette(
                            assigned = mapOf(PaintRole.FILL to fill),
                            fitSum = 0.72 + availabilityScore(fill.owned!!, inventory) * 0.28,
                        ),
                    )

                    buildOrder.filter(template::contains).forEach { role ->
                        val expanded = mutableListOf<PartialPalette>()
                        beam.forEach { partial ->
                            val target = roleTarget(profile, role, fill, partial.assigned)
                            ownedCandidates.asSequence()
                                .filterNot { option -> partial.assigned.values.any { it.hex == option.hex } }
                                .map { option ->
                                    evaluated++
                                    val targetFit = roleFit(option, target, fill, inventory)
                                    val practicalFit = relaxedRoleCompatibility(role, option, partial.assigned)
                                    option to (targetFit * 0.62 + practicalFit * 0.38)
                                }
                                .sortedByDescending { it.second }
                                .forEach { (option, fit) ->
                                    expanded += PartialPalette(
                                        assigned = partial.assigned + (role to option),
                                        fitSum = partial.fitSum + fit,
                                    )
                                }
                        }
                        beam = expanded
                            .sortedByDescending(PartialPalette::fitSum)
                            .distinctBy { partial ->
                                partial.assigned.entries
                                    .sortedBy { it.key.ordinal }
                                    .joinToString("|") { "${it.key}:${it.value.hex}" }
                            }
                            .take(inventoryBeamWidth)
                    }

                    beam.forEach { partial ->
                        if (partial.assigned.size != template.size) return@forEach
                        val validation = evaluateBestEffortPalette(partial.assigned, template)
                        val swatches = template
                            .sortedBy(roleOrder::indexOf)
                            .map { role -> partial.assigned.getValue(role).toSwatch(role, fill.owned!!) }
                        val averageFit = partial.fitSum / template.size
                        val rawScore = averageFit * 0.42 + validation.visibilityScore * 0.58
                        val fingerprint = swatches.joinToString("-") { it.hex.drop(1) }.hashCode().toString(16)
                        val contrastText = formatRatio(validation.minimumEdgeContrast)
                        val palette = ColorHarmonyPalette(
                            id = "inventory-best-${profile.id}-$templateIndex-$usedToneCount-$fingerprint",
                            title = "Bestandspotenzial · ${fill.owned!!.colorName}",
                            rule = "${profile.title} · $usedToneCount vorhandene Töne",
                            description = buildString {
                                append("Bestmögliche professionelle Rollenverteilung nur aus deinem Bestand. ")
                                if (usedToneCount < requestedToneCount) {
                                    append("Verwendet werden alle $usedToneCount verfügbaren exakten Farben statt der gewünschten $requestedToneCount. ")
                                }
                                append("Der Score bewertet Farbtheorie, wahrnehmbaren Abstand, Kantenkontrast und verfügbare Farbmenge. ")
                                if (validation.minimumEdgeContrast < 3.0 && swatches.size > 1) {
                                    append("Schwächste Kante $contrastText:1: Diese Kombination ist experimentell; ähnliche Flächen nicht direkt aneinander setzen oder mit der kontraststärksten Farbe trennen.")
                                } else if (swatches.size > 1) {
                                    append("Die wichtigsten angrenzenden Rollen erreichen mindestens $contrastText:1.")
                                } else {
                                    append("Mit nur einem exakten Ton ist eine Rollen-Harmonie noch eingeschränkt, der vorhandene Fill bleibt aber nutzbar.")
                                }
                            },
                            scorePercent = scorePercent(rawScore),
                            minimumEdgeContrastRatio = validation.minimumEdgeContrast,
                            swatches = swatches,
                            isBestEffort = true,
                        )
                        generated += ScoredPalette(
                            palette = palette,
                            score = rawScore,
                            profileId = "best-${profile.id}",
                            templateIndex = templateIndex,
                            anchorHex = fill.hex,
                        )
                    }
                }
            }
        }

        val unique = generated
            .sortedByDescending(ScoredPalette::score)
            .distinctBy { candidate ->
                candidate.palette.swatches.joinToString("|") { "${it.role}:${it.hex}" }
            }
        return GenerationResult(
            palettes = selectDiversePalettes(unique, limit = 10).map(ScoredPalette::palette),
            evaluatedCount = evaluated,
        )
    }

    private fun selectDiversePalettes(
        candidates: List<ScoredPalette>,
        limit: Int,
    ): List<ScoredPalette> {
        if (candidates.isEmpty()) return emptyList()
        val remaining = candidates.take(1_200).toMutableList()
        val selected = mutableListOf<ScoredPalette>()

        while (selected.size < limit && remaining.isNotEmpty()) {
            val best = remaining.maxBy { candidate ->
                val closestSimilarity = selected.maxOfOrNull { chosen -> paletteSimilarity(candidate, chosen) } ?: 0.0
                val repeatedProfile = if (selected.any { it.profileId == candidate.profileId }) 0.12 else 0.0
                val repeatedAnchor = if (selected.any { it.anchorHex == candidate.anchorHex }) 0.18 else 0.0
                val repeatedTemplate = if (selected.any { it.templateIndex == candidate.templateIndex }) 0.08 else 0.0
                val multiFillBonus = when {
                    candidate.palette.fillColorCount >= 3 &&
                        selected.none { it.palette.fillColorCount >= 3 } -> 0.34
                    candidate.palette.fillColorCount > 1 &&
                        selected.count { it.palette.fillColorCount > 1 } < 2 -> 0.16
                    else -> 0.0
                }
                candidate.score + multiFillBonus -
                    closestSimilarity * 0.46 - repeatedProfile - repeatedAnchor - repeatedTemplate
            }
            selected += best
            remaining.remove(best)
        }
        return selected
    }

    private fun paletteSimilarity(first: ScoredPalette, second: ScoredPalette): Double {
        val firstColors = first.palette.swatches.map { hexToOklch(it.hex) }
        val secondColors = second.palette.swatches.map { hexToOklch(it.hex) }
        val perceptualOverlap = firstColors.count { color ->
            secondColors.any { other -> deltaE(color.toOklab(), other.toOklab()) < 0.055 }
        }.toDouble() / firstColors.size.coerceAtLeast(1)
        val sameRoleColors = first.palette.swatches.count { swatch ->
            second.palette.swatches.any { it.role == swatch.role && it.hex == swatch.hex }
        }.toDouble() / first.palette.swatches.size.coerceAtLeast(1)
        return perceptualOverlap * 0.58 + sameRoleColors * 0.42
    }

    private fun roleTarget(
        profile: ComboProfile,
        role: PaintRole,
        fill: CandidateColor,
        assigned: Map<PaintRole, CandidateColor>,
    ): Oklch {
        val fillLch = fill.lch
        val darkOutline = when (profile.edgeTone) {
            EdgeTone.DARK -> true
            EdgeTone.LIGHT -> false
            EdgeTone.AUTO -> fillLch.lightness >= 0.56
        }
        val outlineLightness = if (darkOutline) 0.16 else 0.91
        val outline = assigned[PaintRole.OUTLINE]
        val secondOutline = assigned[PaintRole.SECOND_OUTLINE]
        val outerEdge = secondOutline ?: outline
        val fadeDirection = if (fillLch.lightness < 0.70) 1.0 else -1.0

        return when (role) {
            PaintRole.FILL -> fillLch
            PaintRole.OUTLINE -> Oklch(
                lightness = outlineLightness,
                chroma = if (profile.neutralOutline) 0.012 else max(fillLch.chroma, 0.12).coerceAtMost(0.22),
                hue = normalizeHue(fillLch.hue + profile.outlineHueOffset),
            )
            PaintRole.SECOND_OUTLINE -> Oklch(
                lightness = if ((outline?.lch?.lightness ?: outlineLightness) < 0.5) 0.90 else 0.14,
                chroma = if (profile.neutralSecondOutline) 0.022 else max(fillLch.chroma * 0.72, 0.075).coerceAtMost(0.17),
                hue = normalizeHue(fillLch.hue + profile.secondOutlineHueOffset),
            )
            PaintRole.BACKGROUND -> Oklch(
                lightness = if ((outerEdge?.lch?.lightness ?: outlineLightness) < 0.5) 0.86 else 0.16,
                chroma = if (profile.neutralBackground) 0.012 else 0.09,
                hue = normalizeHue(fillLch.hue + profile.backgroundHueOffset),
            )
            PaintRole.FILL_SHADOW -> Oklch(
                lightness = (fillLch.lightness - 0.17).coerceIn(0.16, 0.70),
                chroma = (fillLch.chroma * 0.92).coerceAtLeast(0.025),
                hue = normalizeHue(fillLch.hue - profile.fadeHueOffset * 0.35),
            )
            PaintRole.FILL_SECONDARY -> fillLayerTarget(
                fill = fillLch,
                hueOffset = profile.secondaryFillHueOffset,
                preferLighter = true,
            )
            PaintRole.FILL_TERTIARY -> {
                val secondary = assigned[PaintRole.FILL_SECONDARY]?.lch
                fillLayerTarget(
                    fill = fillLch,
                    hueOffset = profile.tertiaryFillHueOffset,
                    preferLighter = secondary?.lightness?.let { it <= fillLch.lightness } ?: false,
                )
            }
            PaintRole.FILL_FADE -> Oklch(
                lightness = (fillLch.lightness + 0.15 * fadeDirection).coerceIn(0.26, 0.88),
                chroma = max(fillLch.chroma, 0.08).coerceAtMost(0.22),
                hue = normalizeHue(fillLch.hue + profile.fadeHueOffset),
            )
            PaintRole.INLINE -> Oklch(
                lightness = if (profile.preferLightHighlight || fillLch.lightness < 0.56) 0.91 else 0.15,
                chroma = if (profile.neutralInline) 0.022 else max(fillLch.chroma * 0.82, 0.085).coerceAtMost(0.19),
                hue = normalizeHue(fillLch.hue + profile.inlineHueOffset),
            )
        }
    }

    private fun fillLayerTarget(
        fill: Oklch,
        hueOffset: Double,
        preferLighter: Boolean,
    ): Oklch {
        val targetHue = normalizeHue(fill.hue + hueOffset)
        val relation = hueDistance(fill.hue, targetHue)
        val lightnessShift = if (preferLighter) 0.16 else -0.16
        val targetLightness = if (relation <= 50.0) {
            (fill.lightness + lightnessShift).coerceIn(0.22, 0.88)
        } else if (preferLighter) {
            max(fill.lightness + 0.10, 0.68).coerceAtMost(0.86)
        } else {
            min(fill.lightness - 0.10, 0.42).coerceAtLeast(0.20)
        }
        return Oklch(
            lightness = targetLightness,
            chroma = max(fill.chroma * 0.90, 0.085).coerceAtMost(0.22),
            hue = targetHue,
        )
    }

    private fun partialRoleIsValid(
        profile: ComboProfile,
        role: PaintRole,
        option: CandidateColor,
        assigned: Map<PaintRole, CandidateColor>,
    ): Boolean {
        val fill = assigned.getValue(PaintRole.FILL)
        return when (role) {
            PaintRole.OUTLINE -> strongEdge(fill, option)
            PaintRole.SECOND_OUTLINE -> assigned[PaintRole.OUTLINE]?.let { strongEdge(it, option) } == true
            PaintRole.BACKGROUND -> {
                val outerEdge = assigned[PaintRole.SECOND_OUTLINE] ?: assigned[PaintRole.OUTLINE]
                val outline = assigned[PaintRole.OUTLINE]
                outerEdge?.let { strongEdge(it, option) } == true &&
                    outline?.let { separatedStructuralRoles(it, option) } == true
            }
            PaintRole.INLINE -> strongEdge(fill, option) &&
                (!profile.preferLightHighlight || option.lch.lightness >= 0.78)
            PaintRole.FILL_SHADOW ->
                fill.lch.lightness - option.lch.lightness >= 0.10 && fadeCompatible(fill, option)
            PaintRole.FILL_SECONDARY -> multiFillCompatible(fill, option)
            PaintRole.FILL_TERTIARY -> {
                val secondary = assigned[PaintRole.FILL_SECONDARY] ?: fill
                multiFillCompatible(secondary, option) && perceptualDistance(fill, option) >= 0.10
            }
            PaintRole.FILL_FADE -> {
                val lightnessDifference = abs(fill.lch.lightness - option.lch.lightness)
                lightnessDifference in 0.07..0.30 && fadeCompatible(fill, option)
            }
            PaintRole.FILL -> true
        }
    }

    private fun relaxedRoleCompatibility(
        role: PaintRole,
        option: CandidateColor,
        assigned: Map<PaintRole, CandidateColor>,
    ): Double {
        val fill = assigned.getValue(PaintRole.FILL)
        return when (role) {
            PaintRole.OUTLINE -> softEdgeQuality(fill, option)
            PaintRole.SECOND_OUTLINE -> assigned[PaintRole.OUTLINE]
                ?.let { softEdgeQuality(it, option) }
                ?: 0.0
            PaintRole.BACKGROUND -> {
                val outline = assigned[PaintRole.OUTLINE]
                val outerEdge = assigned[PaintRole.SECOND_OUTLINE] ?: outline
                listOfNotNull(
                    outerEdge?.let { softEdgeQuality(it, option) },
                    outline?.let { softStructuralSeparation(it, option) },
                ).averageOrZero()
            }
            PaintRole.INLINE -> softEdgeQuality(fill, option)
            PaintRole.FILL_SHADOW -> {
                val lightnessFit = (1.0 - abs((fill.lch.lightness - option.lch.lightness) - 0.16) / 0.28)
                    .coerceIn(0.0, 1.0)
                lightnessFit * 0.55 + softFillRelationship(fill, option) * 0.45
            }
            PaintRole.FILL_SECONDARY -> softFillRelationship(fill, option)
            PaintRole.FILL_TERTIARY -> {
                val secondary = assigned[PaintRole.FILL_SECONDARY] ?: fill
                softFillRelationship(secondary, option) * 0.65 +
                    softStructuralSeparation(fill, option) * 0.35
            }
            PaintRole.FILL_FADE -> {
                val lightnessDifference = abs(fill.lch.lightness - option.lch.lightness)
                val lightnessFit = (1.0 - abs(lightnessDifference - 0.16) / 0.24).coerceIn(0.0, 1.0)
                val hueFit = (1.0 - hueDistance(fill.lch.hue, option.lch.hue) / 85.0).coerceIn(0.0, 1.0)
                lightnessFit * 0.58 + hueFit * 0.42
            }
            PaintRole.FILL -> 1.0
        }
    }

    private fun softEdgeQuality(first: CandidateColor, second: CandidateColor): Double {
        val contrast = ((colorContrastRatio(first.hex, second.hex) - 1.0) / 2.0).coerceIn(0.0, 1.0)
        val distance = (perceptualDistance(first, second) / 0.18).coerceIn(0.0, 1.0)
        val lightness = (abs(first.lch.lightness - second.lch.lightness) / 0.48).coerceIn(0.0, 1.0)
        val discordPenalty = if (isDiscordantAdjacentPair(first, second)) 0.62 else 1.0
        return (contrast * 0.52 + distance * 0.28 + lightness * 0.20) * discordPenalty
    }

    private fun softStructuralSeparation(first: CandidateColor, second: CandidateColor): Double {
        val distance = (perceptualDistance(first, second) / minimumStructuralDistance).coerceIn(0.0, 1.0)
        val contrast = ((colorContrastRatio(first.hex, second.hex) - 1.0) /
            (minimumSeparatedRoleContrast - 1.0)).coerceIn(0.0, 1.0)
        return distance * 0.58 + contrast * 0.42
    }

    private fun softFillRelationship(first: CandidateColor, second: CandidateColor): Double {
        val relation = hueDistance(first.lch.hue, second.lch.hue)
        val analogous = (1.0 - relation / 60.0).coerceIn(0.0, 1.0)
        val complementary = (1.0 - abs(relation - 180.0) / 70.0).coerceIn(0.0, 1.0)
        val triadic = (1.0 - abs(relation - 120.0) / 55.0).coerceIn(0.0, 1.0)
        val theoreticalFit = max(analogous, max(complementary, triadic))
        val distance = (perceptualDistance(first, second) / 0.15).coerceIn(0.0, 1.0)
        val discordPenalty = if (isDiscordantAdjacentPair(first, second)) 0.68 else 1.0
        return (theoreticalFit * 0.68 + distance * 0.32) * discordPenalty
    }

    private fun strongEdge(first: CandidateColor, second: CandidateColor): Boolean {
        if (first.lch.lightness < 0.48 && second.lch.lightness < 0.48) return false
        if (first.lch.lightness > 0.84 && second.lch.lightness > 0.84) return false
        if (perceptualDistance(first, second) < minimumStructuralDistance) return false
        if (isDiscordantAdjacentPair(first, second)) return false
        return colorContrastRatio(first.hex, second.hex) >= minimumEdgeContrast
    }

    private fun separatedStructuralRoles(first: CandidateColor, second: CandidateColor): Boolean {
        if (perceptualDistance(first, second) < minimumStructuralDistance) return false
        if (colorContrastRatio(first.hex, second.hex) < minimumSeparatedRoleContrast) return false
        val sameHueFamily = first.lch.chroma >= 0.035 &&
            second.lch.chroma >= 0.035 &&
            hueDistance(first.lch.hue, second.lch.hue) < 28.0
        if (sameHueFamily && abs(first.lch.lightness - second.lch.lightness) < 0.24) return false
        return !isDiscordantAdjacentPair(first, second)
    }

    private fun perceptualDistance(first: CandidateColor, second: CandidateColor): Double =
        deltaE(first.lch.toOklab(), second.lch.toOklab())

    private fun isDiscordantAdjacentPair(first: CandidateColor, second: CandidateColor): Boolean {
        if (min(first.lch.chroma, second.lch.chroma) < 0.085) return false
        val firstIsGreen = first.lch.hue in 105.0..165.0
        val secondIsGreen = second.lch.hue in 105.0..165.0
        val firstIsPurple = first.lch.hue in 275.0..330.0
        val secondIsPurple = second.lch.hue in 275.0..330.0
        val greenPurplePair = (firstIsGreen && secondIsPurple) || (firstIsPurple && secondIsGreen)
        return greenPurplePair && max(first.lch.lightness, second.lch.lightness) >= 0.60
    }

    private fun fadeCompatible(first: CandidateColor, second: CandidateColor): Boolean {
        val bothNeutral = first.lch.chroma < 0.035 && second.lch.chroma < 0.035
        return bothNeutral || hueDistance(first.lch.hue, second.lch.hue) <= 55.0
    }

    private fun multiFillCompatible(first: CandidateColor, second: CandidateColor): Boolean {
        val distance = perceptualDistance(first, second)
        if (distance < 0.11 || isDiscordantAdjacentPair(first, second)) return false
        val lightnessDifference = abs(first.lch.lightness - second.lch.lightness)
        val bothNeutral = first.lch.chroma < 0.035 && second.lch.chroma < 0.035
        if (bothNeutral) return lightnessDifference >= 0.10

        val relation = hueDistance(first.lch.hue, second.lch.hue)
        val monochromeOrAnalog = relation <= 50.0 && lightnessDifference >= 0.09
        val triadicOrComplementary = relation in 95.0..180.0 && distance >= 0.14
        return monochromeOrAnalog || triadicOrComplementary
    }

    private fun validatePalette(
        assigned: Map<PaintRole, CandidateColor>,
        template: List<PaintRole>,
    ): PaletteValidation? {
        if (assigned.size != template.size) return null
        val fill = assigned.getValue(PaintRole.FILL)
        val outline = assigned.getValue(PaintRole.OUTLINE)
        val edgePairs = mutableListOf(fill to outline)
        assigned[PaintRole.SECOND_OUTLINE]?.let { second -> edgePairs += outline to second }
        assigned[PaintRole.BACKGROUND]?.let { background ->
            edgePairs += (assigned[PaintRole.SECOND_OUTLINE] ?: outline) to background
            if (!separatedStructuralRoles(outline, background)) return null
        }
        assigned[PaintRole.INLINE]?.let { inline -> edgePairs += fill to inline }
        if (edgePairs.any { (first, second) -> !strongEdge(first, second) }) return null

        val values = assigned.values.map { it.lch.lightness }
        val lightnessRange = values.max() - values.min()
        if (template.size >= 3 && (lightnessRange < 0.40 || values.max() < 0.68 || values.min() > 0.46)) return null

        assigned[PaintRole.FILL_SHADOW]?.let { shadow ->
            if (fill.lch.lightness - shadow.lch.lightness < 0.10 || !fadeCompatible(fill, shadow)) return null
        }
        assigned[PaintRole.FILL_SECONDARY]?.let { secondary ->
            if (!multiFillCompatible(fill, secondary)) return null
        }
        assigned[PaintRole.FILL_TERTIARY]?.let { tertiary ->
            val secondary = assigned[PaintRole.FILL_SECONDARY] ?: fill
            if (!multiFillCompatible(secondary, tertiary) || perceptualDistance(fill, tertiary) < 0.10) return null
        }
        assigned[PaintRole.FILL_FADE]?.let { fade ->
            val deltaLightness = abs(fill.lch.lightness - fade.lch.lightness)
            if (deltaLightness !in 0.07..0.30 || !fadeCompatible(fill, fade)) return null
        }

        val contrastRatios = edgePairs.map { (first, second) -> colorContrastRatio(first.hex, second.hex) }
        val minimum = contrastRatios.min()
        val contrastScore = ((minimum - minimumEdgeContrast) / 4.0 + 0.70).coerceIn(0.70, 1.0)
        val rangeScore = (lightnessRange / 0.65).coerceIn(0.0, 1.0)
        return PaletteValidation(
            minimumEdgeContrast = minimum,
            visibilityScore = contrastScore * 0.68 + rangeScore * 0.32,
        )
    }

    private fun evaluateBestEffortPalette(
        assigned: Map<PaintRole, CandidateColor>,
        template: List<PaintRole>,
    ): PaletteValidation {
        val fill = assigned.getValue(PaintRole.FILL)
        val outline = assigned[PaintRole.OUTLINE]
        val edgePairs = mutableListOf<Pair<CandidateColor, CandidateColor>>()
        outline?.let { edgePairs += fill to it }
        assigned[PaintRole.SECOND_OUTLINE]?.let { second -> outline?.let { edgePairs += it to second } }
        assigned[PaintRole.BACKGROUND]?.let { background ->
            (assigned[PaintRole.SECOND_OUTLINE] ?: outline)?.let { edgePairs += it to background }
        }
        assigned[PaintRole.INLINE]?.let { inline -> edgePairs += fill to inline }

        if (edgePairs.isEmpty()) {
            return PaletteValidation(minimumEdgeContrast = 1.0, visibilityScore = 0.12)
        }

        val contrastRatios = edgePairs.map { (first, second) -> colorContrastRatio(first.hex, second.hex) }
        val edgeQuality = edgePairs.map { (first, second) -> softEdgeQuality(first, second) }.average()
        val structuralDistance = edgePairs.map { (first, second) ->
            (perceptualDistance(first, second) / 0.18).coerceIn(0.0, 1.0)
        }.average()
        val values = assigned.values.map { it.lch.lightness }
        val lightnessRange = ((values.max() - values.min()) / 0.58).coerceIn(0.0, 1.0)
        val roleRange = ((template.size - 1) / 6.0).coerceIn(0.0, 1.0)
        return PaletteValidation(
            minimumEdgeContrast = contrastRatios.min(),
            visibilityScore = edgeQuality * 0.54 +
                structuralDistance * 0.24 +
                lightnessRange * 0.16 +
                roleRange * 0.06,
        )
    }

    private fun roleFit(
        option: CandidateColor,
        target: Oklch,
        fill: CandidateColor,
        inventory: List<InventoryPaintColor>,
    ): Double {
        val perceptualDistance = deltaE(option.lch.toOklab(), target.toOklab())
        val perceptualFit = (1.0 - perceptualDistance / 0.48).coerceIn(0.0, 1.0)
        val lightnessFit = (1.0 - abs(option.lch.lightness - target.lightness) / 0.55).coerceIn(0.0, 1.0)
        val hueFit = if (target.chroma < 0.035 || option.lch.chroma < 0.025) {
            1.0
        } else {
            (1.0 - hueDistance(option.lch.hue, target.hue) / 180.0).coerceIn(0.0, 1.0)
        }
        val ownedBonus = option.owned?.let { 0.10 + availabilityScore(it, inventory) * 0.10 } ?: 0.0
        val sourceAffinity = option.official?.let { official ->
            when {
                official.lineId == fill.owned?.lineId -> 0.045
                official.lineId.substringBefore(':') == fill.owned?.brandId -> 0.025
                else -> 0.0
            }
        } ?: 0.0
        return (perceptualFit * 0.55 + lightnessFit * 0.25 + hueFit * 0.20 + ownedBonus + sourceAffinity)
            .coerceIn(0.0, 1.0)
    }

    private fun CandidateColor.toSwatch(role: PaintRole, fill: InventoryPaintColor): PaletteSwatch {
        owned?.let { paint ->
            return PaletteSwatch(
                role = role,
                hex = paint.hex,
                label = paint.colorName,
                productCode = paint.productCode,
                lineLabel = lineName(paint.lineId),
                sourceLabel = null,
                isOwned = true,
                effectiveMl = paint.effectiveMl,
                canCount = paint.canCount,
            )
        }
        val catalogColor = requireNotNull(official)
        return PaletteSwatch(
            role = role,
            hex = catalogColor.hex,
            label = catalogColor.colorName,
            productCode = catalogColor.colorCode ?: catalogColor.productCode,
            lineLabel = lineName(catalogColor.lineId),
            sourceLabel = OfficialCanColorCatalog.sourceFor(catalogColor)?.label,
            isOwned = false,
            effectiveMl = recommendedPurchaseMl(fill, catalogColor),
            canCount = 0,
        )
    }

    private fun recommendedPurchaseMl(anchor: InventoryPaintColor, color: OfficialCanColor): Int {
        val canVolume = catalogDisplayVolumeMl(color.lineId).coerceAtLeast(100)
        val target = anchor.effectiveMl.coerceIn(canVolume, canVolume * 2)
        return (ceil(target / canVolume.toDouble()) * canVolume).roundToInt()
    }

    private fun availabilityScore(color: InventoryPaintColor, inventory: List<InventoryPaintColor>): Double =
        color.effectiveMl.toDouble() / inventory.maxOf(InventoryPaintColor::effectiveMl).coerceAtLeast(1)

    private fun effectiveMl(can: CanItem): Int {
        val volume = can.volumeMl?.coerceAtLeast(0) ?: 400
        val fill = can.fillPercent?.coerceIn(0, 100) ?: 100
        return (volume * fill / 100.0).roundToInt()
    }

    private fun scorePercent(raw: Double): Int =
        (raw.coerceIn(0.0, 1.0) * 100.0).roundToInt().coerceIn(0, 100)
}

private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

internal fun colorContrastRatio(firstHex: String, secondHex: String): Double {
    val first = relativeLuminance(firstHex)
    val second = relativeLuminance(secondHex)
    return (max(first, second) + 0.05) / (min(first, second) + 0.05)
}

internal fun colorPerceptualDistance(firstHex: String, secondHex: String): Double =
    deltaE(hexToOklch(firstHex).toOklab(), hexToOklch(secondHex).toOklab())

internal fun colorPerceptualLightness(hex: String): Double = hexToOklch(hex).lightness

private fun relativeLuminance(hex: String): Double {
    val red = srgbToLinear(hex.substring(1, 3).toInt(16) / 255.0)
    val green = srgbToLinear(hex.substring(3, 5).toInt(16) / 255.0)
    val blue = srgbToLinear(hex.substring(5, 7).toInt(16) / 255.0)
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

private fun combinationsCapped(n: Int, k: Int, cap: Long = 999_999_999L): Long {
    if (k !in 0..n) return 0
    val reducedK = min(k, n - k)
    var result = 1.0
    for (index in 1..reducedK) {
        result = result * (n - reducedK + index) / index
        if (result >= cap) return cap
    }
    return result.toLong()
}

private fun isTransparentOrClear(color: OfficialCanColor): Boolean {
    val searchable = "${color.colorName} ${color.colorCode.orEmpty()}".lowercase()
    return searchable.contains("transparent") || searchable.contains("clear varnish") || searchable.contains("klarlack")
}

private data class Oklab(val lightness: Double, val a: Double, val b: Double)

private data class Oklch(val lightness: Double, val chroma: Double, val hue: Double) {
    fun toOklab(): Oklab {
        val radians = hue * PI / 180.0
        return Oklab(lightness, chroma * cos(radians), chroma * sin(radians))
    }
}

private fun hexToOklch(hex: String): Oklch {
    val red = hex.substring(1, 3).toInt(16) / 255.0
    val green = hex.substring(3, 5).toInt(16) / 255.0
    val blue = hex.substring(5, 7).toInt(16) / 255.0
    val r = srgbToLinear(red)
    val g = srgbToLinear(green)
    val b = srgbToLinear(blue)

    val l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    val m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    val s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    val lRoot = Math.cbrt(l)
    val mRoot = Math.cbrt(m)
    val sRoot = Math.cbrt(s)
    val lab = Oklab(
        lightness = 0.2104542553 * lRoot + 0.7936177850 * mRoot - 0.0040720468 * sRoot,
        a = 1.9779984951 * lRoot - 2.4285922050 * mRoot + 0.4505937099 * sRoot,
        b = 0.0259040371 * lRoot + 0.7827717662 * mRoot - 0.8086757660 * sRoot,
    )
    return Oklch(
        lightness = lab.lightness,
        chroma = sqrt(lab.a * lab.a + lab.b * lab.b),
        hue = normalizeHue(atan2(lab.b, lab.a) * 180.0 / PI),
    )
}

private fun srgbToLinear(value: Double): Double =
    if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

private fun deltaE(first: Oklab, second: Oklab): Double = sqrt(
    (first.lightness - second.lightness).pow(2) +
        (first.a - second.a).pow(2) +
        (first.b - second.b).pow(2),
)

private fun normalizeHue(value: Double): Double = ((value % 360.0) + 360.0) % 360.0

private fun hueDistance(first: Double, second: Double): Double {
    val direct = abs(normalizeHue(first) - normalizeHue(second))
    return min(direct, 360.0 - direct)
}

private fun formatRatio(value: Double): String = "%.1f".format(java.util.Locale.US, value)
