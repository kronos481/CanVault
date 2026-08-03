import SwiftUI

struct StatCard: View {
    let label: String
    let value: String
    var accent = CVColor.mint

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.title3.bold()).foregroundStyle(accent).monospacedDigit().minimumScaleFactor(0.75)
        }
        .frame(maxWidth: .infinity, minHeight: 76, alignment: .leading)
        .padding(14)
        .cvCard(radius: 16)
        .accessibilityElement(children: .combine)
    }
}

struct EmptyState: View {
    let icon: String
    let title: String
    let bodyText: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon).font(.system(size: 34, weight: .medium)).foregroundStyle(CVColor.mint)
            Text(title).font(.headline)
            Text(bodyText).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(28)
        .cvCard()
    }
}

struct BrandLogo: View {
    let brandId: String

    var body: some View {
        Group {
            if let asset = ProductAssets.brandLogo[brandId] {
                Image(asset)
                    .resizable()
                    .renderingMode(.template)
                    .scaledToFit()
                    .foregroundStyle(.white)
                    .accessibilityLabel(CatalogStore.shared.brandName(brandId))
            } else {
                Text(CatalogStore.shared.brandName(brandId).uppercased())
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .minimumScaleFactor(0.65)
            }
        }
        .frame(maxWidth: 160, minHeight: 22, maxHeight: 30)
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background(Color.black.opacity(0.82))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

struct CanArtwork: View {
    let can: CanItem
    @EnvironmentObject private var store: InventoryStore

    var body: some View {
        ZStack {
            Color.secondary.opacity(0.08)
            if let photo = store.photoURL(filename: can.photoFilename) {
                AsyncImage(url: photo) { image in image.resizable().scaledToFit() } placeholder: { ProgressView() }
            } else if let asset = ProductAssets.canArtwork[can.canLineId] {
                Image(asset).resizable().scaledToFit().padding(3)
            } else {
                Image(can.id.hashValue.isMultiple(of: 2) ? "can_mint" : "can_violet").resizable().scaledToFit().padding(4)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .accessibilityLabel("Produktbild \(CatalogStore.shared.lineName(can.canLineId))")
    }
}

struct CatalogCanArtwork: View {
    let lineId: String

    var body: some View {
        ZStack {
            Color.secondary.opacity(0.08)
            Image(ProductAssets.canArtwork[lineId] ?? (lineId.hashValue.isMultiple(of: 2) ? "can_mint" : "can_violet"))
                .resizable().scaledToFit().padding(4)
        }
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .accessibilityLabel("Produktbild \(CatalogStore.shared.lineName(lineId))")
    }
}

struct CanCard: View {
    let can: CanItem
    var archivedStyle = false
    let onTap: () -> Void
    private var accent: Color { Color(hex: CatalogStore.shared.resolveHex(for: can) ?? "#58E4C2") }

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 0) {
                accent.frame(height: 8)
                HStack(spacing: 14) {
                    CanArtwork(can: can).frame(width: 76, height: 110)
                    VStack(alignment: .leading, spacing: 6) {
                        BrandLogo(brandId: can.brandId)
                        Text(CatalogStore.shared.lineName(can.canLineId)).font(.headline).foregroundStyle(.primary).lineLimit(2)
                        Text(can.colorName).font(.subheadline.weight(.medium)).foregroundStyle(.primary)
                        if let code = can.colorCode?.nilIfBlank { Text(code).font(.caption.monospaced()).foregroundStyle(.secondary) }
                        HStack(spacing: 8) {
                            Label(can.status.label, systemImage: can.status == .empty ? "drop.triangle" : "shippingbox")
                            Text("\(can.fillPercent ?? 0) %")
                        }
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(accent)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right").foregroundStyle(.tertiary).accessibilityHidden(true)
                }
                .padding(14)
            }
            .cvCard()
            .opacity(archivedStyle ? 0.56 : 1)
            .contentShape(Rectangle())
        }
        .buttonStyle(CVPressStyle())
        .accessibilityLabel("\(CatalogStore.shared.lineName(can.canLineId)), \(can.colorName), \(can.fillPercent ?? 0) Prozent, \(can.status.label)")
        .accessibilityHint("Öffnet die Details der Dose")
    }
}

struct CapsuleChip: View {
    let title: String
    let selected: Bool
    let icon: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon { Image(systemName: icon) }
                Text(title).lineLimit(1)
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(selected ? Color(hex: "#00382E") : Color.primary)
            .padding(.horizontal, 13)
            .frame(minHeight: 44)
            .background(selected ? CVColor.mint : Color.secondary.opacity(0.12))
            .clipShape(Capsule())
        }
        .buttonStyle(CVPressStyle())
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

enum ProductAssets {
    static let brandLogo: [String: String] = [
        "mtn-montana-colors": "brand_mtn", "montana-cans": "brand_montana_cans",
        "molotow-belton": "brand_molotow", "loop-colors": "brand_loop", "flame": "brand_flame",
        "kobra": "brand_kobra", "ironlak": "brand_ironlak", "nbq": "brand_nbq", "dope": "brand_dope",
        "dang": "brand_dang", "clash": "brand_clash", "beat": "brand_beat", "scribo": "brand_scribo",
        "double-a": "brand_double_a", "krink": "brand_krink",
    ]

    static let canArtwork: [String: String] = [
        "mtn-montana-colors:mtn-94": "can_mtn_94", "mtn-montana-colors:mtn-hardcore": "can_mtn_hardcore",
        "mtn-montana-colors:mtn-vice": "can_mtn_vice", "mtn-montana-colors:mtn-water-based-400": "can_mtn_water_based",
        "mtn-montana-colors:mtn-alien": "can_mtn_alien", "montana-cans:montana-black": "can_montana_black",
        "montana-cans:montana-gold": "can_montana_gold", "montana-cans:montana-white": "can_montana_white",
        "montana-cans:montana-tarblack": "can_montana_tarblack", "montana-cans:montana-blackout-tarblack": "can_montana_blackout_tarblack",
        "montana-cans:montana-ultra-wide": "can_montana_ultrawide", "molotow-belton:molotow-premium": "can_molotow_premium",
        "molotow-belton:molotow-burner": "can_molotow_burner", "molotow-belton:burner-chrome-600-ml": "can_molotow_burner",
        "molotow-belton:burner-gold-600-ml": "can_molotow_burner", "molotow-belton:burner-copper-600-ml": "can_molotow_burner",
        "molotow-belton:burner-black-600-ml": "can_molotow_burner", "molotow-belton:molotow-coversall": "can_molotow_coversall",
        "loop-colors:loop-400-ml": "can_loop_400", "loop-colors:loop-asphalt": "can_loop_asphalt",
        "flame:flame-blue": "can_flame_blue", "flame:flame-orange": "can_flame_orange",
        "kobra:kobra-hp": "can_kobra_hp", "kobra:kobra-lp": "can_kobra_lp",
        "ironlak:ironlak-400-ml": "can_ironlak_400", "ironlak:sugar-artists-acrylic": "can_ironlak_sugar",
        "nbq:nbq-fast": "can_nbq_fast", "nbq:nbq-slow": "can_nbq_slow", "dope:dope-action": "can_dope_action",
        "dope:dope-classic": "can_dope_classic", "dang:dang-prime": "can_dang_prime", "dang:dang-hi-flow": "can_dang_hi_flow",
        "clash:clash": "can_clash_400", "beat:beat": "can_beat_400", "scribo:scribo": "can_scribo_400",
        "double-a:double-a": "can_double_a_400", "krink:krink-k-750": "can_krink_750",
    ]
}
