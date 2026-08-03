import Foundation

private struct RGB: Sendable {
    var r: Double
    var g: Double
    var b: Double
}

private struct HSL: Sendable {
    var hue: Double
    var saturation: Double
    var lightness: Double
}

private struct HarmonyCandidate: Sendable {
    var hex: String
    var hsl: HSL
    var owned: InventoryPaintColor?
    var official: OfficialCanColor?
}

private enum EdgeTone: Sendable { case dark, light, automatic }

private struct ComboProfile: Sendable {
    var id: String
    var title: String
    var rule: String
    var description: String
    var outlineOffset: Double
    var secondOutlineOffset: Double
    var fadeOffset: Double
    var backgroundOffset: Double
    var inlineOffset: Double
    var edgeTone: EdgeTone
    var neutralOutline = false
    var neutralSecondOutline = true
    var neutralBackground = false
    var neutralInline = true
    var preferLightHighlight = true
    var secondaryFillOffset: Double
    var tertiaryFillOffset: Double
}

private struct GeneratedPalette: Sendable {
    var palette: ColorHarmonyPalette
    var rawScore: Double
    var profileId: String
    var anchorHex: String
}

enum ColorHarmonyEngine {
    private static let profiles: [ComboProfile] = [
        profile("blackline-pop", "Blackline Pop", "Leuchtender Fill · dunkle Outline · helles Gegengewicht", "Klassischer, sofort lesbarer Aufbau mit neutraler dunkler Kante.", 0, 205, 24, 180, 35, .dark, neutralOutline: true),
        profile("reverse-ink", "Reverse Ink", "Tiefer Fill · helle Outline · dunkler Background", "Umgekehrte Hell-Dunkel-Hierarchie für dunkle Fills ohne dunkle Kante.", 0, 160, -22, 180, -35, .light, neutralOutline: true, preferLightHighlight: false),
        profile("warm-cool", "Warm–Kalt Punch", "Komplementäre Kante bei 180°", "Warme und kühle Farbbereiche werden durch deutliche Helligkeit getrennt.", 180, 22, 25, -25, 155, .automatic, neutralSecond: false, neutralInline: false, secondary: 180, tertiary: 25),
        profile("analog-fade", "Analog Fade", "Nachbartöne im Fill · Gegenpol an der Outline", "Ein ruhiger Verlauf bleibt durch eine kontrastierende Außenkante klar lesbar.", 180, -30, 32, -32, 28, .automatic, neutralInline: false),
        profile("split-contrast", "Split Contrast", "Geteilter Gegenpol bei 150°/210°", "Mehr Farbspannung als analog, aber stabiler als eine volle Triade.", 150, 30, -25, 210, -42, .automatic, neutralSecond: false, neutralInline: false, secondary: 150, tertiary: 210),
        profile("triad-balance", "Triad Balance", "Drei Farbfamilien bei rund 120°", "Lebendige Farbrollen mit kontrollierter Helligkeit statt gleich dunkler Buntfarben.", 120, 240, -28, 240, -120, .automatic, neutralSecond: false, neutralInline: false, secondary: 120, tertiary: 240),
        profile("neutral-signal", "Neutral + Signal", "Neutrale Flächen · ein klarer Farbakzent", "Ein kräftiger Fill bekommt ruhige neutrale Kanten und einen klaren Helligkeitsanker.", 0, 180, 18, 0, 180, .automatic, neutralOutline: true, neutralBackground: true, neutralInline: false),
        profile("pastel-deep-edge", "Pastel / Deep Edge", "Heller Farbraum · sehr tiefe Außenkante", "Weiche Töne und Fades erhalten durch eine dunkle Outline genug Zeichnung.", 180, 24, 20, -25, 145, .dark, neutralInline: false, preferLightHighlight: false),
        profile("earth-electric", "Earth + Electric", "Gedämpfte Fläche · elektrischer Innenakzent", "Ein erdiger Außenraum hält einen unerwartet leuchtenden Inline-Akzent kontrolliert.", -18, 168, 22, 72, 168, .dark, neutralOutline: true, neutralSecond: false, neutralInline: false, secondary: 168, tertiary: -18),
        profile("night-citrus", "Night Citrus", "Tiefe Außenrollen · heller Zitrus-Stich", "Dunkle Strukturfarben rahmen eine helle, präzise Inline ein.", 195, 18, 38, 205, 78, .dark, neutralSecond: false, neutralInline: false, secondary: 78, tertiary: -35),
        profile("muted-disruption", "Muted Disruption", "Ruhiger Grund · gebrochener Gegenakzent", "Ein entsättigtes Umfeld lässt eine ungewöhnliche Innenfarbe kontrolliert wirken.", 35, 215, -18, 105, 215, .automatic, neutralOutline: true, neutralSecond: false, neutralInline: false, secondary: 215, tertiary: 35),
        profile("compound-shift", "Compound Shift", "Versetzter Gegenpol · asymmetrische Farbachse", "Ungewöhnliche Rollen bleiben an jeder Berührung eindeutig getrennt.", 165, -48, -24, 224, 62, .automatic, neutralSecond: false, neutralInline: false, secondary: 165, tertiary: 224),
        profile("tonal-break", "Tonal Break", "Ton-in-Ton Fill · harter Innenbruch", "Verwandte Haupttöne bekommen durch eine weit entfernte Inline und neutrale Kante Spannung.", 0, 118, 14, -42, 178, .automatic, neutralOutline: true, neutralSecond: false, neutralInline: false, secondary: 14, tertiary: -14),
    ]

