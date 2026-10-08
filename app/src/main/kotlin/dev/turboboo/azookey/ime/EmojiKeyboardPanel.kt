package dev.turboboo.azookey.ime

import android.content.Context
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray

/**
 * On-device emoji tab inspired by azooKey's EmojiTab.swift.
 * Upstream: azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 * AzooKeyCore/Sources/KeyboardViews/View/SpecialTabs/EmojiTab.swift
 * Original author: Keita Miwa (ensan) and azooKey contributors.
 * The Android widget and local recent-history handling are original.
 */
internal class EmojiKeyboardPanel(
    context: Context,
    private val onEmoji: (String) -> Unit,
) : ScrollView(context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "emoji_picker", Context.MODE_PRIVATE,
    )
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    init {
        isFillViewport = true
        isVerticalScrollBarEnabled = false
        addView(content, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        refreshTheme()
    }

    fun refreshTheme() {
        content.removeAllViews()
        val recent = recent()
        if (recent.isNotEmpty()) addCategory("最近使用した絵文字", recent)
        for ((name, emojis) in CATEGORIES) addCategory(name, emojis)
    }

    private fun addCategory(title: String, values: List<String>) {
        content.addView(TextView(context).apply {
            text = title
            textSize = 14f
            setPadding(dp(8), dp(10), 0, dp(6))
        })
        val grid = GridLayout(context).apply {
            columnCount = 8
            useDefaultMargins = false
            setPadding(dp(2), 0, dp(2), dp(8))
        }
        val width = (resources.displayMetrics.widthPixels - dp(8)) / 8
        values.forEachIndexed { index, emoji ->
            val button = Button(context).apply {
                text = emoji
                contentDescription = emoji
                setPadding(0, 0, 0, 0)
                AzooKeyViewStyle.styleKey(this, special = false, textSizeSp = 19f)
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    remember(emoji)
                    onEmoji(emoji)
                    refreshTheme()
                }
            }
            grid.addView(
                button,
                GridLayout.LayoutParams(
                    GridLayout.spec(index / 8), GridLayout.spec(index % 8),
                ).apply {
                    this.width = width
                    height = dp(43)
                },
            )
        }
        content.addView(grid, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
    }

    private fun recent(): List<String> = runCatching {
        val array = JSONArray(prefs.getString("recent", "[]"))
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.getOrDefault(emptyList())

    private fun remember(emoji: String) {
        val items = (listOf(emoji) + recent()).distinct().take(24)
        val array = JSONArray()
        items.forEach(array::put)
        prefs.edit().putString("recent", array.toString()).apply()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        val CATEGORIES = linkedMapOf(
            "表情" to "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😍 🥰 😘 😎 🤔 😢 😭 😡 🥺 😴 😷".split(" "),
            "動物" to "🐱 🐈 🐶 🐕 🐰 🐻 🐼 🦊 🐸 🐢 🦋 🐟 🐦 🐯 🦁 🐮 🐷 🐧 🐵 🐝".split(" "),
            "食べ物" to "🍎 🍊 🍋 🍓 🍇 🍌 🍉 🍑 🍍 🥝 🍔 🍟 🍕 🍣 🍙 🍜 🍱 🍰 🍫 ☕".split(" "),
            "自然" to "☀️ 🌙 ⭐ 🌟 🌈 🌸 🌹 🌻 🌳 🍁 🌊 🔥 ❄️ 🌧️ ☁️ 🌍".split(" "),
            "記号" to "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 💔 💕 💖 ✅ ❌ ⚠️ ➡️ ⬅️ ⬆️ ⬇️ ✨ 🎵".split(" "),
        )
    }
}
