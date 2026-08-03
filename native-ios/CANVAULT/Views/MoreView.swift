import SwiftUI

struct MoreView: View {
    @EnvironmentObject private var store: InventoryStore
    let onMarket: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Mehr").font(.largeTitle.bold())
                    Text("Markt, Export und App-Informationen").foregroundStyle(.secondary)
                }
                Button {
                    Feedback.shared.play(.standard); onMarket()
                } label: { Label("Can-Markt öffnen", systemImage: "storefront.fill") }
                    .buttonStyle(CVPrimaryButtonStyle())

                if let exportURL = store.csvExportURL() {
                    ShareLink(item: exportURL) {
                        Label("Gesamte History als CSV teilen", systemImage: "square.and.arrow.up")
                            .font(.headline).frame(maxWidth: .infinity, minHeight: 50)
                    }
                    .buttonStyle(.bordered)
                }

                CVSectionTitle(title: "Über CANVAULT")
                VStack(spacing: 0) {
                    infoRow("icloud.slash.fill", "Offline-first", "Inventar und Fotos bleiben lokal verfügbar.")
                    Divider().padding(.leading, 52)
                    infoRow("lock.shield.fill", "Private Kamera", "Codes werden direkt auf dem iPhone verarbeitet.")
                    Divider().padding(.leading, 52)
                    infoRow("iphone", "Native iOS-App", "Version 1.9.3 · Build 23 · SwiftUI")
                }
                .cvCard()
            }
            .padding(16).padding(.bottom, 32)
        }
        .background(Color(.systemGroupedBackground))
        .toolbar(.hidden, for: .navigationBar)
    }

    private func infoRow(_ icon: String, _ title: String, _ body: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon).font(.title3).foregroundStyle(CVColor.mint).frame(width: 28)
            VStack(alignment: .leading, spacing: 3) { Text(title).font(.headline); Text(body).font(.caption).foregroundStyle(.secondary) }
            Spacer()
        }
        .padding(16).accessibilityElement(children: .combine)
    }
}