    static func analyze(cans: [CanItem], officialColors: [OfficialCanColor], includeMissingColors: Bool, toneCount: Int) -> ColorComboAnalysis {
        let requested = min(max(toneCount, 2), 7)
        let active = cans.filter { $0.status != .archived && $0.status != .empty && effectiveMl($0) > 0 }
        let resolvedPairs = active.compactMap { can -> (CanItem, String)? in
            CatalogStore.shared.resolveHex(for: can).map { (can, $0.uppercased()) }
        }
        let grouped = Dictionary(grouping: resolvedPairs, by: { $0.1 })
        let inventory = grouped.map { hex, entries -> InventoryPaintColor in
            let representative = entries.max { effectiveMl($0.0) < effectiveMl($1.0) }!.0
            return InventoryPaintColor(
                hex: hex,
                colorName: representative.colorName.nilIfBlank ?? "Unbenannte Farbe",
                productCode: entries.compactMap { $0.0.colorCode?.nilIfBlank }.first,
                lineId: representative.canLineId,
                brandId: representative.brandId,
                effectiveMl: entries.reduce(0) { $0 + effectiveMl($1.0) },
                canCount: entries.count
            )
        }.sorted { $0.effectiveMl > $1.effectiveMl }

        guard !inventory.isEmpty else {
            return ColorComboAnalysis(
                inventoryColors: [], totalEffectiveMl: 0, unresolvedCanCount: active.count,
                requestedToneCount: requested, knowledgeBaseCandidateCount: 0,
                evaluatedCandidateCount: 0, palettes: []
            )
        }

        let owned = inventory.map { HarmonyCandidate(hex: $0.hex, hsl: hsl($0.hex), owned: $0, official: nil) }
        let ownedHex = Set(owned.map(\.hex))
        let official = officialColors
            .filter { !isTransparent($0) && !ownedHex.contains($0.hex.uppercased()) }
            .reduce(into: [String: HarmonyCandidate]()) { result, color in
                let hex = color.hex.uppercased()
                if result[hex] == nil { result[hex] = HarmonyCandidate(hex: hex, hsl: hsl(hex), owned: nil, official: color) }
            }.map { $0.value }
        let pool = includeMissingColors ? owned + official : owned
        let anchors = Array(owned.sorted { availability($0.owned!, inventory) > availability($1.owned!, inventory) }.prefix(includeMissingColors ? 4 : 24))
        let templates = roleTemplates(requested)
        var evaluated: Int64 = 0
        var generated: [GeneratedPalette] = []

        for profile in profiles {
            for (templateIndex, template) in templates.enumerated() {
                for anchor in anchors {
                    for variant in 0..<(includeMissingColors ? 2 : 3) {
                        var assigned: [PaintRole: HarmonyCandidate] = [.fill: anchor]
                        var fitTotal = 0.82
                        var bestEffort = false
                        for role in buildOrder where template.contains(role) {
                            let target = target(for: role, profile: profile, fill: anchor, assigned: assigned, variant: variant)
                            var ranked: [(HarmonyCandidate, Double)] = []
                            ranked.reserveCapacity(pool.count)
                            for candidate in pool where !assigned.values.contains(where: { $0.hex == candidate.hex }) {
                                evaluated += 1
                                let strict = isRoleValid(role, option: candidate, assigned: assigned, profile: profile)
                                if includeMissingColors && !strict { continue }
                                let score = roleFit(candidate, target: target, inventory: inventory) * (strict ? 1 : 0.72)
                                ranked.append((candidate, score))
                            }
                            ranked.sort { $0.1 > $1.1 }
                            let selectionIndex = min(variant, max(ranked.count - 1, 0))
                            guard !ranked.isEmpty else { assigned.removeAll(); break }
                            let selection = ranked[selectionIndex]
                            if !isRoleValid(role, option: selection.0, assigned: assigned, profile: profile) { bestEffort = true }
                            assigned[role] = selection.0
                            fitTotal += selection.1
                        }
                        guard assigned.count == template.count else { continue }
                        if includeMissingColors && !assigned.values.contains(where: { $0.owned == nil }) { continue }
                        let validation = validate(assigned, template: template)
                        if includeMissingColors && !validation.strict { continue }
                        let swatches = template.sorted(by: roleOrder).compactMap { role in
                            assigned[role].map { swatch($0, role: role, anchor: anchor.owned!) }
                        }
                        let ownedRatio = Double(swatches.filter(\.isOwned).count) / Double(swatches.count)
                        let averageFit = fitTotal / Double(template.count)
                        let raw = min(max(averageFit * 0.42 + validation.visibility * 0.48 + ownedRatio * 0.10, 0), 1)
                        let fingerprint = swatches.map { "\($0.role.rawValue):\($0.hex)" }.joined(separator: "|")
                        let edgeText = String(format: "%.1f", validation.minimumContrast)
                        var description = "\(profile.description) Kantenkontrast mindestens \(edgeText):1."
                        if template.contains(.fillFade) { description += " Mit echtem Fill-Fade." }
                        let fillCount = template.filter { [.fill, .fillSecondary, .fillTertiary].contains($0) }.count
                        if fillCount > 1 { description += " Mit \(fillCount) abgestimmten Fill-Farben." }
                        if includeMissingColors { description += " Fehlende Töne sind reale Herstellerfarben." }
                        if !validation.strict { description += " Experimentell: schwächere Kanten bewusst räumlich trennen." }
                        let palette = ColorHarmonyPalette(
                            id: "\(includeMissingColors ? "add" : "inventory")-\(profile.id)-\(templateIndex)-\(variant)-\(fingerprint.hashValue)",
                            title: "\(bestEffort || !validation.strict ? "Bestandspotenzial" : profile.title) · \(anchor.owned!.colorName)",
                            rule: "\(profile.rule) · \(requested) Töne",
                            description: description,
                            scorePercent: min(max(Int((raw * 100).rounded()), 0), 100),
                            minimumEdgeContrastRatio: validation.minimumContrast,
                            swatches: swatches,
                            isBestEffort: bestEffort || !validation.strict
                        )
                        generated.append(GeneratedPalette(palette: palette, rawScore: raw, profileId: profile.id, anchorHex: anchor.hex))
                    }
                }
            }
        }

        let unique = Dictionary(grouping: generated, by: { $0.palette.swatches.map { "\($0.role.rawValue):\($0.hex)" }.joined(separator: "|") })
            .compactMap { $0.value.max { $0.rawScore < $1.rawScore } }
            .sorted { $0.rawScore > $1.rawScore }
        let palettes = selectDiverse(unique, limit: 10).map(\.palette)
        let catalogColorCount = Set(officialColors.map { $0.hex.uppercased() }).count
        return ColorComboAnalysis(
            inventoryColors: inventory,
            totalEffectiveMl: inventory.reduce(0) { $0 + $1.effectiveMl },
            unresolvedCanCount: active.count - resolvedPairs.count,
            requestedToneCount: requested,
            knowledgeBaseCandidateCount: combinationsCapped(catalogColorCount, requested),
            evaluatedCandidateCount: evaluated,
            palettes: palettes
        )
    }

