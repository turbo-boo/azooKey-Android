package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class AuxiliaryCandidateProviderTest {
    private val provider = AuxiliaryCandidateProvider(
        Clock.fixed(Instant.parse("2026-10-08T12:34:00Z"), ZoneId.of("Asia/Tokyo")),
    )

    @Test fun datesUseDeviceLocalZoneAndRealCalendar() {
        assertEquals(
            listOf("2026/10/08", "2026年10月8日", "10月8日"),
            provider.candidates("きょう"),
        )
        assertEquals("2026/10/09", provider.candidates("あした").first())
        assertEquals("2026/10/07", provider.candidates("きのう").first())
        assertEquals(listOf("21:34"), provider.candidates("いま"))
    }

    @Test fun formatThousandsWithoutRewritingIdentifiers() {
        assertEquals(listOf("1,234,567"), provider.candidates("1234567"))
        assertEquals(emptyList<String>(), provider.candidates("0001234"))
        assertEquals(emptyList<String>(), provider.candidates("123"))
        assertEquals(emptyList<String>(), provider.candidates("1234a"))
    }

    @Test fun mergesWithoutDuplicatingOrHidingSwiftCandidates() {
        assertEquals(
            listOf("１２３４", "1,234"),
            appendAuxiliaryCandidates(
                listOf("１２３４", "１２３４"),
                "1234",
                extras = provider,
            ),
        )
        assertEquals(
            listOf("仮名"),
            appendAuxiliaryCandidates(listOf("仮名"), "かな", extras = provider),
        )
    }

    @Test fun supplementaryCandidatesAreReachableWithTenSwiftResults() {
        val swift = (1..10).map { "候補$it" }
        val merged = appendAuxiliaryCandidates(swift, "きょう", extras = provider)
        assertEquals(10, merged.size)
        assertEquals("候補1", merged.first())
        assertEquals(true, "2026/10/08" in merged)
        assertEquals(true, "2026年10月8日" in merged)
    }

    @Test fun supportsBasicEmojiSuggestions() {
        assertFalse(provider.candidates("ねこ").isEmpty())
        assertEquals("❤️", provider.candidates("はーと").first())
    }
}
