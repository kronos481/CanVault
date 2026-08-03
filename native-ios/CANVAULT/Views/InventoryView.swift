import SwiftUI

private enum InventorySortMode: String, CaseIterable, Identifiable {
    case newest = "Neueste"
    case name = "Name"
    case color = "Nach Farbe"
    case fill = "Füllstand"
    var id: String { rawValue }
}

struct InventoryView: View {
    @EnvironmentObject private var store: InventoryStore
    @State private var query = ""
    @State private var selectedStatus: CanStatus?
    @State private var sortMode: InventorySortMode = .newest
    let onScan: () -> Void
    let onColorCombo: () -> Void
    let onOpenCan: (String) -> Void

    private var activeCount: Int { store.snapshot.cans.filter { $0.status != .archived }.count }
    private var visible: [CanItem] {
        let catalog = CatalogStore.shared
        let filtered = store.snapshot.cans.filter { can in
            guard can.status != .archived else { return false }
            guard selectedStatus == nil || can.status == selectedStatus else { return false }
            guard !query.isEmpty else { return true }
            return [catalog.brandName(can.brandId), catalog.lineName(can.canLineId), can.colorName, can.colorCode ?? "", can.externalBarcode ?? ""]
                .contains { $0.localizedCaseInsensitiveContains(query) }
        }
        switch sortMode {
        case .newest: return filtered.sorted { $0.createdAt > $1.createdAt }
        case .name: return filtered.sorted { "\(catalog.brandName($0.brandId)) \($0.colorName)".localizedCaseInsensitiveCompare("\(catalog.brandName($1.brandId)) \($1.colorName)") == .orderedAscending }
        case .fill: return filtered.sorted { ($0.fillPercent ?? -1) > ($1.fillPercent ?? -1) }
        case .color: return filtered.sorted { colorSortKey($0) < colorSortKey($1) }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVStack(spacing: 12) {
                    header
                    comboButton
                    search
                    statusFilters
                    sortFilters
                    if visible.isEmpty {
                        EmptyState(icon: "magnifyingglass", title: query.isEmpty ? "Inventar ist leer" : "Keine Treffer", bodyText: query.isEmpty ? "Füge deine erste Dose hinzu oder scanne einen Barcode." : "Passe Suche oder Filter an.")
                    } else {
                        ForEach(visible) { can in CanCard(can: can) { onOpenCan(can.id) } }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 28)
            }
        }
        .background(Color(.systemGroupedBackground))
        .toolbar(.hidden, for: .navigationBar)
        .animation(.easeOut(duration: 0.22), value: visible.map(\.id))
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 3) {
                Text("Inventar").font(.largeTitle.bold())
                Text("\(visible.count) von \(activeCount) Dosen").foregroundStyle(.secondary)
            }
            Spacer()
            Button {
                Feedback.shared.play(.scan)
                onScan()
            } label: { Label("Barcode", systemImage: "barcode.viewfinder").frame(minHeight: 44) }
                .buttonStyle(.bordered)
        }
    }

    private var comboButton: some View {
        Button {
            Feedback.shared.play(.shake)
            onColorCombo()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "paintpalette.fill").font(.title2)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Color Combo").font(.headline)
                    Text("Paletten aus deinem echten Bestand").font(.caption)
                }
                Spacer()
                Image(systemName: "chevron.right")
            }
            .foregroundStyle(Color(hex: "#00382E"))
            .padding(16)
            .background(CVColor.mint)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(CVPressStyle())
    }

    private var search: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
            TextField("Marke, Linie, Farbe oder Code", text: $query)
                .textInputAutocapitalization(.never)
                .submitLabel(.search)
            if !query.isEmpty {
                Button { query = "" } label: { Image(systemName: "xmark.circle.fill").frame(width: 44, height: 44) }
                    .foregroundStyle(.secondary).accessibilityLabel("Suche leeren")
            }
        }
        .padding(.leading, 14)
        .frame(minHeight: 52)
        .background(Color.secondary.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
    }

    private var statusFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                CapsuleChip(title: "Alle", selected: selectedStatus == nil, icon: nil) { selectedStatus = nil }
                ForEach(CanStatus.allCases.filter { $0 != .archived }) { status in
                    CapsuleChip(title: status.label, selected: selectedStatus == status, icon: nil) {
                        selectedStatus = selectedStatus == status ? nil : status
                    }
                }
            }
        }
    }

    private var sortFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(InventorySortMode.allCases) { mode in
                    CapsuleChip(title: mode.rawValue, selected: sortMode == mode, icon: mode == .color ? "paintpalette" : nil) { sortMode = mode }
                }
            }
        }
    }

    private func colorSortKey(_ can: CanItem) -> String {
        guard let hex = CatalogStore.shared.resolveHex(for: can), let value = Int(hex.dropFirst(), radix: 16) else { return "2-\(can.colorName.lowercased())" }
        let r = Double((value >> 16) & 0xFF) / 255, g = Double((value >> 8) & 0xFF) / 255, b = Double(value & 0xFF) / 255
        let maximum = max(r, max(g, b)), minimum = min(r, min(g, b)), delta = maximum - minimum
        let lightness = (maximum + minimum) / 2
        let hue: Double
        if delta == 0 { hue = 0 }
        else if maximum == r { hue = 60 * ((g - b) / delta).truncatingRemainder(dividingBy: 6) }
        else if maximum == g { hue = 60 * ((b - r) / delta + 2) }
        else { hue = 60 * ((r - g) / delta + 4) }
        let normalized = (hue + 375).truncatingRemainder(dividingBy: 360)
        let neutral = delta < 0.08
        return String(format: "%d-%07.2f-%07.3f-%@", neutral ? 1 : 0, neutral ? 0 : normalized, -lightness, can.colorName.lowercased())
    }
}
