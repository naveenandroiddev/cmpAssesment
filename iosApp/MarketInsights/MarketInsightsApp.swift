import SwiftUI
import MarketKitWrapper

@main
struct MarketInsightsApp: App {

    private let component = MarketInsightsComponent()

    var body: some Scene {
        WindowGroup {
            TabView {
                MarketInsightsComposeScreen(component: component)
                    .ignoresSafeArea(.keyboard)
                    .tabItem { Label("Markets", systemImage: "chart.line.uptrend.xyaxis") }

                NativeTicketPreview(component: component)
                    .tabItem { Label("Ticket", systemImage: "list.bullet.rectangle") }
            }
        }
    }
}
