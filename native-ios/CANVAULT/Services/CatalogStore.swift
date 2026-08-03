import Foundation

final class CatalogStore: @unchecked Sendable {
    static let shared = CatalogStore()

    let data: CatalogData
    let brands: [CatalogBrand]
    private let colorsByLine: [String: [OfficialCanColor]]
    private let sourcesById: [String: OfficialCanColorSource]

    var colors: [OfficialCanColor] { data.colors }
    var products: [VerifiedCatalogProduct] { data.products }
    var prices: [CatalogPriceAnalysis] { data.prices }

    private init(bundle: Bundle = .main) {
        if let url = bundle.url(forResource: "CatalogData", withExtension: "json"),
           let raw = try? Data(contentsOf: url),
           let decoded = try? JSONDecoder().decode(CatalogData.self, from: raw) {
            data = decoded
        } else {
            data = CatalogData(version: "unavailable", publishedAt: "", sources: [], colors: [], products: [], prices: [])
        }
        brands = Self.makeBrands()
        colorsByLine = Dictionary(grouping: data.colors, by: \.lineId)
        sourcesById = Dictionary(uniqueKeysWithValues: data.sources.map { ($0.id, $0) })
    }

    func brand(_ id: String) -> CatalogBrand? { brands.first { $0.id == id } }
    func line(_ id: String) -> CatalogLine? { brands.lazy.flatMap(\.lines).first { $0.id == id } }
    func brandName(_ id: String) -> String { brand(id)?.displayName ?? id }
    func lineName(_ id: String) -> String { line(id)?.displayName ?? id.split(separator: ":").last.map(String.init) ?? id }
    func colors(for lineId: String) -> [OfficialCanColor] { colorsByLine[lineId] ?? [] }
    func source(for color: OfficialCanColor) -> OfficialCanColorSource? { sourcesById[color.sourceId] }

    func findColor(lineId: String?, name: String?, code: String?) -> OfficialCanColor? {
        guard let lineId else { return nil }
        let candidates = colors(for: lineId)
        let normalizedCode = Self.normalizeCode(code)
        if !normalizedCode.isEmpty,
           let result = candidates.first(where: {
               Self.normalizeCode($0.colorCode) == normalizedCode || Self.normalizeCode($0.productCode) == normalizedCode
           }) {
            return result
        }
        let normalizedName = Self.normalizeName(name)
        guard !normalizedName.isEmpty else { return nil }
        return candidates.first { color in
            let aliases = [color.colorName, color.colorCode, color.productCode].compactMap { $0 }.map(Self.normalizeName)
            return aliases.contains(normalizedName)
        }
    }

    func searchColors(lineId: String, query: String, limit: Int = 8) -> [OfficialCanColor] {
        let normalized = Self.normalizeName(query)
        let values = colors(for: lineId)
        guard !normalized.isEmpty else { return Array(values.prefix(limit)) }
        return values
            .filter { color in
                [color.colorName, color.colorCode, color.productCode]
                    .compactMap { $0 }
                    .map(Self.normalizeName)
                    .contains { $0.contains(normalized) }
            }
            .prefix(limit)
            .map { $0 }
    }

    func resolveHex(for can: CanItem) -> String? {
        findColor(lineId: can.canLineId, name: can.colorName, code: can.colorCode)?.hex
            ?? Self.normalizeHex(can.customHex)
            ?? Self.normalizeHex(can.colorCode)
            ?? line(can.canLineId)?.defaultColorHex.flatMap(Self.normalizeHex)
    }

    func verifiedProduct(barcode: String) -> VerifiedCatalogProduct? {
        products.first { Self.barcodesEquivalent($0.barcode, barcode) }
    }

    func price(for lineId: String) -> CatalogPriceAnalysis? { prices.first { $0.lineId == lineId } }

    func displayVolume(lineId: String) -> Int {
        price(for: lineId)?.volumeMl ?? line(lineId)?.defaultVolumeMl ?? {
            switch lineId {
            case "mtn-montana-colors:mtn-mega": 600
            case "mtn-montana-colors:mtn-alien": 250
            case "montana-cans:montana-tarblack": 500
            case "montana-cans:montana-ultra-wide": 750
            case "molotow-belton:molotow-burner", "molotow-belton:molotow-coversall": 600
            case "loop-colors:loop-asphalt", "dope:dope-action": 600
            case "krink:krink-k-750": 750
            default: 400
            }
        }()
    }

    func suggestedPrice(lineId: String, volumeMl: Int) -> Int? {
        guard volumeMl > 0 else { return nil }
        if let exact = prices.first(where: { $0.lineId == lineId && $0.volumeMl == volumeMl }) {
            return exact.averageEurCents
        }
        if let sameLine = prices.filter({ $0.lineId == lineId }).min(by: {
            abs($0.volumeMl - volumeMl) < abs($1.volumeMl - volumeMl)
        }) {
            return Int((Double(sameLine.averageEurCents) * Double(volumeMl) / Double(sameLine.volumeMl)).rounded())
        }
        let brandId = lineId.split(separator: ":").first.map(String.init) ?? ""
        let sameBrand = prices.filter { $0.lineId.hasPrefix("\(brandId):") && $0.volumeMl == volumeMl }
        if !sameBrand.isEmpty { return averageLinePrice(sameBrand) }
        let sameFormat = prices.filter { $0.volumeMl == volumeMl }
        if !sameFormat.isEmpty { return averageLinePrice(sameFormat) }
        guard !prices.isEmpty else { return nil }
        let centsPerMl = prices.map { Double($0.averageEurCents) / Double($0.volumeMl) }.reduce(0, +) / Double(prices.count)
        return Int((centsPerMl * Double(volumeMl)).rounded())
    }

