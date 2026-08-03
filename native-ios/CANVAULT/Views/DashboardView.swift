import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var store: InventoryStore
    let onScan: () -> Void
    let onAdd: () -> Void
    let onColorCombo: () -> Void
    let onMarket: () -> Void
    let onOpenCan: (String) -> Void

    private var active: [CanItem] { store.snapshot.cans.filter { $0.status != .archived } }
    private var remainingMl: Int { active.reduce(0) { $0 + StorageStatistics.remainingMl($1) } }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                header
                quickCapture
                actionCard(title: "Color Combo", subtitle: "Harmonische Paletten aus Füllstand, Menge und deinen exakten Farben", icon: "paintpalette.fill", accent: CVColor.mint, action: onColorCombo)
                actionCard(title: "Can-Markt", subtitle: "Alle Dosenlinien, verifizierter Katalog und Preisvergleich", icon: "storefront.fill", accent: CVColor.warning, action: onMarket)

                CVSectionTitle(title: "Bestand")
                HStack(spacing: 12) {
                    StatCard(label: "Aktive Dosen", value: "\(active.count)")
                    StatCard(label: "Geschätzt übrig", value: "\(remainingMl) ml", accent: CVColor.warning)
                }
                HStack(spacing: 12) {
                    StatCard(label: "Farben", value: "\(Set(active.map { $0.colorName.lowercased() }).count)")
                    StatCard(label: "Dosenlinien", value: "\(Set(active.map(\.canLineId)).count)")
                }

                CVSectionTitle(title: "Zuletzt hinzugefügt")
                if active.isEmpty {
                    EmptyState(icon: "shippingbox", title: "Noch keine Dose im Vault", bodyText: "Scanne einen Produkt-Barcode oder lege deine erste Dose manuell an.")
                } else {
                    ForEach(active.sorted { $0.createdAt > $1.createdAt }.prefix(4)) { can in
                        CanCard(can: can) { onOpenCan(can.id) }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(Color(.systemGroupedBackground))
        .toolbar(.hidden, for: .navigationBar)
        .accessibilityIdentifier("dashboard")
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image("AppLogo").resizable().scaledToFit().frame(width: 54, height: 54)
                .background(Color.white).clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .accessibilityLabel("CANVAULT Logo")
            VStack(alignment: .leading, spacing: 2) {
                Text("CANVAULT").font(.title2.bold())
                Text("Deine Farben. Dein Bestand.").font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer()
        }
    }

    private var quickCapture: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Schnell erfassen").font(.title2.bold())
            Text("Produkt-Barcode scannen, Daten bestätigen und die Dose lokal speichern.").font(.subheadline).foregroundStyle(.secondary)
            Button {
                Feedback.shared.play(.scan)
                onScan()
            } label: { Label("Barcode scannen", systemImage: "barcode.viewfinder") }
                .buttonStyle(CVPrimaryButtonStyle())
            Button {
                Feedback.shared.play(.standard)
                onAdd()
            } label: {
                Label("Manuell hinzufügen", systemImage: "plus")
                    .font(.headline).frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.bordered)
        }
        .padding(20)
        .background(
            LinearGradient(colors: [CVColor.mint.opacity(0.22), Color.clear], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .cvCard(radius: 24)
    }

    private func actionCard(title: String, subtitle: String, icon: String, accent: Color, action: @escaping () -> Void) -> some View {
        Button {
            Feedback.shared.play(title == "Color Combo" ? .shake : .standard)
            action()
        } label: {
            HStack(spacing: 14) {
                Image(systemName: icon).font(.system(size: 30, weight: .semibold)).foregroundStyle(accent).frame(width: 40)
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(.headline).foregroundStyle(.primary)
                    Text(subtitle).font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.leading)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
            }
            .padding(18)
            .cvCard()
        }
        .buttonStyle(CVPressStyle())
    }
}