    private static let buildOrder: [PaintRole] = [.outline, .secondOutline, .background, .fillShadow, .fillSecondary, .fillTertiary, .fillFade, .inline]

    private static func roleOrder(_ first: PaintRole, _ second: PaintRole) -> Bool {
        let order: [PaintRole] = [.background, .secondOutline, .outline, .fillShadow, .fill, .fillSecondary, .fillTertiary, .fillFade, .inline]
        return (order.firstIndex(of: first) ?? 0) < (order.firstIndex(of: second) ?? 0)
    }

    private static func roleTemplates(_ count: Int) -> [[PaintRole]] {
        switch count {
        case 2: [[.fill, .outline]]
        case 3: [[.background, .fill, .outline], [.fill, .outline, .inline], [.outline, .fill, .fillSecondary]]
        case 4: [[.background, .fill, .outline, .inline], [.fill, .fillFade, .outline, .secondOutline], [.background, .outline, .fill, .fillSecondary], [.outline, .fill, .fillSecondary, .inline]]
        case 5: [[.background, .secondOutline, .outline, .fill, .inline], [.background, .outline, .fill, .fillSecondary, .inline], [.secondOutline, .outline, .fill, .fillSecondary, .fillTertiary]]
        case 6: [[.background, .fill, .fillFade, .outline, .secondOutline, .inline], [.background, .fillShadow, .fill, .outline, .secondOutline, .inline], [.background, .outline, .fill, .fillSecondary, .fillTertiary, .inline]]
        default: [[.background, .secondOutline, .outline, .fillShadow, .fill, .fillFade, .inline], [.background, .secondOutline, .outline, .fill, .fillSecondary, .fillTertiary, .inline], [.background, .outline, .fillShadow, .fill, .fillSecondary, .fillFade, .inline]]
        }
    }

