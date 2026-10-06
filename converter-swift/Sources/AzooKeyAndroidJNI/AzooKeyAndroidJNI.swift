import Foundation
import KanaKanjiConverterModule

/*
 * JNI-facing adapter for AzooKeyKanaKanjiConverter.
 *
 * Upstream:
 *   azooKey/AzooKeyKanaKanjiConverter
 * Revision:
 *   d59a28e4c7ca049aef04f29a91eae9677a7753f2
 * Original author: Miwa / Ensan
 * License: MIT
 *
 * The dictionary is supplied as an Android filesystem path instead of using
 * Bundle.module so the same code works when packaged inside an APK/AAR.
 */
public func candidatesJSON(
    _ input: String,
    _ dictionaryPath: String
) -> String {
    guard !input.isEmpty, !dictionaryPath.isEmpty else {
        return "[]"
    }

    return AndroidConverterStorage.shared.withConverter(
        dictionaryPath: dictionaryPath
    ) { converter, workingDirectory in
        var composingText = ComposingText()
        composingText.insertAtCursorPosition(input, inputStyle: .direct)

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
                memoryDirectoryURL: workingDirectory,
                sharedContainerURL: workingDirectory,
                textReplacer: .empty,
                specialCandidateProviders: nil,
                metadata: .init(versionString: "azooKey-Android")
            )
        )

        let candidates = Array(result.mainResults.prefix(10).map(\.text))
        guard let encoded = try? JSONEncoder().encode(candidates) else {
            return "[]"
        }
        return String(decoding: encoded, as: UTF8.self)
    }
}

private final class AndroidConverterStorage: @unchecked Sendable {
    static let shared = AndroidConverterStorage()

    private let lock = NSLock()
    private var dictionaryPath: String?
    private var converter: KanaKanjiConverter?

    private let workingDirectory: URL = {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("azookey-android-converter-jni", isDirectory: true)
        try? FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )
        return directory
    }()

    func withConverter<T>(
        dictionaryPath: String,
        operation: (KanaKanjiConverter, URL) -> T
    ) -> T {
        lock.lock()
        defer { lock.unlock() }

        if converter == nil || self.dictionaryPath != dictionaryPath {
            converter = KanaKanjiConverter(
                dictionaryURL: URL(
                    fileURLWithPath: dictionaryPath,
                    isDirectory: true
                ),
                preloadDictionary: true
            )
            self.dictionaryPath = dictionaryPath
        }

        return operation(converter!, workingDirectory)
    }
}
