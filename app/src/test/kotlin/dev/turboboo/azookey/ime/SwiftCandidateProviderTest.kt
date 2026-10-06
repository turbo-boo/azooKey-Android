package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

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
    fun malformedJsonFallsBackToEmptyCandidates() {
        assertEquals(emptyList<String>(), parseCandidateJson("not-json"))
        assertEquals(emptyList<String>(), parseCandidateJson("{}"))
    }
}
