import SwiftUI

enum CVColor {
    static let background = Color(hex: "#090B0E")
    static let raisedBackground = Color(hex: "#0D1014")
    static let surface = Color(hex: "#14181E")
    static let raisedSurface = Color(hex: "#1A2028")
    static let mint = Color(hex: "#58E4C2")
    static let mintDark = Color(hex: "#0B6B5A")
    static let text = Color(hex: "#F5F7FA")
    static let muted = Color(hex: "#AEB8C4")
    static let danger = Color(hex: "#FF7B7B")
    static let warning = Color(hex: "#F5BF60")
}

extension Color {
    init(hex: String) {
        let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var value: UInt64 = 0
        Scanner(string: clean).scanHexInt64(&value)
        let red, green, blue, alpha: UInt64
        switch clean.count {
        case 8:
            red = value >> 24
            green = value >> 16 & 0xFF
            blue = value >> 8 & 0xFF
            alpha = value & 0xFF
        default:
            red = value >> 16
            green = value >> 8 & 0xFF
            blue = value & 0xFF
            alpha = 0xFF
        }
        self.init(.sRGB, red: Double(red) / 255, green: Double(green) / 255, blue: Double(blue) / 255, opacity: Double(alpha) / 255)
    }
}

struct CVCardModifier: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme
    var radius: CGFloat = 20

    func body(content: Content) -> some View {
        content
            .background(colorScheme == .dark ? CVColor.surface : Color.white)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .stroke(colorScheme == .dark ? Color.white.opacity(0.07) : Color.black.opacity(0.07), lineWidth: 1)
            }
    }
}

extension View {
    func cvCard(radius: CGFloat = 20) -> some View { modifier(CVCardModifier(radius: radius)) }
}

struct CVPressStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.78 : 1)
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.975 : 1)
            .animation(reduceMotion ? nil : .spring(response: 0.22, dampingFraction: 0.78), value: configuration.isPressed)
    }
}

struct CVPrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(Color(hex: "#00382E"))
            .frame(maxWidth: .infinity, minHeight: 52)
            .padding(.horizontal, 16)
            .background(CVColor.mint.opacity(isEnabled ? (configuration.isPressed ? 0.76 : 1) : 0.36))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.985 : 1)
            .animation(reduceMotion ? nil : .spring(response: 0.22, dampingFraction: 0.8), value: configuration.isPressed)
    }
}

struct CVSectionTitle: View {
    let title: String
    var subtitle: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.title3.bold())
            if let subtitle { Text(subtitle).font(.subheadline).foregroundStyle(.secondary) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        layout(proposal: proposal, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = layout(proposal: proposal, subviews: subviews)
        for (index, point) in result.points.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + point.x, y: bounds.minY + point.y), proposal: .unspecified)
        }
    }

    private func layout(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, points: [CGPoint]) {
        let width = proposal.width ?? 320
        var points: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            points.append(CGPoint(x: x, y: y))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return (CGSize(width: width, height: y + rowHeight), points)
    }
}
