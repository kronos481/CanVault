import SwiftUI

private enum ColorComboMode: String, CaseIterable, Identifiable {
    case inventory = "Nur Bestand"
    case addColor = "Add Color"
    var id: String { rawValue }
}

private struct ComboGenerationKey: Hashable {
    var inventoryFingerprint: Int
    var mode: ColorComboMode
    var tones: Int
}

struct ColorComboView: View {
    @EnvironmentObject private var store: InventoryStore
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var mode: ColorComboMode = .inventory
    @State private var tones = 5.0
    @State private var analysis: ColorComboAnalysis?
    @State private var calculating = true

    private var key: ComboGenerationKey {
        ComboGenerationKey(inventoryFingerprint: store.snapshot.cans.hashValue, mode: mode, tones: Int(tones))
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                toneSelector
                modeSelector
                if calculating || analysis == nil {
                    loadingCard
                } else if let analysis {
                    summary(analysis)
                    if analysis.unresolvedCanCount > 0 { unresolvedNotice(analysis.unresolvedCanCount) }
                    if analysis.inventoryColors.isEmpty {
                        EmptyState(icon: "paintpalette", title: "Keine exakten Farben", bodyText: "Füge Farbnamen, Produktcodes oder HEX-Werte hinzu, damit CANVAULT echte Töne berechnen kann.")
                    } else if analysis.palettes.isEmpty {
                        EmptyState(icon: "circle.slash", title: "Noch keine passende Palette", bodyText: "Probiere weniger Töne oder ergänze eine Farbe im Add-Color-Modus.")
                    } else {
                        CVSectionTitle(title: mode == .inventory ? "Generierte Bestands-Combos" : "Generierte Kauf-Combos")
                        ForEach(analysis.palettes) { palette in PaletteCard(palette: palette) }
                    }
                }
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Color Combo")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: key) { await generate() }
    }

    private var toneSelector: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Anzahl Farbtöne").font(.headline)
                    Text("Rollen werden automatisch passend verteilt").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Text("\(Int(tones))").font(.title2.bold()).foregroundStyle(CVColor.mint).monospacedDigit()
            }
            Slider(value: $tones, in: 2...7, step: 1) { Text("Anzahl Farbtöne") } minimumValueLabel: { Text("2") } maximumValueLabel: { Text("7") }
        }
        .padding(16).cvCard()
    }

    private var modeSelector: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Modus").font(.headline)
            HStack(spacing: 8) {
                ForEach(ColorComboMode.allCases) { option in
                    CapsuleChip(title: option.rawValue, selected: mode == option, icon: option == .inventory ? "checkmark.circle" : "cart.badge.plus") {
                        mode = option
                        Feedback.shared.play(.shake)
                    }
                }
            }
            Text(mode == .inventory
                 ? "Nur vorhandene Dosen. Auch Best-Effort-Combos erscheinen mit einem ehrlichen 0–100-%-Score."
                 : "Vorhandene Farben zuerst, danach reale kaufbare Herstellerfarben. Fehlende Dosen sind gestrichelt markiert.")
                .font(.caption).foregroundStyle(.secondary)
        }
        .padding(16).cvCard()
    }

    private var loadingCard: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle().stroke(CVColor.mint.opacity(0.2), lineWidth: 7).frame(width: 68, height: 68)
                Circle().trim(from: 0.12, to: 0.76).stroke(CVColor.mint, style: StrokeStyle(lineWidth: 7, lineCap: .round)).frame(width: 68, height: 68)
                    .rotationEffect(.degrees(calculating && !reduceMotion ? 360 : 0))
                    .animation(reduceMotion ? nil : .linear(duration: 0.8).repeatForever(autoreverses: false), value: calculating)
                Image(systemName: "paintpalette.fill").foregroundStyle(CVColor.mint)
            }
            Text("Color Combo wird berechnet").font(.headline)
            Text("Kontrast, Farbabstand, Füllmenge und Rollen werden gegeneinander geprüft.").font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity).padding(28).cvCard()
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Color Combo wird berechnet")
    }

    private func summary(_ value: ColorComboAnalysis) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Berechnung abgeschlossen", systemImage: "checkmark.seal.fill").font(.headline).foregroundStyle(CVColor.mint)
            HStack(spacing: 10) {
                StatCard(label: "Exakte Farben", value: "\(value.inventoryColors.count)")
                StatCard(label: "Effektiv übrig", value: "\(value.totalEffectiveMl) ml", accent: CVColor.warning)
            }
            Text("\(value.evaluatedCandidateCount.formatted()) Kandidaten geprüft · Wissensraum bis \(value.knowledgeBaseCandidateCount.formatted()) Kombinationen")
                .font(.caption).foregroundStyle(.secondary)
        }
    }

    private func unresolvedNotice(_ count: Int) -> some View {
        Label("\(count) \(count == 1 ? "Dose hat" : "Dosen haben") noch keine exakt auflösbare Farbe und wurde nicht eingerechnet.", systemImage: "info.circle.fill")
            .font(.subheadline).foregroundStyle(CVColor.warning).padding(14).cvCard(radius: 14)
    }

    @MainActor
    private func generate() async {
        calculating = true
        analysis = nil
        let cans = store.snapshot.cans
        let colors = CatalogStore.shared.colors
        let includeMissing = mode == .addColor
        let count = Int(tones)
        let started = Date()
        let result = await Task.detached(priority: .userInitiated) {
            ColorHarmonyEngine.analyze(cans: cans, officialColors: colors, includeMissingColors: includeMissing, toneCount: count)
        }.value
        guard !Task.isCancelled else { return }
        let minimum = includeMissing ? 0.18 : 0.22
        let remaining = minimum - Date().timeIntervalSince(started)
        if remaining > 0 { try? await Task.sleep(nanoseconds: UInt64(remaining * 1_000_000_000)) }
        guard !Task.isCancelled else { return }
        analysis = result
        calculating = false
    }
}

