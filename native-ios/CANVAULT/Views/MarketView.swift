import SwiftUI

private struct MarketItem: Identifiable {
    var brand: CatalogBrand
    var line: CatalogLine
    var id: String { line.id }
}

struct MarketView: View {
    @State private var market: CatalogMarket = .europe
    @State private var query = ""
    @State private var selectedBrandId: String?
    @State private var selectedPrice: CatalogPriceAnalysis?
    private let catalog = CatalogStore.shared

    private var allItems: [MarketItem] { catalog.brands.flatMap { brand in brand.lines.map { MarketItem(brand: brand, line: $0) } } }
    private var filtered: [MarketItem] {
        allItems.filter { item in
            (selectedBrandId == nil || item.brand.id == selectedBrandId) &&
            (query.isEmpty || "\(item.brand.displayName) \(item.line.displayName)".localizedCaseInsensitiveContains(query))
        }
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                statusCard
                searchField
                brandFilters
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(filtered.count) Dosenlinien").font(.title2.bold())
                    Text("\(filtered.filter { catalog.price(for: $0.line.id) != nil }.count) mit Preisprüfung · EUR-Basis, ohne Versand").font(.caption).foregroundStyle(.secondary)
                }
                ForEach(filtered) { item in marketCard(item) }
                Text("Preisbasis: öffentlich sichtbare EU-Einzelpreise inklusive ausgewiesener MwSt., ohne Versand und Mengenrabatte. USD, GBP und CHF werden mit den Referenzkursen vom 31.07.2026 umgerechnet. Händlerpreise können sich jederzeit ändern.")
                    .font(.caption).foregroundStyle(.secondary).padding(.vertical, 10)
            }
            .padding(16).padding(.bottom, 24)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Can-Markt")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    ForEach(CatalogMarket.allCases) { option in
                        Button { market = option; Feedback.shared.play(.standard) } label: {
                            Label("\(option.label) · \(option.currencyCode)", systemImage: market == option ? "checkmark.circle.fill" : "circle")
                        }
                    }
                } label: {
                    HStack(spacing: 6) { MarketFlag(market: market).frame(width: 24, height: 16); Text(market.currencyCode).font(.headline); Image(systemName: "chevron.down").font(.caption) }
                        .frame(minHeight: 44)
                }
                .accessibilityLabel("Markt: \(market.label), \(market.currencyCode)")
            }
        }
        .sheet(item: $selectedPrice) { PriceSourcesSheet(price: $0, market: market) }
    }

    private var statusCard: some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.seal.fill").font(.title2).foregroundStyle(CVColor.mint)
            VStack(alignment: .leading, spacing: 3) {
                Text("Verifizierter Offline-Kernkatalog").font(.headline)
                Text("\(catalog.products.count) geprüfte GTIN-Zuordnungen · Version \(catalog.data.version)").font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(16).background(CVColor.mint.opacity(0.12)).clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
            TextField("Dose oder Marke suchen", text: $query).submitLabel(.search)
            if !query.isEmpty { Button { query = "" } label: { Image(systemName: "xmark.circle.fill").frame(width: 44, height: 44) }.foregroundStyle(.secondary).accessibilityLabel("Suche leeren") }
        }
        .padding(.leading, 14).frame(minHeight: 52).background(Color.secondary.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 15))
    }

    private var brandFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                CapsuleChip(title: "Alle", selected: selectedBrandId == nil, icon: nil) { selectedBrandId = nil }
                ForEach(catalog.brands) { brand in
                    CapsuleChip(title: brand.displayName, selected: selectedBrandId == brand.id, icon: nil) {
                        selectedBrandId = selectedBrandId == brand.id ? nil : brand.id
                    }
                }
            }
        }
    }

    private func marketCard(_ item: MarketItem) -> some View {
        let price = catalog.price(for: item.line.id)
        return HStack(spacing: 14) {
            CatalogCanArtwork(lineId: item.line.id).frame(width: 82, height: 118)
            VStack(alignment: .leading, spacing: 5) {
                BrandLogo(brandId: item.brand.id)
                Text(item.line.displayName).font(.headline).lineLimit(2)
                Text("\(catalog.displayVolume(lineId: item.line.id)) ml").font(.caption).foregroundStyle(.secondary)
                if let price {
                    Text("\(price.observations.count >= 2 ? "Ø" : "Richtwert") \(formatPrice(Int((Double(price.averageEurCents) * market.euroRate).rounded()), market: market))")
                        .font(.title3.bold()).foregroundStyle(CVColor.mint)
                    Text(price.observations.count >= 2
                         ? "\(formatPrice(Int((Double(price.minimumEurCents) * market.euroRate).rounded()), market: market))–\(formatPrice(Int((Double(price.maximumEurCents) * market.euroRate).rounded()), market: market)) · \(price.observations.count) Quellen"
                         : "1 Quelle · EU-Preis umgerechnet")
                        .font(.caption).foregroundStyle(.secondary)
                    Button { selectedPrice = price; Feedback.shared.play(.standard) } label: { Label("Quellen ansehen", systemImage: "doc.text.magnifyingglass").frame(minHeight: 44) }
                        .buttonStyle(.plain).foregroundStyle(CVColor.mint)
                } else {
                    Text("Noch keine belastbaren Preisdaten").font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(14).cvCard()
    }

    private func formatPrice(_ cents: Int, market: CatalogMarket) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = market.currencyCode
        formatter.locale = switch market {
        case .europe: Locale(identifier: "de_DE")
        case .unitedStates: Locale(identifier: "en_US")
        case .unitedKingdom: Locale(identifier: "en_GB")
        case .switzerland: Locale(identifier: "de_CH")
        }
        return formatter.string(from: NSNumber(value: Double(cents) / 100)) ?? "–"
    }
}

private struct PriceSourcesSheet: View {
    @Environment(\.dismiss) private var dismiss
    let price: CatalogPriceAnalysis
    let market: CatalogMarket

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("Geprüft am \(price.observations.map(\.observedAt).max() ?? "–") · inklusive ausgewiesener MwSt. · ohne Versand")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Section("Quellen") {
                    ForEach(price.observations) { source in
                        Link(destination: URL(string: source.sourceUrl)!) {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(source.retailerName).foregroundStyle(.primary)
                                    Text(source.sourceUrl).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                                }
                                Spacer()
                                Text(formatEuro(source.priceEurCents)).font(.headline).foregroundStyle(CVColor.mint)
                                Image(systemName: "arrow.up.right.square").foregroundStyle(.secondary)
                            }
                            .frame(minHeight: 48)
                        }
                    }
                }
            }
            .navigationTitle("Preisquellen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Fertig") { dismiss() } } }
        }
    }

    private func formatEuro(_ cents: Int) -> String {
        let formatter = NumberFormatter(); formatter.numberStyle = .currency; formatter.currencyCode = "EUR"; formatter.locale = Locale(identifier: "de_DE")
        return formatter.string(from: NSNumber(value: Double(cents) / 100)) ?? "–"
    }
}

private struct MarketFlag: View {
    let market: CatalogMarket

    var body: some View {
        Text(flag)
            .font(.system(size: 22))
            .frame(width: 30, height: 22)
            .clipShape(RoundedRectangle(cornerRadius: 3, style: .continuous))
        .accessibilityLabel("Flagge \(market.label)")
    }

    private var flag: String {
        switch market {
        case .europe: "🇪🇺"
        case .unitedStates: "🇺🇸"
        case .unitedKingdom: "🇬🇧"
        case .switzerland: "🇨🇭"
        }
    }
}