    private static func target(for role: PaintRole, profile: ComboProfile, fill: HarmonyCandidate, assigned: [PaintRole: HarmonyCandidate], variant: Int) -> HSL {
        let base = fill.hsl
        let hueNudge = Double(variant) * 7
        let darkOutline: Bool = switch profile.edgeTone {
        case .dark: true
        case .light: false
        case .automatic: base.lightness >= 0.56
        }
        let outlineLightness = darkOutline ? 0.14 : 0.92
        switch role {
        case .fill: return base
        case .outline: return HSL(hue: normalizeHue(base.hue + profile.outlineOffset + hueNudge), saturation: profile.neutralOutline ? 0.03 : max(base.saturation, 0.58), lightness: outlineLightness)
        case .secondOutline:
            let first = assigned[.outline]?.hsl.lightness ?? outlineLightness
            return HSL(hue: normalizeHue(base.hue + profile.secondOutlineOffset - hueNudge), saturation: profile.neutralSecondOutline ? 0.06 : max(base.saturation * 0.72, 0.36), lightness: first < 0.5 ? 0.90 : 0.13)
        case .background:
            let edge = assigned[.secondOutline] ?? assigned[.outline]
            return HSL(hue: normalizeHue(base.hue + profile.backgroundOffset + hueNudge), saturation: profile.neutralBackground ? 0.03 : 0.42, lightness: (edge?.hsl.lightness ?? outlineLightness) < 0.5 ? 0.85 : 0.15)
        case .fillShadow: return HSL(hue: normalizeHue(base.hue - profile.fadeOffset * 0.35), saturation: max(base.saturation * 0.9, 0.2), lightness: min(max(base.lightness - 0.17, 0.14), 0.68))
        case .fillSecondary: return fillLayer(base, offset: profile.secondaryFillOffset + hueNudge, lighter: true)
        case .fillTertiary: return fillLayer(base, offset: profile.tertiaryFillOffset - hueNudge, lighter: (assigned[.fillSecondary]?.hsl.lightness ?? base.lightness) <= base.lightness)
        case .fillFade:
            let shift = base.lightness < 0.7 ? 0.15 : -0.15
            return HSL(hue: normalizeHue(base.hue + profile.fadeOffset), saturation: max(base.saturation, 0.4), lightness: min(max(base.lightness + shift, 0.24), 0.88))
        case .inline: return HSL(hue: normalizeHue(base.hue + profile.inlineOffset - hueNudge), saturation: profile.neutralInline ? 0.04 : max(base.saturation * 0.82, 0.45), lightness: profile.preferLightHighlight || base.lightness < 0.56 ? 0.92 : 0.14)
        }
    }