private struct PaletteCard: View {
    let palette: ColorHarmonyPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(palette.title).font(.headline)
                    Text(palette.rule).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Text("\(palette.scorePercent) %")
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(scoreColor)
                    .padding(.horizontal, 10).frame(minHeight: 36)
                    .background(scoreColor.opacity(0.12)).clipShape(Capsule())
                    .accessibilityLabel("Harmonie-Score \(palette.scorePercent) Prozent")
            }

            HStack(spacing: 0) {
                ForEach(palette.swatches) { swatch in
                    Color(hex: swatch.hex)
                        .frame(maxWidth: .infinity, minHeight: 74)
                        .overlay {
                            if !swatch.isOwned {
                                RoundedRectangle(cornerRadius: 0).stroke(Color.primary.opacity(0.9), style: StrokeStyle(lineWidth: 2, dash: [7, 5]))
                            }
                        }
                        .accessibilityLabel("\(swatch.role.displayName): \(swatch.label), \(swatch.hex)\(swatch.isOwned ? ", vorhanden" : ", kaufen")")
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(spacing: 8) {
                ForEach(palette.swatches) { swatch in
                    HStack(spacing: 10) {
                        RoundedRectangle(cornerRadius: 5).fill(Color(hex: swatch.hex)).frame(width: 28, height: 28)
                            .overlay { RoundedRectangle(cornerRadius: 5).stroke(Color.primary.opacity(0.15)) }
                        VStack(alignment: .leading, spacing: 2) {
                            Text(swatch.role.displayName).font(.caption.weight(.bold)).foregroundStyle(.secondary)
                            Text(swatch.label).font(.subheadline.weight(.semibold)).lineLimit(1)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 2) {
                            Text(swatch.productCode ?? swatch.hex).font(.caption.monospaced())
                            Text(swatch.isOwned ? "Bestand · \(swatch.effectiveMl) ml" : "Kaufen · ca. \(swatch.effectiveMl) ml")
                                .font(.caption2).foregroundStyle(swatch.isOwned ? CVColor.mint : CVColor.warning)
                        }
                    }
                    .padding(.vertical, 2)
                }
            }
            Text(palette.description).font(.caption).foregroundStyle(.secondary)
        }
        .padding(16).cvCard()
    }

    private var scoreColor: Color {
        if palette.scorePercent >= 80 { return CVColor.mint }
        if palette.scorePercent >= 60 { return CVColor.warning }
        return Color.orange
    }
}
