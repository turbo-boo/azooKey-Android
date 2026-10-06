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
