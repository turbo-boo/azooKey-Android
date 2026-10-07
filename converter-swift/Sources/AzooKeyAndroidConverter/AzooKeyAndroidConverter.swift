import Foundation
import KanaKanjiConverterModuleWithDefaultDictionary

/*
 * Android-facing wrapper around AzooKeyKanaKanjiConverter.
 *
 * Upstream:
 *   azooKey/AzooKeyKanaKanjiConverter
 * Revision:
 *   d59a28e4c7ca049aef04f29a91eae9677a7753f2
 * Original author: Miwa / Ensan
 * License: MIT
 */
public enum AzooKeyAndroidConverter {
    private static let storage = ConverterStorage()

    public static func predictionCandidatesJSON(_ input: String) -> String {
        guard !input.isEmpty else {
            return "[]"
        }

        var composingText = ComposingText()
        composingText.insertAtCursorPosition(input, inputStyle: .direct)

        let candidates = storage.withConverter { converter in
            let result = converter.requestCandidates(
                composingText,
                options: ConvertRequestOptions(
                    N_best: 10,
                    requireJapanesePrediction: .manualMix,
                    requireEnglishPrediction: .disabled,
                    keyboardLanguage: .ja_JP,
                    englishCandidateInRoman2KanaInput: false,
                    fullWidthRomanCandidate: false,
                    halfWidthKanaCandidate: false,
                    learningType: .nothing,
                    maxMemoryCount: 0,
                    shouldResetMemory: false,
                    memoryDirectoryURL: storage.workingDirectory,
                    sharedContainerURL: storage.workingDirectory,
                    textReplacer: .empty,
                    specialCandidateProviders: nil,
                    metadata: .init(versionString: "azooKey-Android")
                )
            )
            return Array(result.predictionResults.prefix(3).map(\.text))
        }

        guard let encoded = try? JSONEncoder().encode(candidates) else {
            return "[]"
        }
        return String(decoding: encoded, as: UTF8.self)
    }

    public static func candidatesJSON(_ input: String) -> String {
        guard !input.isEmpty else {
            return "[]"
        }

        var composingText = ComposingText()
        composingText.insertAtCursorPosition(input, inputStyle: .direct)

        let candidates = storage.withConverter { converter in
            let result = converter.requestCandidates(
                composingText,
                options: ConvertRequestOptions(
                    N_best: 10,
                    requireJapanesePrediction: .autoMix,
                    requireEnglishPrediction: .disabled,
                    keyboardLanguage: .ja_JP,
                    englishCandidateInRoman2KanaInput: false,
                    fullWidthRomanCandidate: false,
                    halfWidthKanaCandidate: false,
                    learningType: .nothing,
                    maxMemoryCount: 0,
                    shouldResetMemory: false,
                    memoryDirectoryURL: storage.workingDirectory,
                    sharedContainerURL: storage.workingDirectory,
                    textReplacer: .empty,
                    specialCandidateProviders: nil,
                    metadata: .init(versionString: "azooKey-Android")
                )
            )
            return Array(result.mainResults.prefix(10).map(\.text))
        }

        guard let encoded = try? JSONEncoder().encode(candidates) else {
            return "[]"
        }
        return String(decoding: encoded, as: UTF8.self)
    }
}

private final class ConverterStorage: @unchecked Sendable {
    let workingDirectory: URL

    private let converter: KanaKanjiConverter
    private let lock = NSLock()

    init() {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("azookey-android-converter", isDirectory: true)
        try? FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )
        self.workingDirectory = directory
        self.converter = KanaKanjiConverter.withDefaultDictionary(
            preloadDictionary: true
        )
    }

    func withConverter<T>(
        _ operation: (KanaKanjiConverter) -> T
    ) -> T {
        lock.lock()
        defer { lock.unlock() }
        return operation(converter)
    }
}
