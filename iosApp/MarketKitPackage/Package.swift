import PackageDescription

let package = Package(
    name: "MarketKitPackage",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "MarketKitWrapper", targets: ["MarketKitWrapper"])
    ],
    targets: [
        .binaryTarget(
            name: "MarketKit",
            path: "../../shared/build/XCFrameworks/release/MarketKit.xcframework"
        ),

        .target(
            name: "MarketKitWrapper",
            dependencies: ["MarketKit"]
        )
    ]
)
