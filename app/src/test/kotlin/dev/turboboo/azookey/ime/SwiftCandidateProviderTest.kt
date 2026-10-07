package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SwiftCandidateProviderTest {
    @Test
    fun parsesCandidateArrayFromSwiftBridge() {
        assertEquals(
            listOf("仮名", "かな", "カナ"),
            parseCandidateJson("""["仮名","かな","カナ"]"""),
        )
    }

    @Test
    fun ignoresBlankAndDuplicateCandidates() {
        assertEquals(
            listOf("仮名", "かな"),
            parseCandidateJson("""["仮名","","仮名","かな"]"""),
        )
    }

    @Test
    fun parsesPredictionDisabledConversionBridge() {
        val parsed = parseConversionBridgeJson(
            """{"candidates":[{"text":"仮名","value":-1.5,"exactRuby":true},{"text":"かな","value":-2.0,"exactRuby":false}],"path":[{"word":"仮名","ruby":"カナ","lcid":1,"rcid":2,"mid":3,"value":-1.5}]}""",
        )

        assertEquals(
            listOf(
                ScoredCandidate("仮名", -1.5f, exactRuby = true),
                ScoredCandidate("かな", -2.0f),
            ),
            parsed.candidates,
        )
        assertEquals(
            """[{"word":"仮名","ruby":"カナ","lcid":1,"rcid":2,"mid":3,"value":-1.5}]""",
            parsed.pathJson,
        )
    }

    @Test
    fun parsesScoredRustPredictionArray() {
        assertEquals(
            listOf(
                ScoredCandidate("仮名", -3.25f),
                ScoredCandidate("かな", -4.5f),
            ),
            parseScoredPredictionJson(
                """[{"word":"仮名","score":-3.25},{"word":"かな","score":-4.5}]""",
            ),
        )
    }

    @Test
    fun malformedJsonFallsBackToEmptyCandidates() {
        assertEquals(emptyList<String>(), parseCandidateJson("not-json"))
        assertEquals(emptyList<String>(), parseCandidateJson("{}"))
    }
}
