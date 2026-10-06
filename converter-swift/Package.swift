// swift-tools-version: 6.2

import PackageDescription

let package = Package(
    name: "AzooKeyAndroidConverter",
    products: [
        .library(
            name: "AzooKeyAndroidConverter",
            targets: ["AzooKeyAndroidConverter"]
        )
    ],
    dependencies: [
        .package(
            url: "https://github.com/azooKey/AzooKeyKanaKanjiConverter",
            revision: "d59a28e4c7ca049aef04f29a91eae9677a7753f2"
        )
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
        .testTarget(
            name: "AzooKeyAndroidConverterTests",
            dependencies: ["AzooKeyAndroidConverter"]
        )
    ]
)
