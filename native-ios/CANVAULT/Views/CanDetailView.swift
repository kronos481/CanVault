import CoreImage
import CoreImage.CIFilterBuiltins
import SwiftUI
import UIKit

struct CanDetailView: View {
    @EnvironmentObject private var store: InventoryStore
    @Environment(\.dismiss) private var dismiss
    let canId: String
    @State private var fill = 100.0
    @State private var showArchiveConfirmation = false

    private var can: CanItem? { store.canItem(canId) }
    private var events: [CanEvent] { store.snapshot.events.filter { $0.canId == canId }.sorted { $0.occurredAt > $1.occurredAt } }

    var body: some View {
        Group {
            if let can {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 16) {
                        hero(can)
                        if can.status == .archived {
                            Label("Diese Dose liegt im Archiv. Stelle sie im Speicher wieder her, um sie zu bearbeiten.", systemImage: "archivebox.fill")
                                .font(.subheadline).foregroundStyle(CVColor.warning).padding(14).cvCard(radius: 14)
                        } else {
                            fillSection(can)
                            statusSection(can)
                        }
                        qrSection(can)
                        historySection
                        if can.status != .archived {
                            Button(role: .destructive) { showArchiveConfirmation = true; Feedback.shared.play(.standard) } label: {
                                Label("Dose archivieren", systemImage: "archivebox.fill").frame(maxWidth: .infinity, minHeight: 50)
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                    .padding(16).padding(.bottom, 28)
                }
                .background(Color(.systemGroupedBackground))
                .navigationTitle(can.colorName)
                .navigationBarTitleDisplayMode(.inline)
                .onAppear { fill = Double(can.fillPercent ?? 0) }
                .onChange(of: can.fillPercent) { value in fill = Double(value ?? 0) }
            } else {
                EmptyState(icon: "exclamationmark.triangle", title: "Dose nicht gefunden", bodyText: "Dieser Eintrag wurde möglicherweise endgültig gelöscht.")
                    .padding(16)
            }
        }
        .confirmationDialog("Dose archivieren?", isPresented: $showArchiveConfirmation, titleVisibility: .visible) {
            Button("Archivieren", role: .destructive) {
                store.archive(canId: canId); Feedback.shared.play(.archive); dismiss()
            }
            Button("Abbrechen", role: .cancel) { }
        } message: { Text("Die Dose bleibt gespeichert und kann im Speicher jederzeit wiederhergestellt werden.") }
    }

    private func hero(_ can: CanItem) -> some View {
        let accent = Color(hex: CatalogStore.shared.resolveHex(for: can) ?? "#58E4C2")
        return VStack(spacing: 0) {
            accent.frame(height: 8)
            HStack(spacing: 18) {
                CanArtwork(can: can).frame(width: 116, height: 176)
                VStack(alignment: .leading, spacing: 7) {
                    BrandLogo(brandId: can.brandId)
                    Text(CatalogStore.shared.lineName(can.canLineId)).font(.title2.bold())
                    Text(can.colorName).font(.headline)
                    if let code = can.colorCode { Text(code).font(.subheadline.monospaced()).foregroundStyle(.secondary) }
                    if let volume = can.volumeMl { Text("\(volume) ml").foregroundStyle(.secondary) }
                }
                Spacer(minLength: 0)
            }
            .padding(18)
        }
        .cvCard(radius: 24)
    }

    private func fillSection(_ can: CanItem) -> some View {
        let accent = Color(hex: CatalogStore.shared.resolveHex(for: can) ?? "#58E4C2")
        return VStack(alignment: .leading, spacing: 10) {
            CVSectionTitle(title: "Füllstand")
            VStack(alignment: .leading, spacing: 8) {
                Text("\(Int(fill)) %").font(.largeTitle.bold()).foregroundStyle(accent).monospacedDigit()
                Slider(value: $fill, in: 0...100, step: 5, onEditingChanged: { editing in
                    if !editing { store.updateFill(canId: can.id, percent: Int(fill)); Feedback.shared.play(.standard) }
                })
                Text("Geschätzter Wert – jederzeit anpassbar").font(.caption).foregroundStyle(.secondary)
            }
            .padding(16).cvCard()
        }
    }

    private func statusSection(_ can: CanItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            CVSectionTitle(title: "Status")
            FlowLayout(spacing: 8) {
                ForEach(CanStatus.allCases.filter { $0 != .archived }) { status in
                    CapsuleChip(title: status.label, selected: can.status == status, icon: nil) {
                        store.updateStatus(canId: can.id, status: status); Feedback.shared.play(.standard)
                    }
                }
            }
        }
    }

    private func qrSection(_ can: CanItem) -> some View {
        let content = CanVaultQR.encode(can)
        return VStack(alignment: .leading, spacing: 10) {
            CVSectionTitle(title: "CANVAULT QR", subtitle: "Überträgt Produkt und exakte Farbdaten")
            if let image = qrImage(content) {
                Image(uiImage: image).interpolation(.none).resizable().scaledToFit().padding(24).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 18))
                    .accessibilityLabel("QR-Code für \(can.colorName)")
            }
            ShareLink(item: content, subject: Text("CANVAULT – \(can.colorName)")) {
                Label("Code teilen", systemImage: "square.and.arrow.up").font(.headline).frame(maxWidth: .infinity, minHeight: 50)
            }
            .buttonStyle(.bordered)
        }
    }

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            CVSectionTitle(title: "Verlauf")
            if events.isEmpty { Text("Noch keine Ereignisse").foregroundStyle(.secondary) }
            else {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(events) { event in
                        HStack(alignment: .top, spacing: 12) {
                            Circle().fill(CVColor.mint).frame(width: 9, height: 9).padding(.top, 5)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(event.description).font(.subheadline.weight(.medium))
                                Text(Date(millisecondsSince1970: event.occurredAt).formatted(date: .abbreviated, time: .shortened)).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .padding(16).cvCard()
            }
        }
    }

    private func qrImage(_ value: String) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(value.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 12, y: 12)) else { return nil }
        let context = CIContext()
        guard let cgImage = context.createCGImage(output, from: output.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
