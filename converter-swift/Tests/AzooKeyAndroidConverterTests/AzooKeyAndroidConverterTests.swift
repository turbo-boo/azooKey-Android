import Foundation
import Testing
@testable import AzooKeyAndroidConverter

@Test
func emptyInputProducesEmptyCandidateArray() throws {
    let json = AzooKeyAndroidConverter.candidatesJSON("")
    let candidates = try #require(
        JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String]
    )

    #expect(candidates.isEmpty)
}

@Test
func defaultDictionaryConvertsUpstreamReadmeExample() throws {
    let json = AzooKeyAndroidConverter.candidatesJSON(
        "あずーきーはしんじだいのきーぼーどあぷりです"
    )
    let candidates = try #require(
        JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String]
    )

    #expect(candidates.contains("azooKeyは新時代のキーボードアプリです"))
}


@Test
func dynamicUserDictionaryAffectsConversion() throws {
    let dictionaryJSON = #"[{"reading":"ゆーざーじしょしけんご","word":"ユーザー辞書試験語"}]"#
    #expect(AzooKeyAndroidConverter.replaceUserDictionaryJSON(dictionaryJSON))
    defer {
        _ = AzooKeyAndroidConverter.replaceUserDictionaryJSON("[]")
    }

    let json = AzooKeyAndroidConverter.candidatesJSON("ゆーざーじしょしけんご")
    let candidates = try #require(
        JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String]
    )

    #expect(candidates.contains("ユーザー辞書試験語"))
}


@Test
func selectedCandidateCanBeLearnedAndUnknownCandidateIsRejected() {
    AzooKeyAndroidConverter.resetLearningMemory()
    defer {
        AzooKeyAndroidConverter.resetLearningMemory()
    }

    #expect(
        AzooKeyAndroidConverter.learnCandidate(
            input: "あさって",
            candidateText: "明後日"
        )
    )
    #expect(
        !AzooKeyAndroidConverter.learnCandidate(
            input: "あさって",
            candidateText: "存在しない候補"
        )
    )
}