    private static func fillLayer(_ fill: HSL, offset: Double, lighter: Bool) -> HSL {
        let relation = hueDistance(fill.hue, normalizeHue(fill.hue + offset))
        let lightness: Double
        if relation <= 50 { lightness = min(max(fill.lightness + (lighter ? 0.16 : -0.16), 0.2), 0.88) }
        else { lightness = lighter ? max(fill.lightness + 0.1, 0.68) : min(fill.lightness - 0.1, 0.42) }
        return HSL(hue: normalizeHue(fill.hue + offset), saturation: max(fill.saturation * 0.9, 0.4), lightness: lightness)
    }

    private static func roleFit(_ option: HarmonyCandidate, target: HSL, inventory: [InventoryPaintColor]) -> Double {
        let hueFit = target.saturation < 0.08 || option.hsl.saturation < 0.06 ? 1 : 1 - hueDistance(option.hsl.hue, target.hue) / 180
        let lightFit = max(1 - abs(option.hsl.lightness - target.lightness) / 0.55, 0)
        let saturationFit = max(1 - abs(option.hsl.saturation - target.saturation), 0)
        let owned = option.owned.map { 0.08 + availability($0, inventory) * 0.08 } ?? 0
        return min(max(hueFit * 0.4 + lightFit * 0.42 + saturationFit * 0.18 + owned, 0), 1)
    }

    private static func isRoleValid(_ role: PaintRole, option: HarmonyCandidate, assigned: [PaintRole: HarmonyCandidate], profile: ComboProfile) -> Bool {
        let fill = assigned[.fill]!
        switch role {
        case .outline: return strongEdge(fill, option)
        case .secondOutline: return assigned[.outline].map { strongEdge($0, option) } ?? false
        case .background:
            let edge = assigned[.secondOutline] ?? assigned[.outline]
            return edge.map { strongEdge($0, option) } == true && assigned[.outline].map { separated($0, option) } == true
        case .inline: return strongEdge(fill, option) && (!profile.preferLightHighlight || option.hsl.lightness >= 0.72)
        case .fillShadow: return fill.hsl.lightness - option.hsl.lightness >= 0.09 && fadeCompatible(fill, option)
        case .fillSecondary: return multiFillCompatible(fill, option)
        case .fillTertiary: return multiFillCompatible(assigned[.fillSecondary] ?? fill, option) && distance(fill, option) >= 0.10
        case .fillFade: return abs(fill.hsl.lightness - option.hsl.lightness) >= 0.07 && abs(fill.hsl.lightness - option.hsl.lightness) <= 0.3 && fadeCompatible(fill, option)
        case .fill: return true
        }
    }

    private static func validate(_ assigned: [PaintRole: HarmonyCandidate], template: [PaintRole]) -> (strict: Bool, minimumContrast: Double, visibility: Double) {
        guard let fill = assigned[.fill] else { return (false, 1, 0) }
        var pairs: [(HarmonyCandidate, HarmonyCandidate)] = []
        if let outline = assigned[.outline] {
            pairs.append((fill, outline))
            if let second = assigned[.secondOutline] { pairs.append((outline, second)) }
            if let background = assigned[.background] {
                pairs.append((assigned[.secondOutline] ?? outline, background))
                if !separated(outline, background) { return (false, pairs.map { contrast($0.0.hex, $0.1.hex) }.min() ?? 1, 0.35) }
            }
        }
        if let inline = assigned[.inline] { pairs.append((fill, inline)) }
        let ratios = pairs.map { contrast($0.0.hex, $0.1.hex) }
        let minimum = ratios.min() ?? 1
        let lightness = assigned.values.map { $0.hsl.lightness }
        let range = (lightness.max() ?? 0) - (lightness.min() ?? 0)
        let strict = pairs.allSatisfy { strongEdge($0.0, $0.1) } && (template.count < 3 || range >= 0.36)
        let contrastScore = min(max((minimum - 1) / 4.8, 0), 1)
        let visibility = contrastScore * 0.68 + min(range / 0.65, 1) * 0.32
        return (strict, minimum, visibility)
    }