    static func normalizeHex(_ value: String?) -> String? {
        guard let value else { return nil }
        let result = value.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard result.range(of: "^#[0-9A-F]{6}$", options: .regularExpression) != nil else { return nil }
        return result
    }

    static func barcodesEquivalent(_ first: String, _ second: String) -> Bool {
        !barcodeAliases(first).isDisjoint(with: barcodeAliases(second))
    }

    private static func barcodeAliases(_ raw: String) -> Set<String> {
        let value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard value.allSatisfy(\.isNumber) else { return [value] }
        var aliases: Set<String> = [value]
        if value.count == 12 { aliases.insert("0\(value)") }
        if value.count == 13 && value.first == "0" { aliases.insert(String(value.dropFirst())) }
        return aliases
    }

    private func averageLinePrice(_ values: [CatalogPriceAnalysis]) -> Int {
        Int((Double(values.reduce(0) { $0 + $1.averageEurCents }) / Double(values.count)).rounded())
    }

    private static func normalizeName(_ value: String?) -> String {
        value.orEmpty
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9]+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func normalizeCode(_ value: String?) -> String {
        value.orEmpty.lowercased().replacingOccurrences(of: "[^a-z0-9#]+", with: "", options: .regularExpression)
    }

    private static func line(_ brand: String, _ name: String, volume: Int? = nil, color: String? = nil, code: String? = nil, hex: String? = nil) -> CatalogLine {
        let slug = name.lowercased().replacingOccurrences(of: "[^a-z0-9]+", with: "-", options: .regularExpression).trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        return CatalogLine(id: "\(brand):\(slug)", displayName: name, defaultVolumeMl: volume, defaultColorName: color, defaultColorCode: code, defaultColorHex: hex)
    }

    private static func brand(_ id: String, _ name: String, _ lines: [String]) -> CatalogBrand {
        CatalogBrand(id: id, displayName: name, lines: lines.map { line(id, $0) })
    }

    private static func makeBrands() -> [CatalogBrand] {
        [
            brand("mtn-montana-colors", "MTN / Montana Colors", ["MTN 94", "MTN Hardcore", "MTN Vice", "MTN Water Based 400", "MTN Mega", "MTN Alien"]),
            brand("montana-cans", "Montana Cans", ["Montana Black", "Montana Gold", "Montana White", "Montana Tarblack", "Montana Blackout Tarblack", "Montana Ultra Wide"]),
            CatalogBrand(id: "molotow-belton", displayName: "Molotow / Belton", lines: [
                line("molotow-belton", "Molotow Premium"),
                line("molotow-belton", "Molotow Burner"),
                CatalogLine(id: "molotow-belton:burner-chrome-600-ml", displayName: "Burner Chrome 600 ml", defaultVolumeMl: 600, defaultColorName: "Metallic Chrome", defaultColorCode: "940397E", defaultColorHex: "#B7BDC3"),
                CatalogLine(id: "molotow-belton:burner-gold-600-ml", displayName: "Burner Gold 600 ml", defaultVolumeMl: 600, defaultColorName: "Metallic Gold", defaultColorCode: "940499", defaultColorHex: "#C29A45"),
                CatalogLine(id: "molotow-belton:burner-copper-600-ml", displayName: "Burner Copper 600 ml", defaultVolumeMl: 600, defaultColorName: "Metallic Copper", defaultColorCode: "940500", defaultColorHex: "#B66A45"),
                CatalogLine(id: "molotow-belton:burner-black-600-ml", displayName: "Burner Black 600 ml", defaultVolumeMl: 600, defaultColorName: "Black", defaultColorCode: "940398", defaultColorHex: "#090909"),
                line("molotow-belton", "Molotow CoversAll"),
            ]),
            brand("loop-colors", "Loop Colors", ["Loop 400 ml", "Loop Asphalt"]),
            brand("flame", "Flame", ["Flame Blue", "Flame Orange"]),
            brand("kobra", "Kobra", ["Kobra HP", "Kobra LP"]),
            brand("ironlak", "Ironlak", ["Ironlak 400 ml", "Sugar Artists Acrylic"]),
            brand("nbq", "NBQ", ["NBQ Fast", "NBQ Slow"]),
            brand("dope", "Dope", ["Dope Action", "Dope Classic"]),
            brand("dang", "Dang", ["Dang Prime", "Dang Hi-Flow"]),
            brand("clash", "Clash", ["Clash"]),
            brand("beat", "Beat", ["Beat"]),
            brand("scribo", "Scribo", ["Scribo"]),
            brand("double-a", "Double A", ["Double A"]),
            brand("krink", "Krink", ["Krink K-750"]),
        ]
    }
}

private extension Optional where Wrapped == String {
    var orEmpty: String { self ?? "" }
}
