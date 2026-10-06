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
    fun malformedJsonFallsBackToEmptyCandidates() {
        assertEquals(emptyList<String>(), parseCandidateJson("not-json"))
        assertEquals(emptyList<String>(), parseCandidateJson("{}"))
    }
}
