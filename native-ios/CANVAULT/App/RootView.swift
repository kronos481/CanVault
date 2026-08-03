import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case dashboard
    case inventory
    case add
    case storage
    case more

    var id: String { rawValue }
    var title: String {
        switch self {
        case .dashboard: "Übersicht"
        case .inventory: "Inventar"
        case .add: "Hinzufügen"
        case .storage: "Speicher"
        case .more: "Mehr"
        }
    }
    var icon: String {
        switch self {
        case .dashboard: "square.grid.2x2.fill"
        case .inventory: "shippingbox.fill"
        case .add: "plus.circle.fill"
        case .storage: "folder.fill"
        case .more: "ellipsis.circle.fill"
        }
    }
}

enum AppRoute: Hashable {
    case scanner
    case colorCombo
    case market
    case canDetail(String)
}

struct RootView: View {
    @EnvironmentObject private var store: InventoryStore
    @State private var selectedTab: AppTab = .dashboard
    @State private var path: [AppRoute] = []
    @State private var scanPrefill: ScanPrefill?

    var body: some View {
        NavigationStack(path: $path) {
            TabView(selection: $selectedTab) {
                DashboardView(
                    onScan: { path.append(.scanner) },
                    onAdd: { selectedTab = .add },
                    onColorCombo: { path.append(.colorCombo) },
                    onMarket: { path.append(.market) },
                    onOpenCan: { path.append(.canDetail($0)) }
                )
                .tabItem { Label(AppTab.dashboard.title, systemImage: AppTab.dashboard.icon) }
                .tag(AppTab.dashboard)

                InventoryView(
                    onScan: { path.append(.scanner) },
                    onColorCombo: { path.append(.colorCombo) },
                    onOpenCan: { path.append(.canDetail($0)) }
                )
                .tabItem { Label(AppTab.inventory.title, systemImage: AppTab.inventory.icon) }
                .tag(AppTab.inventory)

                AddCanView(
                    prefill: $scanPrefill,
                    onScan: { path.append(.scanner) },
                    onSaved: { selectedTab = .inventory }
                )
                .tabItem { Label(AppTab.add.title, systemImage: AppTab.add.icon) }
                .tag(AppTab.add)

                StorageView(onOpenCan: { path.append(.canDetail($0)) })
                    .tabItem { Label(AppTab.storage.title, systemImage: AppTab.storage.icon) }
                    .tag(AppTab.storage)

                MoreView(onMarket: { path.append(.market) })
                    .tabItem { Label(AppTab.more.title, systemImage: AppTab.more.icon) }
                    .tag(AppTab.more)
            }
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .scanner:
                    ScannerScreen { prefill in
                        scanPrefill = prefill
                        selectedTab = .add
                        if !path.isEmpty { path.removeLast() }
                    }
                case .colorCombo: ColorComboView()
                case .market: MarketView()
                case .canDetail(let id): CanDetailView(canId: id)
                }
            }
        }
        .onChange(of: selectedTab) { _ in Feedback.shared.play(.navigation) }
    }
}
