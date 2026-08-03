import Foundation

enum CanStatus: String, Codable, CaseIterable, Identifiable, Sendable {
    case inStock
    case opened
    case reserved
    case empty
    case collection
    case archived

    var id: String { rawValue }
    var label: String {
        switch self {
        case .inStock: "Ungeöffnet"
        case .opened: "Geöffnet"
        case .reserved: "Reserviert"
        case .empty: "Leer"
        case .collection: "Sammlung"
        case .archived: "Archiviert"
        }
    }
}

struct CanItem: Identifiable, Codable, Hashable, Sendable {
    var id = UUID().uuidString
    var brandId: String
    var canLineId: String
    var colorName: String
    var colorCode: String?
    var customHex: String?
    var volumeMl: Int? = 400
    var fillPercent: Int? = 100
    var status: CanStatus = .inStock
    var statusBeforeArchive: CanStatus?
    var purchasePriceCents: Int?
    var currency = "EUR"
    var photoFilename: String?
    var externalBarcode: String?
    var acquiredAt = Date().millisecondsSince1970
    var archivedAt: Int64?
    var createdAt = Date().millisecondsSince1970
    var updatedAt = Date().millisecondsSince1970
}

enum CanEventType: String, Codable, Sendable {
    case created
    case fillChanged
    case statusChanged
    case archived
    case restored
}

struct CanEvent: Identifiable, Codable, Hashable, Sendable {
    var id = UUID().uuidString
    var canId: String
    var type: CanEventType
    var description: String
    var occurredAt = Date().millisecondsSince1970
}

struct InventorySnapshot: Codable, Equatable, Sendable {
    var cans: [CanItem] = []
    var events: [CanEvent] = []
}

struct AddCanRequest: Sendable {
    var brandId: String
    var canLineId: String
    var colorName: String
    var colorCode: String?
    var customHex: String?
    var volumeMl: Int?
    var fillPercent: Int
    var quantity: Int
    var purchasePriceCents: Int?
    var photoFilename: String?
    var externalBarcode: String?
}

struct CatalogBrand: Identifiable, Hashable, Sendable {
    var id: String
    var displayName: String
    var lines: [CatalogLine]
}

struct CatalogLine: Identifiable, Hashable, Sendable {
    var id: String
    var displayName: String
    var defaultVolumeMl: Int?
    var defaultColorName: String?
    var defaultColorCode: String?
    var defaultColorHex: String?
}

struct OfficialCanColorSource: Codable, Identifiable, Hashable, Sendable {
    var id: String
    var label: String
    var url: String
    var extractedShadeCount: Int
}

struct OfficialCanColor: Codable, Identifiable, Hashable, Sendable {
    var lineId: String
    var colorName: String
    var colorCode: String?
    var productCode: String?
    var hex: String
    var sourceId: String
    var id: String { "\(lineId)|\(colorCode ?? productCode ?? colorName)|\(hex)" }
}

struct VerifiedCatalogProduct: Codable, Hashable, Sendable {
    var barcode: String
    var barcodeType: String
    var brandId: String
    var lineId: String
    var colorName: String
    var colorCode: String?
    var customHex: String?
    var volumeMl: Int?
    var regionCode: String?
    var sourceName: String
    var sourceUrl: String
    var verifiedAt: String
}

struct CatalogPriceSource: Codable, Identifiable, Hashable, Sendable {
    var retailerName: String
    var sourceUrl: String
    var priceEurCents: Int
    var observedAt: String
    var taxIncluded: Bool
    var shippingIncluded: Bool
    var id: String { sourceUrl }
}

struct CatalogPriceAnalysis: Codable, Identifiable, Hashable, Sendable {
    var lineId: String
    var volumeMl: Int
    var observations: [CatalogPriceSource]
    var id: String { lineId }
    var averageEurCents: Int {
        guard !observations.isEmpty else { return 0 }
        return Int((Double(observations.reduce(0) { $0 + $1.priceEurCents }) / Double(observations.count)).rounded())
    }
    var minimumEurCents: Int { observations.map(\.priceEurCents).min() ?? 0 }
    var maximumEurCents: Int { observations.map(\.priceEurCents).max() ?? 0 }
}

struct CatalogData: Codable, Sendable {
    var version: String
    var publishedAt: String
    var sources: [OfficialCanColorSource]
    var colors: [OfficialCanColor]
    var products: [VerifiedCatalogProduct]
    var prices: [CatalogPriceAnalysis]
}

enum CatalogMarket: String, CaseIterable, Identifiable, Sendable {
    case europe
    case unitedStates
    case unitedKingdom
    case switzerland

