import SwiftUI

@main
struct CANVAULTApp: App {
    @StateObject private var inventory = InventoryStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(inventory)
                .tint(CVColor.mint)
        }
    }
}