    private static func strongEdge(_ first: HarmonyCandidate, _ second: HarmonyCandidate) -> Bool {
        if first.hsl.lightness < 0.46 && second.hsl.lightness < 0.46 { return false }
        if first.hsl.lightness > 0.86 && second.hsl.lightness > 0.86 { return false }
        return distance(first, second) >= 0.13 && !discordant(first, second) && contrast(first.hex, second.hex) >= 3.2
    }

    private static func separated(_ first: HarmonyCandidate, _ second: HarmonyCandidate) -> Bool {
        guard distance(first, second) >= 0.13, contrast(first.hex, second.hex) >= 1.8, !discordant(first, second) else { return false }
        let sameHue = first.hsl.saturation > 0.18 && second.hsl.saturation > 0.18 && hueDistance(first.hsl.hue, second.hsl.hue) < 28
        return !sameHue || abs(first.hsl.lightness - second.hsl.lightness) >= 0.24
    }

    private static func multiFillCompatible(_ first: HarmonyCandidate, _ second: HarmonyCandidate) -> Bool {
        guard distance(first, second) >= 0.11, !discordant(first, second) else { return false }
        let relation = hueDistance(first.hsl.hue, second.hsl.hue)
        let lightness = abs(first.hsl.lightness - second.hsl.lightness)
        return (relation <= 50 && lightness >= 0.08) || (relation >= 92 && relation <= 180 && distance(first, second) >= 0.14)
    }

    private static func fadeCompatible(_ first: HarmonyCandidate, _ second: HarmonyCandidate) -> Bool {
        (first.hsl.saturation < 0.12 && second.hsl.saturation < 0.12) || hueDistance(first.hsl.hue, second.hsl.hue) <= 55
    }

    private static func discordant(_ first: HarmonyCandidate, _ second: HarmonyCandidate) -> Bool {
        guard min(first.hsl.saturation, second.hsl.saturation) > 0.42 else { return false }
        let greenPurple = (first.hsl.hue >= 80 && first.hsl.hue <= 155 && second.hsl.hue >= 265 && second.hsl.hue <= 325)
            || (second.hsl.hue >= 80 && second.hsl.hue <= 155 && first.hsl.hue >= 265 && first.hsl.hue <= 325)
        return greenPurple && max(first.hsl.lightness, second.hsl.lightness) >= 0.6
    }

    private static func swatch(_ candidate: HarmonyCandidate, role: PaintRole, anchor: InventoryPaintColor) -> PaletteSwatch {
        if let owned = candidate.owned {
            return PaletteSwatch(role: role, hex: owned.hex, label: owned.colorName, productCode: owned.productCode, lineLabel: CatalogStore.shared.lineName(owned.lineId), sourceLabel: nil, isOwned: true, effectiveMl: owned.effectiveMl, canCount: owned.canCount)
        }
        let official = candidate.official!
        let canVolume = max(CatalogStore.shared.displayVolume(lineId: official.lineId), 100)
        let target = min(max(anchor.effectiveMl, canVolume), canVolume * 2)
        let purchaseMl = Int(ceil(Double(target) / Double(canVolume))) * canVolume
        return PaletteSwatch(role: role, hex: official.hex, label: official.colorName, productCode: official.colorCode ?? official.productCode, lineLabel: CatalogStore.shared.lineName(official.lineId), sourceLabel: CatalogStore.shared.source(for: official)?.label, isOwned: false, effectiveMl: purchaseMl, canCount: 0)
    }

    private static func selectDiverse(_ source: [GeneratedPalette], limit: Int) -> [GeneratedPalette] {
        var remaining = source
        var selected: [GeneratedPalette] = []
        while selected.count < limit && !remaining.isEmpty {
            let bestIndex = remaining.indices.max { lhs, rhs in
                diversifiedScore(remaining[lhs], selected) < diversifiedScore(remaining[rhs], selected)
            }!
            selected.append(remaining.remove(at: bestIndex))
        }
        return selected
    }

    private static func diversifiedScore(_ item: GeneratedPalette, _ selected: [GeneratedPalette]) -> Double {
        var score = item.rawScore
        if selected.contains(where: { $0.profileId == item.profileId }) { score -= 0.11 }
        if selected.contains(where: { $0.anchorHex == item.anchorHex }) { score -= 0.08 }
        return score
    }