    var id: String { rawValue }
    var marketCode: String {
        switch self {
        case .europe: "EU"
        case .unitedStates: "US"
        case .unitedKingdom: "GB"
        case .switzerland: "CH"
        }
    }
    var label: String {
        switch self {
        case .europe: "Europa"
        case .unitedStates: "USA"
        case .unitedKingdom: "UK"
        case .switzerland: "Schweiz"
        }
    }
    var currencyCode: String {
        switch self {
        case .europe: "EUR"
        case .unitedStates: "USD"
        case .unitedKingdom: "GBP"
        case .switzerland: "CHF"
        }
    }
    var euroRate: Double {
        switch self {
        case .europe: 1
        case .unitedStates: 1.1485
        case .unitedKingdom: 0.85573
        case .switzerland: 0.9304
        }
    }
}

struct ScanPrefill: Hashable, Sendable {
    var brandId: String?
    var lineId: String?
    var colorName: String?
    var colorCode: String?
    var customHex: String?
    var volumeMl: Int?
    var purchasePriceCents: Int?
    var externalBarcode: String?
    var message: String
}

struct CanVaultQRPayload: Codable, Sendable {
    var app = "canvault"
    var version = 1
    var kind = "can"
    var brandId: String
    var canLineId: String
    var colorName: String
    var colorCode: String?
    var customHex: String?
}

enum PaintRole: String, Codable, CaseIterable, Identifiable, Sendable {
    case background
    case secondOutline
    case outline
    case fillShadow
    case fill
    case fillSecondary
    case fillTertiary
    case fillFade
    case inline

    var id: String { rawValue }
    var displayName: String {
        switch self {
        case .background: "Background"
        case .secondOutline: "Second Outline"
        case .outline: "Outline"
        case .fillShadow: "Fill-Schatten"
        case .fill: "Fill"
        case .fillSecondary: "Fill 2"
        case .fillTertiary: "Fill 3"
        case .fillFade: "Fill-Fade"
        case .inline: "Inline / Highlight"
        }
    }
}

struct InventoryPaintColor: Hashable, Sendable {
    var hex: String
    var colorName: String
    var productCode: String?
    var lineId: String
    var brandId: String
    var effectiveMl: Int
    var canCount: Int
}

struct PaletteSwatch: Identifiable, Hashable, Sendable {
    var role: PaintRole
    var hex: String
    var label: String
    var productCode: String?
    var lineLabel: String?
    var sourceLabel: String?
    var isOwned: Bool
    var effectiveMl: Int
    var canCount: Int
    var id: String { "\(role.rawValue)-\(hex)-\(label)" }
}

struct ColorHarmonyPalette: Identifiable, Hashable, Sendable {
    var id: String
    var title: String
    var rule: String
    var description: String
    var scorePercent: Int
    var minimumEdgeContrastRatio: Double
    var swatches: [PaletteSwatch]
    var isBestEffort = false
    var ownedCount: Int { swatches.filter(\.isOwned).count }
    var missingCount: Int { swatches.count - ownedCount }
}

struct ColorComboAnalysis: Sendable {
    var inventoryColors: [InventoryPaintColor]
    var totalEffectiveMl: Int
    var unresolvedCanCount: Int
    var requestedToneCount: Int
    var knowledgeBaseCandidateCount: Int64
    var evaluatedCandidateCount: Int64
    var palettes: [ColorHarmonyPalette]
}

struct StorageMonthStat: Identifiable, Sendable {
    var month: Date
    var addedCanCount: Int
    var spentCents: Int
    var id: Date { month }
}

struct StorageStats: Sendable {
    var allTimeCanCount: Int
    var currentCanCount: Int
    var archivedCanCount: Int
    var emptyCanCount: Int
    var totalSpentCents: Int
    var currentInventoryValueCents: Int
    var averagePriceCents: Int?
    var mostExpensiveCanCents: Int?
    var purchasedVolumeMl: Int
    var currentRemainingVolumeMl: Int
    var estimatedUsedVolumeMl: Int
    var distinctColorCount: Int
    var unopenedPercent: Int
    var averageUsageDays: Int?
    var topBrandId: String?
    var topLineId: String?
    var topUsedColorName: String?
    var monthlyActivity: [StorageMonthStat]
}

extension Date {
    var millisecondsSince1970: Int64 { Int64((timeIntervalSince1970 * 1_000).rounded()) }
    init(millisecondsSince1970 value: Int64) { self.init(timeIntervalSince1970: Double(value) / 1_000) }
}

extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
