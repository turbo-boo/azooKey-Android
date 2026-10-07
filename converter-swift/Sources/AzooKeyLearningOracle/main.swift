import AzooKeyAndroidConverter
import Foundation

guard CommandLine.arguments.count >= 3 else {
    fputs("usage: AzooKeyLearningOracle <input> <candidate>\n", stderr)
    exit(64)
}

let input = CommandLine.arguments[1]
let candidate = CommandLine.arguments[2]
let entry = [["reading": input, "word": candidate]]
let data = try JSONSerialization.data(withJSONObject: entry)
let dictionaryJSON = String(decoding: data, as: UTF8.self)

AzooKeyAndroidConverter.resetLearningMemory()
guard AzooKeyAndroidConverter.replaceUserDictionaryJSON(dictionaryJSON) else {
    print("AZOOKEY_LEARNED=false")
    exit(1)
}

let learned = AzooKeyAndroidConverter.learnCandidate(
    input: input,
    candidateText: candidate
)
print("AZOOKEY_LEARNED=\(learned)")
print("AZOOKEY_LEARNING_MEMORY_PATH=\(AzooKeyAndroidConverter.learningMemoryDirectoryPath)")