    private static func profile(_ id: String, _ title: String, _ rule: String, _ description: String, _ outline: Double, _ second: Double, _ fade: Double, _ background: Double, _ inline: Double, _ edge: EdgeTone, neutralOutline: Bool = false, neutralSecond: Bool = true, neutralBackground: Bool = false, neutralInline: Bool = true, preferLightHighlight: Bool = true, secondary: Double? = nil, tertiary: Double? = nil) -> ComboProfile {
        ComboProfile(id: id, title: title, rule: rule, description: description, outlineOffset: outline, secondOutlineOffset: second, fadeOffset: fade, backgroundOffset: background, inlineOffset: inline, edgeTone: edge, neutralOutline: neutralOutline, neutralSecondOutline: neutralSecond, neutralBackground: neutralBackground, neutralInline: neutralInline, preferLightHighlight: preferLightHighlight, secondaryFillOffset: secondary ?? fade, tertiaryFillOffset: tertiary ?? -fade)
    }

    private static func effectiveMl(_ can: CanItem) -> Int { Int((Double(max(can.volumeMl ?? 400, 0)) * Double(min(max(can.fillPercent ?? 100, 0), 100)) / 100).rounded()) }
    private static func availability(_ color: InventoryPaintColor, _ inventory: [InventoryPaintColor]) -> Double { Double(color.effectiveMl) / Double(max(inventory.map(\.effectiveMl).max() ?? 1, 1)) }
    private static func isTransparent(_ color: OfficialCanColor) -> Bool { color.colorName.lowercased().contains("transparent") || color.colorName.lowercased().contains("clear") }
    private static func normalizeHue(_ hue: Double) -> Double { (hue.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360) }
    private static func hueDistance(_ first: Double, _ second: Double) -> Double { min(abs(first - second), 360 - abs(first - second)) }
    private static func distance(_ first: HarmonyCandidate, _ second: HarmonyCandidate) -> Double {
        let a = rgb(first.hex), b = rgb(second.hex)
        return sqrt(pow(a.r - b.r, 2) + pow(a.g - b.g, 2) + pow(a.b - b.b, 2)) / sqrt(3)
    }

    private static func hsl(_ hex: String) -> HSL {
        let value = rgb(hex)
        let maximum = max(value.r, max(value.g, value.b)), minimum = min(value.r, min(value.g, value.b))
        let delta = maximum - minimum, lightness = (maximum + minimum) / 2
        let saturation = delta == 0 ? 0 : delta / (1 - abs(2 * lightness - 1))
        let hue: Double
        if delta == 0 { hue = 0 }
        else if maximum == value.r { hue = 60 * ((value.g - value.b) / delta).truncatingRemainder(dividingBy: 6) }
        else if maximum == value.g { hue = 60 * ((value.b - value.r) / delta + 2) }
        else { hue = 60 * ((value.r - value.g) / delta + 4) }
        return HSL(hue: normalizeHue(hue), saturation: saturation, lightness: lightness)
    }

    private static func rgb(_ hex: String) -> RGB {
        let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        guard clean.count >= 6, let value = Int(clean.prefix(6), radix: 16) else { return RGB(r: 0.35, g: 0.89, b: 0.76) }
        return RGB(r: Double((value >> 16) & 0xFF) / 255, g: Double((value >> 8) & 0xFF) / 255, b: Double(value & 0xFF) / 255)
    }

    private static func contrast(_ firstHex: String, _ secondHex: String) -> Double {
        let first = luminance(rgb(firstHex)), second = luminance(rgb(secondHex))
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private static func luminance(_ value: RGB) -> Double {
        func linear(_ component: Double) -> Double { component <= 0.04045 ? component / 12.92 : pow((component + 0.055) / 1.055, 2.4) }
        return 0.2126 * linear(value.r) + 0.7152 * linear(value.g) + 0.0722 * linear(value.b)
    }

    private static func combinationsCapped(_ n: Int, _ k: Int) -> Int64 {
        guard k >= 0, k <= n else { return 0 }
        let selected = min(k, n - k)
        var result: Int64 = 1
        guard selected > 0 else { return result }
        for index in 1...selected {
            let multiplier = Int64(n - selected + index)
            if result > 999_999_999 / multiplier { return 999_999_999 }
            result = result * multiplier / Int64(index)
        }
        return min(result, 999_999_999)
    }
}
