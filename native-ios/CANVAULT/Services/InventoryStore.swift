import Foundation
import SwiftUI

@MainActor
final class InventoryStore: ObservableObject {
    @Published private(set) var snapshot = InventorySnapshot()

    private let fileManager: FileManager
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder
    private let inventoryURL: URL
    private let photoDirectory: URL

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
        encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        decoder = JSONDecoder()

        let root = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            .appendingPathComponent("CANVAULT", isDirectory: true)
        inventoryURL = root.appendingPathComponent("inventory-v2.json")
        photoDirectory = root.appendingPathComponent("can-photos", isDirectory: true)
        try? fileManager.createDirectory(at: photoDirectory, withIntermediateDirectories: true)
        if let data = try? Data(contentsOf: inventoryURL),
           let stored = try? decoder.decode(InventorySnapshot.self, from: data) {
            snapshot = stored
        }
    }

    @discardableResult
    func add(_ request: AddCanRequest) -> [String] {
        let count = min(max(request.quantity, 1), 99)
        let now = Date().millisecondsSince1970
        let created = (0..<count).map { _ in
            let fill = min(max(request.fillPercent, 0), 100)
            return CanItem(
                brandId: request.brandId,
                canLineId: request.canLineId,
                colorName: request.colorName.trimmingCharacters(in: .whitespacesAndNewlines),
                colorCode: request.colorCode?.nilIfBlank,
                customHex: request.customHex?.nilIfBlank?.uppercased(),
                volumeMl: request.volumeMl,
                fillPercent: fill,
                status: fill == 0 ? .empty : (fill == 100 ? .inStock : .opened),
                purchasePriceCents: request.purchasePriceCents,
                photoFilename: request.photoFilename,
                externalBarcode: request.externalBarcode,
                acquiredAt: now,
                createdAt: now,
                updatedAt: now
            )
        }
        let events = created.map {
            CanEvent(canId: $0.id, type: .created, description: "Dose hinzugefügt", occurredAt: now)
        }
        snapshot.cans.insert(contentsOf: created, at: 0)
        snapshot.events.insert(contentsOf: events, at: 0)
        persist()
        return created.map(\.id)
    }

    func updateFill(canId: String, percent: Int?) {
        guard let index = snapshot.cans.firstIndex(where: { $0.id == canId }) else { return }
        let normalized = percent.map { min(max($0, 0), 100) }
        snapshot.cans[index].fillPercent = normalized
        if normalized == 0 { snapshot.cans[index].status = .empty }
        snapshot.cans[index].updatedAt = Date().millisecondsSince1970
        addEvent(canId, .fillChanged, "Füllstand: \(normalized.map { "\($0) %" } ?? "unbekannt")")
        persist()
    }

    func updateStatus(canId: String, status: CanStatus) {
        guard let index = snapshot.cans.firstIndex(where: { $0.id == canId }), snapshot.cans[index].status != status else { return }
        snapshot.cans[index].status = status
        snapshot.cans[index].updatedAt = Date().millisecondsSince1970
        addEvent(canId, .statusChanged, "Status geändert: \(status.label)")
        persist()
    }

    func archive(canId: String) {
        guard let index = snapshot.cans.firstIndex(where: { $0.id == canId }), snapshot.cans[index].status != .archived else { return }
        let now = Date().millisecondsSince1970
        snapshot.cans[index].statusBeforeArchive = snapshot.cans[index].status
        snapshot.cans[index].status = .archived
        snapshot.cans[index].archivedAt = now
        snapshot.cans[index].updatedAt = now
        addEvent(canId, .archived, "Dose archiviert")
        persist()
    }

    func restore(canId: String) {
        guard let index = snapshot.cans.firstIndex(where: { $0.id == canId }), snapshot.cans[index].status == .archived else { return }
        snapshot.cans[index].status = snapshot.cans[index].statusBeforeArchive ?? .inStock
        snapshot.cans[index].statusBeforeArchive = nil
        snapshot.cans[index].archivedAt = nil
        snapshot.cans[index].updatedAt = Date().millisecondsSince1970
        addEvent(canId, .restored, "Dose wiederhergestellt")
        persist()
    }

    @discardableResult
    func deleteArchivedPermanently(canId: String) -> Bool {
        guard let index = snapshot.cans.firstIndex(where: { $0.id == canId && $0.status == .archived }) else { return false }
        let removed = snapshot.cans.remove(at: index)
        snapshot.events.removeAll { $0.canId == canId }
        if let filename = removed.photoFilename, !snapshot.cans.contains(where: { $0.photoFilename == filename }) {
            try? fileManager.removeItem(at: photoDirectory.appendingPathComponent(filename))
        }
        persist()
        return true
    }

    func savePhoto(_ data: Data) throws -> String {
        let filename = "\(UUID().uuidString).jpg"
        try data.write(to: photoDirectory.appendingPathComponent(filename), options: .atomic)
        return filename
    }

    func photoURL(filename: String?) -> URL? {
        guard let filename else { return nil }
        let url = photoDirectory.appendingPathComponent(filename)
        return fileManager.fileExists(atPath: url.path) ? url : nil
    }

    func canItem(_ id: String) -> CanItem? { snapshot.cans.first { $0.id == id } }

    func knownCan(barcode: String) -> CanItem? {
        snapshot.cans
            .filter { can in can.externalBarcode.map { CatalogStore.barcodesEquivalent($0, barcode) } ?? false }
            .sorted {
                if ($0.status == .archived) != ($1.status == .archived) { return $1.status == .archived }
                return $0.updatedAt > $1.updatedAt
            }
            .first
    }

    func csvExportURL() -> URL? {
        let header = "id;marke;linie;farbe;farbcode;hex;volumen_ml;fuellstand_prozent;status;preis_cent\n"
        let catalog = CatalogStore.shared
        var rowValues: [String] = []
        rowValues.reserveCapacity(snapshot.cans.count)
        for can in snapshot.cans {
            let values: [String] = [
                can.id,
                catalog.brandName(can.brandId),
                catalog.lineName(can.canLineId),
                can.colorName,
                can.colorCode ?? "",
                can.customHex ?? "",
                can.volumeMl.map { String($0) } ?? "",
                can.fillPercent.map { String($0) } ?? "",
                can.status.rawValue,
                can.purchasePriceCents.map { String($0) } ?? "",
            ]
            let escapedValues = values.map { Self.csvEscape($0) }
            rowValues.append(escapedValues.joined(separator: ";"))
        }
        let rows = rowValues.joined(separator: "\n")
        let destination = fileManager.temporaryDirectory.appendingPathComponent("canvault-history.csv")
        do {
            try (header + rows).write(to: destination, atomically: true, encoding: .utf8)
            return destination
        } catch {
            return nil
        }
    }

    private func addEvent(_ canId: String, _ type: CanEventType, _ description: String) {
        snapshot.events.insert(CanEvent(canId: canId, type: type, description: description), at: 0)
    }

    private func persist() {
        do {
            try fileManager.createDirectory(at: inventoryURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try encoder.encode(snapshot).write(to: inventoryURL, options: .atomic)
        } catch {
            assertionFailure("CANVAULT inventory could not be saved: \(error)")
        }
    }

    private static func csvEscape(_ value: String) -> String { "\"\(value.replacingOccurrences(of: "\"", with: "\"\""))\"" }
}

enum CanVaultQR {
    static func encode(_ can: CanItem) -> String {
        let payload = CanVaultQRPayload(
            brandId: can.brandId,
            canLineId: can.canLineId,
            colorName: can.colorName,
            colorCode: can.colorCode,
            customHex: can.customHex
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        guard let data = try? encoder.encode(payload) else { return "" }
        return String(data: data, encoding: .utf8) ?? ""
    }

    static func decode(_ raw: String) -> CanVaultQRPayload? {
        guard raw.count <= 4_096, let data = raw.data(using: .utf8),
              let payload = try? JSONDecoder().decode(CanVaultQRPayload.self, from: data),
              payload.app == "canvault", payload.version == 1, payload.kind == "can",
              CatalogStore.shared.brand(payload.brandId) != nil,
              CatalogStore.shared.line(payload.canLineId) != nil,
              !payload.colorName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              payload.customHex == nil || CatalogStore.normalizeHex(payload.customHex) != nil else { return nil }
        return payload
    }
}
