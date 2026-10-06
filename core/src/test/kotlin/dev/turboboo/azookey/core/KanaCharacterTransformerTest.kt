package dev.turboboo.azookey.core

import org.junit.Assert.assertEquals
import org.junit.Test

class KanaCharacterTransformerTest {
    @Test
    fun smallKanaTogglesBackToLargeKana() {
        assertEquals("ぁ", KanaCharacterTransformer.transform("あ"))
        assertEquals("あ", KanaCharacterTransformer.transform("ぁ"))
        assertEquals("ゃ", KanaCharacterTransformer.transform("や"))
        assertEquals("や", KanaCharacterTransformer.transform("ゃ"))
    }

    @Test
    fun basicDakutenToggles() {
        assertEquals("が", KanaCharacterTransformer.transform("か"))
        assertEquals("か", KanaCharacterTransformer.transform("が"))
        assertEquals("じ", KanaCharacterTransformer.transform("し"))
        assertEquals("し", KanaCharacterTransformer.transform("じ"))
    }

    @Test
    fun haRowCyclesDakutenAndHandakuten() {
        assertEquals("ば", KanaCharacterTransformer.transform("は"))
        assertEquals("ぱ", KanaCharacterTransformer.transform("ば"))
        assertEquals("は", KanaCharacterTransformer.transform("ぱ"))
    }

    @Test
    fun tsuCyclesSmallKanaDakutenAndPlainKana() {
        assertEquals("っ", KanaCharacterTransformer.transform("つ"))
        assertEquals("づ", KanaCharacterTransformer.transform("っ"))
        assertEquals("つ", KanaCharacterTransformer.transform("づ"))
    }

    @Test
    fun uCyclesSmallKanaDakutenAndPlainKana() {
        assertEquals("ぅ", KanaCharacterTransformer.transform("う"))
        assertEquals("ゔ", KanaCharacterTransformer.transform("ぅ"))
        assertEquals("う", KanaCharacterTransformer.transform("ゔ"))
    }

    @Test
    fun unsupportedCharacterRemainsUnchanged() {
        assertEquals("ん", KanaCharacterTransformer.transform("ん"))
        assertEquals("。", KanaCharacterTransformer.transform("。"))
    }
}
