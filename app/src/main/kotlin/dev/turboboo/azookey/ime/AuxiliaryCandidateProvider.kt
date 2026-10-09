package dev.turboboo.azookey.ime

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Android-side supplementary conversion candidates (date, time, numbers and
 * a small emoji index). Based on feature categories in azooKey's
 * InputManager specialCandidateProviders; implementation is original.
 */
internal class AuxiliaryCandidateProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun candidates(input: String): List<String> {
        return when (input) {
            "きょう", "今日" -> dateCandidates(LocalDate.now(clock))
            "あした", "明日" -> dateCandidates(LocalDate.now(clock).plusDays(1))
            "きのう", "昨日" -> dateCandidates(LocalDate.now(clock).minusDays(1))
            "いま", "今" -> {
                val time = LocalTime.now(clock)
                listOf(time.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
            "えがお", "わらう" -> listOf("😊", "😄", "😆")
            "はーと" -> listOf("❤️", "💕", "💗")
            "ねこ" -> listOf("🐈", "🐱")
            "いぬ" -> listOf("🐕", "🐶")
            else -> numberCandidates(input)
        }
    }

    private fun dateCandidates(date: LocalDate): List<String> = listOf(
        date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
        date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
        date.format(DateTimeFormatter.ofPattern("M月d日")),
    )

    private fun numberCandidates(input: String): List<String> {
        // Do not corrupt telephone numbers or identifiers with leading zeros.
        if (input.length !in 4..15 || input[0] == '0' || !input.all(Char::isDigit)) {
            return emptyList()
        }
        val parts = input.reversed().chunked(3)
        return listOf(parts.joinToString(",").reversed())
    }
}

internal fun appendAuxiliaryCandidates(
    main: List<String>,
    input: String,
    extras: AuxiliaryCandidateProvider = AuxiliaryCandidateProvider(),
    limit: Int = 10,
): List<String> {
    if (limit <= 0) return emptyList()
    val additional = extras.candidates(input).filter(String::isNotBlank).distinct()
    if (additional.isEmpty()) return main.filter(String::isNotBlank).distinct().take(limit)
    val reserved = minOf(3, additional.size, limit)
    val leading = main.filter(String::isNotBlank).distinct().take(limit - reserved)
    return (leading + additional + main)
        .filter(String::isNotBlank)
        .distinct()
        .take(limit)
}
