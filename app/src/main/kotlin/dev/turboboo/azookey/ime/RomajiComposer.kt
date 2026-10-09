package dev.turboboo.azookey.ime

/**
 * Incremental, offline romaji-to-kana input. The Android state machine is
 * original; built-in azooKey layouts are the reference for key placement.
 */
internal class RomajiComposer {
    private var pending = ""
    val hasPending get() = pending.isNotEmpty()

    fun accept(text: String): String {
        val output = StringBuilder()
        for (ch in text.lowercase()) {
            if (ch !in 'a'..'z' && ch != '\'') {
                output.append(flush())
                output.append(ch)
                continue
            }
            pending += ch
            while (pending.isNotEmpty()) {
                if (pending.startsWith("n'")) {
                    output.append("ん")
                    pending = pending.drop(2)
                    continue
                }
                if (pending.startsWith("nn") && pending.length > 2) {
                    output.append("ん")
                    pending = pending.drop(1)
                    continue
                }
                if (pending.length > 1 && pending[0] == pending[1] &&
                    pending[0] !in "aeioun"
                ) {
                    output.append("っ")
                    pending = pending.drop(1)
                    continue
                }
                val mapped = MAP[pending]
                if (mapped != null && pending != "nn") {
                    output.append(mapped)
                    pending = ""
                    continue
                }
                if (pending in PREFIXES) break
                if (pending[0] == 'n' && pending.length > 1 &&
                    pending[1] !in "aiueoyn"
                ) {
                    output.append("ん")
                    pending = pending.drop(1)
                    continue
                }
                output.append(pending[0])
                pending = pending.drop(1)
            }
        }
        return output.toString()
    }

    fun backspace(): Boolean {
        if (pending.isEmpty()) return false
        pending = pending.dropLast(1)
        return true
    }

    fun flush(): String = when (val tail = pending) {
        "n", "nn" -> "ん"
        else -> tail
    }.also { pending = "" }

    fun reset() { pending = "" }

    private companion object {
        val MAP = buildMap<String, String> {
            val base = mapOf(
                "" to "あいうえお", "k" to "かきくけこ", "s" to "さしすせそ",
                "t" to "たちつてと", "n" to "なにぬねの", "h" to "はひふへほ",
                "m" to "まみむめも", "r" to "らりるれろ", "g" to "がぎぐげご",
                "z" to "ざじずぜぞ", "d" to "だぢづでど", "b" to "ばびぶべぼ",
                "p" to "ぱぴぷぺぽ",
            )
            for ((consonant, kana) in base) {
                "aiueo".forEachIndexed { i, vowel ->
                    put("$consonant$vowel", kana[i].toString())
                }
            }
            putAll(mapOf(
                "ya" to "や", "yu" to "ゆ", "yo" to "よ",
                "wa" to "わ", "wo" to "を", "wi" to "うぃ", "we" to "うぇ",
                "shi" to "し", "chi" to "ち", "tsu" to "つ", "fu" to "ふ",
                "ji" to "じ", "si" to "し", "ti" to "ち", "tu" to "つ",
                "hu" to "ふ", "zi" to "じ", "nn" to "ん",
                "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
                "sya" to "しゃ", "syu" to "しゅ", "syo" to "しょ",
                "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
                "tya" to "ちゃ", "tyu" to "ちゅ", "tyo" to "ちょ",
                "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
                "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
                "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
                "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
                "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
                "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
                "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
                "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
                "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
                "fa" to "ふぁ", "fi" to "ふぃ", "fe" to "ふぇ", "fo" to "ふぉ",
                "va" to "ゔぁ", "vi" to "ゔぃ", "vu" to "ゔ", "ve" to "ゔぇ", "vo" to "ゔぉ",
                "xtsu" to "っ", "ltsu" to "っ", "xtu" to "っ", "ltu" to "っ",
            ))
            for (smallPrefix in listOf("x", "l")) {
                "aiueo".forEachIndexed { i, vowel ->
                    put("$smallPrefix$vowel", "ぁぃぅぇぉ"[i].toString())
                }
            }
        }
        val PREFIXES = MAP.keys.flatMap { key ->
            (1..key.length).map { key.take(it) }
        }.toSet()
    }
}
