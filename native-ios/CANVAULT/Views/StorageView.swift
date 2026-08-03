import SwiftUI

private enum StorageSection: String, CaseIterable, Identifiable {
    case archive = "Archiv"
    case history = "History"
    var id: String { rawValue }
}

struct StorageView: View {
    @EnvironmentObject private var store: InventoryStore
    @State private var section: StorageSection = .archive
    @State private var pendingDeletion: CanItem?
    @State private var lastRestored: CanItem?
    let onOpenCan: (String) -> Void

    private var archived: [CanItem] { store.snapshot.cans.filter { $0.status == .archived }.sorted { ($0.archivedAt ?? $0.updatedAt) > ($1.archivedAt ?? $1.updatedAt) } }
    private var history: [CanItem] { store.snapshot.cans.sorted { $0.acquiredAt > $1.acquiredAt } }
    private var stats: StorageStats { StorageStatistics.calculate(cans: store.snapshot.cans) }

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Speicher").font(.largeTitle.bold())
                        Text("Archiv und vollständige Dosen-History an einem Ort").foregroundStyle(.secondary)
                    }
                    selector
                    if section == .archive { archiveContent } else { historyContent }
                }
                .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 90)
            }
            if let restored = lastRestored { undoBanner(restored) }
        }
        .background(Color(.systemGroupedBackground))
        .toolbar(.hidden, for: .navigationBar)
        .confirmationDialog("Dose endgültig löschen?", isPresented: Binding(get: { pendingDeletion != nil }, set: { if !$0 { pendingDeletion = nil } }), titleVisibility: .visible) {
            Button("Endgültig löschen", role: .destructive) {
                if let can = pendingDeletion { _ = store.deleteArchivedPermanently(canId: can.id); Feedback.shared.play(.destructive) }
                pendingDeletion = nil
            }
            Button("Abbrechen", role: .cancel) { pendingDeletion = nil }
        } message: {
            Text("„\(pendingDeletion?.colorName ?? "Diese Dose")“ wird aus Archiv, History und Statistiken entfernt. Diese Aktion kann nicht rückgängig gemacht werden.")
        }
    }

    private var selector: some View {
        HStack(spacing: 12) {
            sectionButton(.archive, count: archived.count, icon: "archivebox.fill")
            sectionButton(.history, count: history.count, icon: "clock.arrow.circlepath")
        }
    }

    private func sectionButton(_ option: StorageSection, count: Int, icon: String) -> some View {
        Button {
            section = option; Feedback.shared.play(.standard)
        } label: {
            VStack(alignment: .leading) {
                Image(systemName: icon).font(.title2).foregroundStyle(section == option ? Color(hex: "#00382E") : CVColor.mint)
                Spacer()
                HStack { Text(option.rawValue).font(.headline); Spacer(); Text("\(count)").font(.title2.bold()).monospacedDigit() }
            }
            .foregroundStyle(section == option ? Color(hex: "#00382E") : Color.primary)
            .padding(14).frame(maxWidth: .infinity, minHeight: 108)
            .background(section == option ? CVColor.mint : Color.secondary.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 22).stroke(section == option ? CVColor.mintDark : Color.primary.opacity(0.08), lineWidth: section == option ? 2 : 1) }
        }
        .buttonStyle(CVPressStyle()).accessibilityAddTraits(section == option ? .isSelected : [])
    }

    private var archiveContent: some View {
        Group {
            CVSectionTitle(title: "Archiv", subtitle: "Aus dem Bestand entfernt, aber sicher gespeichert und jederzeit wiederherstellbar.")
            if archived.isEmpty {
                EmptyState(icon: "archivebox", title: "Archiv ist leer", bodyText: "Archivierte Dosen erscheinen hier und können wiederhergestellt werden.")
            } else {
                ForEach(archived) { can in
                    VStack(spacing: 8) {
                        CanCard(can: can) { onOpenCan(can.id) }
                        HStack(spacing: 8) {
                            Button {
                                store.restore(canId: can.id); lastRestored = can; Feedback.shared.play(.success)
                                Task { try? await Task.sleep(nanoseconds: 5_000_000_000); if lastRestored?.id == can.id { lastRestored = nil } }
                            } label: { Label("Wiederherstellen", systemImage: "arrow.uturn.backward.circle.fill").frame(maxWidth: .infinity, minHeight: 48) }
                                .buttonStyle(.borderedProminent)
                            Button(role: .destructive) { pendingDeletion = can; Feedback.shared.play(.standard) } label: { Label("Löschen", systemImage: "trash.fill").frame(minHeight: 48) }
                                .buttonStyle(.bordered)
                        }
                    }
                }
            }
        }
    }

    private var historyContent: some View {
        Group {
            Label("Deine Gesamtstatistik", systemImage: "chart.bar.xaxis").font(.title2.bold()).foregroundStyle(CVColor.mint)
            HStack(spacing: 12) { StatCard(label: "Jemals besessen", value: "\(stats.allTimeCanCount)"); StatCard(label: "Aktuell", value: "\(stats.currentCanCount)") }
            HStack(spacing: 12) { StatCard(label: "Gesamtausgaben", value: currency(stats.totalSpentCents)); StatCard(label: "Aktueller Wert", value: currency(stats.currentInventoryValueCents), accent: CVColor.warning) }
            HStack(spacing: 12) { StatCard(label: "Verbraucht", value: volume(stats.estimatedUsedVolumeMl)); StatCard(label: "Noch vorhanden", value: volume(stats.currentRemainingVolumeMl)) }
            HStack(spacing: 12) { StatCard(label: "Farben", value: "\(stats.distinctColorCount)"); StatCard(label: "Ungeöffnet", value: "\(stats.unopenedPercent) %") }
            highlights
            monthlyActivity
            CVSectionTitle(title: "Alle jemals erfassten Dosen", subtitle: "\(history.count) Dosen · Archivierte Einträge sind blasser dargestellt.")
            if history.isEmpty {
                EmptyState(icon: "clock.arrow.circlepath", title: "Noch keine History", bodyText: "Sobald du eine Dose hinzufügst, bleibt sie in dieser Übersicht.")
            } else {
                ForEach(history) { can in CanCard(can: can, archivedStyle: can.status == .archived) { onOpenCan(can.id) } }
            }
        }
    }

    private var highlights: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Highlights").font(.headline)
            highlight("Meistgekaufte Marke", stats.topBrandId.map { CatalogStore.shared.brandName($0) } ?? "Noch keine Daten")
            Divider()
            highlight("Meistgekaufte Linie", stats.topLineId.map { CatalogStore.shared.lineName($0) } ?? "Noch keine Daten")
            Divider()
            highlight("Meistverbrauchte Farbe", stats.topUsedColorName ?? "Noch keine Nutzung erfasst")
            Divider()
            highlight("Ø Nutzungsdauer", stats.averageUsageDays.map { "\($0) Tage" } ?? "Noch nicht berechenbar")
            Divider()
            highlight("Insgesamt gekauft", volume(stats.purchasedVolumeMl))
        }
        .padding(16).cvCard()
    }

    private func highlight(_ label: String, _ value: String) -> some View {
        HStack { Text(label).font(.subheadline).foregroundStyle(.secondary); Spacer(); Text(value).font(.subheadline.weight(.semibold)).multilineTextAlignment(.trailing) }
    }

    private var monthlyActivity: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Letzte 6 Monate").font(.headline)
            Text("Neu erfasste Dosen und Ausgaben pro Monat").font(.caption).foregroundStyle(.secondary)
            let maximum = max(stats.monthlyActivity.map(\.addedCanCount).max() ?? 1, 1)
            ForEach(stats.monthlyActivity) { item in
                HStack(spacing: 10) {
                    Text(item.month.formatted(.dateTime.month(.abbreviated))).font(.caption).frame(width: 38, alignment: .leading)
                    GeometryReader { geometry in
                        Capsule().fill(CVColor.mint.opacity(0.18)).overlay(alignment: .leading) {
                            Capsule().fill(CVColor.mint).frame(width: max(4, geometry.size.width * CGFloat(item.addedCanCount) / CGFloat(maximum)))
                        }
                    }.frame(height: 9)
                    Text("\(item.addedCanCount)").font(.caption.bold()).monospacedDigit().frame(width: 20)
                }
            }
        }
        .padding(16).cvCard()
    }

    private func undoBanner(_ can: CanItem) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill").foregroundStyle(CVColor.mint)
            Text("Dose wiederhergestellt").font(.subheadline.weight(.semibold))
            Spacer()
            Button("Rückgängig") { store.archive(canId: can.id); lastRestored = nil; Feedback.shared.play(.archive) }.font(.subheadline.bold()).frame(minHeight: 44)
        }
        .padding(.horizontal, 16).padding(.vertical, 8).background(.ultraThinMaterial).clipShape(RoundedRectangle(cornerRadius: 16)).padding(16).shadow(radius: 10)
    }

    private func currency(_ cents: Int) -> String {
        let formatter = NumberFormatter(); formatter.numberStyle = .currency; formatter.currencyCode = "EUR"; formatter.locale = Locale(identifier: "de_DE")
        return formatter.string(from: NSNumber(value: Double(cents) / 100)) ?? "–"
    }
    private func volume(_ ml: Int) -> String { ml >= 1_000 ? String(format: "%.1f l", Double(ml) / 1_000) : "\(ml) ml" }
}
