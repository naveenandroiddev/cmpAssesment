import SwiftUI
import UIKit
import MarketKitWrapper

struct MarketInsightsComposeScreen: UIViewControllerRepresentable {

    let component: MarketInsightsComponent

    func makeUIViewController(context: Context) -> UIViewController {
        IosEntryPointsKt.MarketInsightsViewController(component: component)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
