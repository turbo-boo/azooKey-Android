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

    public static func replaceUserDictionaryJSON(_ json: String) -> Bool {
        guard let data = json.data(using: .utf8),
              let entries = try? JSONDecoder().decode([UserDictionaryEntryWire].self, from: data)
        else {
            return false
        }

        storage.withConverter { converter in
            converter.importDynamicUserDictionary(
                entries.compactMap { entry in
                    let reading = dictionaryReading(entry.reading.trimmingCharacters(in: .whitespacesAndNewlines))
                    let word = entry.word.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !reading.isEmpty, !word.isEmpty else {
                        return nil
                    }
                    return DicdataElement(
                        word: word,
                        ruby: reading,
                        cid: CIDData.固有名詞.cid,
                        mid: MIDData.一般.mid,
                        value: -10
                    )
                }
            )
        }
        return true
    }

    public static func learnCandidate(
        input: String,
        candidateText: String
    ) -> Bool {
        guard !input.isEmpty, !candidateText.isEmpty else {
            return false
        }

        var composingText = ComposingText()
        composingText.insertAtCursorPosition(input, inputStyle: .direct)

        return storage.withConverter { converter in
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
                learningType: .inputAndOutput,
                maxMemoryCount: 65536,
                shouldResetMemory: false,
                memoryDirectoryURL: storage.workingDirectory,
                sharedContainerURL: storage.workingDirectory,
                textReplacer: .empty,
                specialCandidateProviders: nil,
                metadata: .init(versionString: "azooKey-Android")
            )
            )
            guard let candidate = (result.mainResults + result.predictionResults)
                .first(where: { $0.text == candidateText })
            else {
                return false
            }

            converter.setCompletedData(candidate)
            converter.updateLearningData(candidate)
            converter.commitUpdateLearningData()
            converter.stopComposition()
            return true
        }
    }

    public static var learningMemoryDirectoryPath: String {
        storage.workingDirectory.path
    }

    public static func resetLearningMemory() {
        storage.withConverter { converter in
            converter.resetMemory()
            converter.stopComposition()
        }
    }

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

    public static func conversionBridgeJSON(_ input: String) -> String {
        guard !input.isEmpty else {
            return #"{"candidates":[],"path":[]}"#
        }

        var composingText = ComposingText()
        composingText.insertAtCursorPosition(input, inputStyle: .direct)

        let bridge = storage.withConverter { converter in
            let result = converter.requestCandidates(
                composingText,
                options: ConvertRequestOptions(
                    N_best: 10,
                    requireJapanesePrediction: .disabled,
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

            let targetRuby = dictionaryReading(input)
            let pathCandidate = result.mainResults.first {
                !$0.data.isEmpty && $0.data.map(\.ruby).joined() == targetRuby
            } ?? result.mainResults.first {
                !$0.data.isEmpty
            }
            let candidates = Array(result.mainResults.prefix(10)).map {
                ScoredCandidateWire(
                    text: $0.text,
                    value: Float($0.value),
                    exactRuby: !$0.data.isEmpty && $0.data.map(\.ruby).joined() == targetRuby
                )
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

            return ConversionBridgeWire(
                candidates: candidates,
                path: path
            )
        }

        guard let encoded = try? JSONEncoder().encode(bridge) else {
            return #"{"candidates":[],"path":[]}"#
        }
        return String(decoding: encoded, as: UTF8.self)
    }

    public static func sequentialPredictionParityJSON(_ inputs: [String]) -> String {
        guard !inputs.isEmpty else {
            return "[]"
        }

        let autoMixStorage = ConverterStorage()
        let bridgeStorage = ConverterStorage()

        let rows = inputs.map { input -> SequentialPredictionParityWire in
            var autoText = ComposingText()
            autoText.insertAtCursorPosition(input, inputStyle: .direct)
            let autoMix = autoMixStorage.withConverter { converter in
                let result = converter.requestCandidates(
                    autoText,
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
                        memoryDirectoryURL: autoMixStorage.workingDirectory,
                        sharedContainerURL: autoMixStorage.workingDirectory,
                        textReplacer: .empty,
                        specialCandidateProviders: nil,
                        metadata: .init(versionString: "azooKey-Android")
                    )
                )
                return Array(result.mainResults.prefix(5).map(\.text))
            }

            var bridgeText = ComposingText()
            bridgeText.insertAtCursorPosition(input, inputStyle: .direct)
            let bridge = bridgeStorage.withConverter { converter in
                let result = converter.requestCandidates(
                    bridgeText,
                    options: ConvertRequestOptions(
                        N_best: 10,
                        requireJapanesePrediction: .disabled,
                        requireEnglishPrediction: .disabled,
                        keyboardLanguage: .ja_JP,
                        englishCandidateInRoman2KanaInput: false,
                        fullWidthRomanCandidate: false,
                        halfWidthKanaCandidate: false,
                        learningType: .nothing,
                        maxMemoryCount: 0,
                        shouldResetMemory: false,
                        memoryDirectoryURL: bridgeStorage.workingDirectory,
                        sharedContainerURL: bridgeStorage.workingDirectory,
                        textReplacer: .empty,
                        specialCandidateProviders: nil,
                        metadata: .init(versionString: "azooKey-Android")
                    )
                )

                let targetRuby = dictionaryReading(input)
                let pathCandidate = result.mainResults.first {
                    !$0.data.isEmpty && $0.data.map(\.ruby).joined() == targetRuby
                } ?? result.mainResults.first {
                    !$0.data.isEmpty
                }
                let candidates = Array(result.mainResults.prefix(10)).map {
                    ScoredCandidateWire(
                        text: $0.text,
                        value: Float($0.value),
                        exactRuby: !$0.data.isEmpty && $0.data.map(\.ruby).joined() == targetRuby
                    )
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
                return ConversionBridgeWire(
                    candidates: candidates,
                    path: path
                )
            }

            return SequentialPredictionParityWire(
                input: input,
                autoMix: autoMix,
                bridge: bridge
            )
        }

        guard let encoded = try? JSONEncoder().encode(rows) else {
            return "[]"
        }
        return String(decoding: encoded, as: UTF8.self)
    }

    public static func predictionDiagnosticsJSON(_ input: String) -> String {
        guard !input.isEmpty else {
            return #"{"predictions":[],"path":[]}"#
        }

        var composingText = ComposingText()
        composingText.insertAtCursorPosition(input, inputStyle: .direct)

        let diagnostics = storage.withConverter { converter in
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

            return PredictionDiagnosticsWire(
                predictions: predictions,
                path: path
            )
        }

        guard let encoded = try? JSONEncoder().encode(diagnostics) else {
            return #"{"predictions":[],"path":[]}"#
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


private struct SequentialPredictionParityWire: Encodable {
    let input: String
    let autoMix: [String]
    let bridge: ConversionBridgeWire
}

private struct UserDictionaryEntryWire: Decodable {
    let reading: String
    let word: String
}

private struct ScoredCandidateWire: Encodable {
    let text: String
    let value: Float
    let exactRuby: Bool
}

private struct ConversionBridgeWire: Encodable {
    let candidates: [ScoredCandidateWire]
    let path: [PredictionPathElementWire]
}

private struct PredictionPathElementWire: Encodable {
    let word: String
    let ruby: String
    let lcid: Int
    let rcid: Int
    let mid: Int
    let value: Float
}

private struct PredictionDiagnosticsWire: Encodable {
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
