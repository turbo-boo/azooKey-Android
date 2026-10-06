// swift-tools-version: 6.2

import CompilerPluginSupport
import PackageDescription

let package = Package(
    name: "AzooKeyAndroidConverter",
    products: [
        .library(
            name: "AzooKeyAndroidConverter",
            targets: ["AzooKeyAndroidConverter"]
        ),
        .library(
            name: "AzooKeyAndroidJNI",
            type: .dynamic,
            targets: ["AzooKeyAndroidJNI"]
        ),
    ],
    dependencies: [
        .package(
            url: "https://github.com/azooKey/AzooKeyKanaKanjiConverter",
            revision: "d59a28e4c7ca049aef04f29a91eae9677a7753f2"
        ),
        .package(
            url: "https://github.com/swiftlang/swift-java",
            exact: "0.6.0"
        ),
    ],
    targets: [
        .target(
            name: "AzooKeyAndroidConverter",
            dependencies: [
                .product(
                    name: "KanaKanjiConverterModuleWithDefaultDictionary",
                    package: "AzooKeyKanaKanjiConverter"
                )
            ]
        ),
        .target(
            name: "AzooKeyAndroidJNI",
            dependencies: [
                .product(
                    name: "KanaKanjiConverterModule",
                    package: "AzooKeyKanaKanjiConverter"
                ),
                .product(
                    name: "SwiftJava",
                    package: "swift-java"
                ),
            ],
            plugins: [
                .plugin(
                    name: "JExtractSwiftPlugin",
                    package: "swift-java"
                )
            ]
        ),
        .testTarget(
            name: "AzooKeyAndroidConverterTests",
            dependencies: ["AzooKeyAndroidConverter"]
        )
    ]
)
