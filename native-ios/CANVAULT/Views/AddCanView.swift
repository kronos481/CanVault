import PhotosUI
import SwiftUI
import UIKit

struct AddCanView: View {
    @EnvironmentObject private var store: InventoryStore
    @Binding var prefill: ScanPrefill?
    let onScan: () -> Void
    let onSaved: () -> Void

    @State private var brandId = "mtn-montana-colors"
    @State private var lineId = "mtn-montana-colors:mtn-94"
    @State private var colorName = ""
    @State private var colorCode = ""
    @State private var customHex = ""
    @State private var volume = "400"
    @State private var price = ""
    @State private var priceManuallyEdited = false
    @State private var fillPercent = 100.0
    @State private var quantity = 1
    @State private var photoFilename: String?
    @State private var photoItem: PhotosPickerItem?
    @State private var externalBarcode: String?
    @State private var scanMessage: String?
    @State private var saving = false
    @State private var errorMessage: String?

    private let catalog = CatalogStore.shared
    private var selectedBrand: CatalogBrand { catalog.brand(brandId) ?? catalog.brands[0] }
    private var selectedLine: CatalogLine { catalog.line(lineId) ?? selectedBrand.lines[0] }
    private var officialMatch: OfficialCanColor? { catalog.findColor(lineId: lineId, name: colorName, code: colorCode) }
    private var resolvedHex: String? { officialMatch?.hex ?? CatalogStore.normalizeHex(customHex) ?? selectedLine.defaultColorHex }
    private var hexValid: Bool { customHex.isEmpty || CatalogStore.normalizeHex(customHex) != nil }
    private var formValid: Bool { !colorName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && hexValid && !saving }
    private var suggestions: [OfficialCanColor] { catalog.searchColors(lineId: lineId, query: colorName, limit: 8) }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                header
                if let scanMessage { scanNotice(scanMessage) }
                if let externalBarcode { barcodeField(externalBarcode) }
                productSection
                colorSection
                photoSection
                amountSection
                previewCard
                saveButton
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 32)
        }
        .background(Color(.systemGroupedBackground))
        .toolbar(.hidden, for: .navigationBar)
        .onAppear(perform: consumePrefill)
        .onChange(of: prefill) { _ in consumePrefill() }
        .onChange(of: photoItem) { item in
            guard let item else { return }
            Task {
                do {
                    guard let data = try await item.loadTransferable(type: Data.self),
                          let image = UIImage(data: data),
                          let jpeg = image.jpegData(compressionQuality: 0.9) else {
                        throw CocoaError(.fileReadCorruptFile)
                    }
                    photoFilename = try store.savePhoto(jpeg)
                    Feedback.shared.play(.success)
                } catch {
                    errorMessage = "Das Foto konnte nicht übernommen werden."
                }
            }
        }
        .alert("Hinzufügen nicht möglich", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: { Text(errorMessage ?? "Unbekannter Fehler") }
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text("Dose hinzufügen").font(.largeTitle.bold())
                Text("Daten prüfen und lokal speichern").foregroundStyle(.secondary)
            }
            Spacer()
            Button {
                Feedback.shared.play(.scan)
                onScan()
            } label: { Label("Barcode", systemImage: "barcode.viewfinder").frame(minHeight: 44) }
                .buttonStyle(.bordered)
        }
    }

    private func scanNotice(_ message: String) -> some View {
        Label(message, systemImage: "checkmark.circle.fill")
            .font(.subheadline)
            .foregroundStyle(Color(hex: "#00382E"))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(CVColor.mint.opacity(0.8))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .accessibilityAddTraits(.isStaticText)
    }

    private func barcodeField(_ barcode: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Label("Produkt-Barcode", systemImage: "barcode")
                .font(.caption.weight(.semibold)).foregroundStyle(.secondary)
            Text(barcode).font(.body.monospaced()).textSelection(.enabled)
            Text("Wird mit diesem Produkt gespeichert und beim nächsten Scan erkannt.").font(.caption).foregroundStyle(.secondary)
        }
        .padding(14)
        .cvCard(radius: 16)
    }

    private var productSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            CVSectionTitle(title: "Produkt")
            Menu {
                ForEach(catalog.brands) { brand in
                    Button(brand.displayName) { selectBrand(brand.id) }
                }
            } label: { selectionField(label: "Marke", value: selectedBrand.displayName) }
            Menu {
                ForEach(selectedBrand.lines) { line in
                    Button(line.displayName) { selectLine(line.id) }
                }
            } label: { selectionField(label: "Dosenlinie", value: selectedLine.displayName) }
        }
    }

    private var colorSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            CVSectionTitle(title: "Farbe", subtitle: "\(catalog.colors(for: lineId).count) echte Farbtöne für diese Linie")
            VStack(alignment: .leading, spacing: 6) {
                Text("Farbname").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                TextField("z. B. Shock Blue", text: $colorName)
                    .textInputAutocapitalization(.words)
                    .padding(.horizontal, 13).frame(minHeight: 50)
                    .background(Color.secondary.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 13))
            }
            if !colorName.isEmpty && officialMatch == nil && !suggestions.isEmpty {
                VStack(spacing: 0) {
                    ForEach(suggestions) { color in
                        Button { selectColor(color) } label: {
                            HStack {
                                Circle().fill(Color(hex: color.hex)).frame(width: 26, height: 26).overlay(Circle().stroke(Color.primary.opacity(0.15)))
                                VStack(alignment: .leading) {
                                    Text(color.colorName).foregroundStyle(.primary)
                                    Text(color.colorCode ?? color.productCode ?? color.hex).font(.caption.monospaced()).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "arrow.up.left").foregroundStyle(.secondary)
                            }
                            .padding(.horizontal, 12).frame(minHeight: 52)
                        }
                        .buttonStyle(.plain)
                        if color.id != suggestions.last?.id { Divider().padding(.leading, 50) }
                    }
                }
                .cvCard(radius: 14)
            }
            field(label: "Farb- oder Produktcode", placeholder: "Code", text: $colorCode)
            VStack(alignment: .leading, spacing: 6) {
                field(label: "Eigener HEX-Wert (optional)", placeholder: "#58E4C2", text: $customHex)
                if !hexValid { Text("Format: #RRGGBB").font(.caption).foregroundStyle(.red) }
            }
            if let match = officialMatch, let source = catalog.source(for: match) {
                HStack(alignment: .top, spacing: 10) {
                    Circle().fill(Color(hex: match.hex)).frame(width: 38, height: 38).overlay(Circle().stroke(Color.primary.opacity(0.15)))
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Exakter digitaler Herstellerwert").font(.subheadline.weight(.semibold))
                        Text("\(match.hex) · Quelle: \(source.label)").font(.caption).foregroundStyle(.secondary)
                        Text("Lack kann je nach Untergrund, Licht und Display abweichen.").font(.caption2).foregroundStyle(.secondary)
                    }
                }
                .padding(14).cvCard(radius: 16)
            }
        }
    }

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            CVSectionTitle(title: "Foto", subtitle: "Optionales eigenes Produktfoto")
            PhotosPicker(selection: $photoItem, matching: .images) {
                Label(photoFilename == nil ? "Foto auswählen" : "Foto ersetzen", systemImage: "photo.badge.plus")
                    .font(.headline).frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.bordered)
        }
    }

    private var amountSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            CVSectionTitle(title: "Menge & Zustand")
            VStack(alignment: .leading, spacing: 8) {
                HStack { Text("Füllstand").font(.headline); Spacer(); Text("\(Int(fillPercent)) %").font(.title3.bold()).foregroundStyle(resolvedHex.map { Color(hex: $0) } ?? CVColor.mint).monospacedDigit() }
                Slider(value: $fillPercent, in: 0...100, step: 5) { Text("Füllstand") } minimumValueLabel: { Text("0") } maximumValueLabel: { Text("100") }
            }
            .padding(14).cvCard(radius: 16)

            HStack(spacing: 12) {
                numericField(label: "Volumen (ml)", value: $volume)
                numericField(label: "Preis (€)", value: Binding(get: { price }, set: { price = $0; priceManuallyEdited = true }))
            }
            Stepper("Anzahl: \(quantity)", value: $quantity, in: 1...99)
                .font(.headline).padding(14).cvCard(radius: 16)
        }
    }

    private var previewCard: some View {
        VStack(spacing: 0) {
            Color(hex: resolvedHex ?? "#58E4C2").frame(height: 8)
            HStack(spacing: 16) {
                let preview = CanItem(brandId: brandId, canLineId: lineId, colorName: colorName.isEmpty ? "Farbe" : colorName, colorCode: colorCode.nilIfBlank, customHex: customHex.nilIfBlank, volumeMl: Int(volume), fillPercent: Int(fillPercent), photoFilename: photoFilename)
                CanArtwork(can: preview).frame(width: 86, height: 128)
                VStack(alignment: .leading, spacing: 6) {
                    BrandLogo(brandId: brandId)
                    Text(selectedLine.displayName).font(.headline)
                    Text(colorName.isEmpty ? "Farbe auswählen" : colorName).foregroundStyle(.secondary)
                    Text("\(Int(fillPercent)) % · \(volume) ml").font(.caption.weight(.semibold)).foregroundStyle(resolvedHex.map { Color(hex: $0) } ?? CVColor.mint)
                }
                Spacer()
            }
            .padding(16)
        }
        .cvCard(radius: 20)
    }

    private var saveButton: some View {
        Button(action: save) {
            if saving { ProgressView().tint(Color(hex: "#00382E")) }
            else { Label("\(quantity) \(quantity == 1 ? "Dose" : "Dosen") speichern", systemImage: "checkmark.circle.fill") }
        }
        .buttonStyle(CVPrimaryButtonStyle())
        .disabled(!formValid)
    }

    private func selectionField(label: String, value: String) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) { Text(label).font(.caption).foregroundStyle(.secondary); Text(value).font(.body.weight(.medium)).foregroundStyle(.primary) }
            Spacer()
            Image(systemName: "chevron.up.chevron.down").foregroundStyle(.secondary)
        }
        .padding(.horizontal, 14).frame(minHeight: 54).background(Color.secondary.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func field(label: String, placeholder: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(label).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
            TextField(placeholder, text: text).textInputAutocapitalization(.characters).padding(.horizontal, 13).frame(minHeight: 50).background(Color.secondary.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 13))
        }
    }

    private func numericField(label: String, value: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(label).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
            TextField("0", text: value).keyboardType(.decimalPad).padding(.horizontal, 13).frame(minHeight: 50).background(Color.secondary.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 13))
        }
        .frame(maxWidth: .infinity)
    }

    private func selectBrand(_ id: String) {
        brandId = id
        guard let first = catalog.brand(id)?.lines.first else { return }
        selectLine(first.id)
        Feedback.shared.play(.standard)
    }

    private func selectLine(_ id: String) {
        lineId = id
        let line = catalog.line(id)
        volume = String(catalog.displayVolume(lineId: id))
        colorName = line?.defaultColorName ?? ""
        colorCode = line?.defaultColorCode ?? ""
        customHex = line?.defaultColorHex ?? ""
        priceManuallyEdited = false
        updateSuggestedPrice()
        Feedback.shared.play(.standard)
    }

    private func selectColor(_ color: OfficialCanColor) {
        colorName = color.colorName
        colorCode = color.colorCode ?? color.productCode ?? ""
        customHex = ""
        Feedback.shared.play(.color)
    }

    private func updateSuggestedPrice() {
        guard !priceManuallyEdited, let volumeValue = Int(volume), let cents = catalog.suggestedPrice(lineId: lineId, volumeMl: volumeValue) else { return }
        price = String(format: "%.2f", Double(cents) / 100).replacingOccurrences(of: ".", with: ",")
    }

    private func consumePrefill() {
        guard let value = prefill else { updateSuggestedPrice(); return }
        if let brand = value.brandId, catalog.brand(brand) != nil { brandId = brand }
        if let line = value.lineId, catalog.line(line) != nil { lineId = line }
        if let name = value.colorName { colorName = name }
        if let code = value.colorCode { colorCode = code }
        if let hex = value.customHex { customHex = hex }
        if let value = value.volumeMl { volume = String(value) }
        if let cents = value.purchasePriceCents {
            price = String(format: "%.2f", Double(cents) / 100).replacingOccurrences(of: ".", with: ",")
            priceManuallyEdited = true
        } else {
            priceManuallyEdited = false
            updateSuggestedPrice()
        }
        externalBarcode = value.externalBarcode
        scanMessage = value.message
        prefill = nil
    }

    private func save() {
        guard formValid else { errorMessage = "Bitte gib mindestens einen Farbnamen und einen gültigen HEX-Wert ein."; return }
        saving = true
        let normalizedPrice = price.replacingOccurrences(of: ",", with: ".")
        let cents = Double(normalizedPrice).map { Int(($0 * 100).rounded()) }
        let code = officialMatch?.colorCode ?? officialMatch?.productCode ?? colorCode.nilIfBlank
        let request = AddCanRequest(
            brandId: brandId, canLineId: lineId, colorName: colorName,
            colorCode: code, customHex: officialMatch == nil ? customHex.nilIfBlank : nil,
            volumeMl: Int(volume), fillPercent: Int(fillPercent), quantity: quantity,
            purchasePriceCents: cents, photoFilename: photoFilename, externalBarcode: externalBarcode
        )
        store.add(request)
        Feedback.shared.play(.success)
        resetForm()
        saving = false
        onSaved()
    }

    private func resetForm() {
        colorName = ""; colorCode = ""; customHex = ""; fillPercent = 100; quantity = 1
        photoFilename = nil; photoItem = nil; externalBarcode = nil; scanMessage = nil; priceManuallyEdited = false
        updateSuggestedPrice()
    }
}
