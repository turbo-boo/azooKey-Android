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

public func predictionCandidatesJSON(
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
                requireJapanesePrediction: .manualMix,
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

        let candidates = Array(result.predictionResults.prefix(3).map(\.text))
        guard let encoded = try? JSONEncoder().encode(candidates) else {
            return "[]"
        }
        return String(decoding: encoded, as: UTF8.self)
    }
}


private struct PredictionPathElementWire: Encodable {
    let word: String
    let ruby: String
    let lcid: Int
    let rcid: Int
    let mid: Int
    let value: Float
}

private struct PredictionShadowWire: Encodable {
    let predictions: [String]
    let path: [PredictionPathElementWire]
}

private func dictionaryReading(_ input: String) -> String {
    let units = input.utf16.map { unit -> UInt16 in
        if 0x3041 <= unit && unit <= 0x3096 {
            return unit + 0x60
        }
        return unit
    }
    return String(decoding: units, as: UTF16.self)
}

public func predictionShadowJSON(
    _ input: String,
    _ dictionaryPath: String
) -> String {
    guard !input.isEmpty, !dictionaryPath.isEmpty else {
        return #"{"predictions":[],"path":[]}"#
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
                requireJapanesePrediction: .manualMix,
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

        let predictions = Array(result.predictionResults.prefix(3).map(\.text))
        let targetRuby = dictionaryReading(input)
        let pathCandidate = result.mainResults.first {
            !$0.data.isEmpty && $0.data.map(\.ruby).joined() == targetRuby
        } ?? result.mainResults.first {
            !$0.data.isEmpty
        }
        let path = pathCandidate?.data.map {
            PredictionPathElementWire(
                word: $0.word,
                ruby: $0.ruby,
                lcid: $0.lcid,
                rcid: $0.rcid,
                mid: $0.mid,
                value: Float($0.value())
            )
        } ?? []

        let wire = PredictionShadowWire(
            predictions: predictions,
            path: path
        )
        guard let encoded = try? JSONEncoder().encode(wire) else {
            return #"{"predictions":[],"path":[]}"#
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
