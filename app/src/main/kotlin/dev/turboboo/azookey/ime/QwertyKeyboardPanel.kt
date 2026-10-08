package dev.turboboo.azookey.ime

import android.content.Context
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

/**
 * Android QWERTY counterpart for azooKey's built-in Japanese/English layouts.
 * Layout reference:
 * azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 * AzooKeyCore/Sources/KeyboardViews/Custard/QwertyCustards.swift
 * Original layout author: Keita Miwa (ensan) and azooKey contributors.
 * This Android implementation is original. See THIRD_PARTY_NOTICES.md.
 */
internal val KeyboardInputMode.isQwerty: Boolean
    get() = this == KeyboardInputMode.JAPANESE_QWERTY ||
        this == KeyboardInputMode.LATIN_QWERTY

internal class QwertyKeyboardPanel(
    context: Context,
    private val onText: (String) -> Unit,
    private val onDelete: () -> Unit,
    private val onSpace: () -> Unit,
    private val onEnter: () -> Unit,
    private val onModeChange: (KeyboardInputMode) -> Unit,
) : LinearLayout(context) {
    private var mode = KeyboardInputMode.JAPANESE_QWERTY
    private var enterLabel = "改行"
    private var upperCase = false

    init {
        orientation = VERTICAL
        rebuild()
    }

    fun setMode(value: KeyboardInputMode) {
        if (!value.isQwerty || mode == value) return
        mode = value
        upperCase = false
        rebuild()
    }

    fun setEnterLabel(value: String) {
        if (enterLabel == value) return
        enterLabel = value
        rebuild()
    }

    fun refreshTheme() = rebuild()

    private fun rebuild() {
        removeAllViews()
        for (letters in listOf("qwertyuiop", "asdfghjkl")) {
            addView(letterRow(letters), LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
            ))
        }
        val third = newRow()
        if (mode == KeyboardInputMode.LATIN_QWERTY) {
            third.addView(key("⇧", true, 1.3f) {
                upperCase = !upperCase
                rebuild()
            })
        }
        for (char in "zxcvbnm") third.addView(letterKey(char))
        third.addView(key("⌫", true, 1.5f, onDelete))
        addView(third, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottom = newRow()
        bottom.addView(key("あ", true, 1f) {
            onModeChange(KeyboardInputMode.JAPANESE_QWERTY)
        })
        bottom.addView(key("英", true, 1f) {
            onModeChange(KeyboardInputMode.LATIN_QWERTY)
        })
        bottom.addView(key("かな", true, 1f) {
            onModeChange(KeyboardInputMode.HIRAGANA)
        })
        bottom.addView(key("空白", false, 3f, onSpace))
        bottom.addView(key(enterLabel, true, 1.6f, onEnter))
        addView(bottom, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun letterRow(letters: String): LinearLayout =
        newRow().apply {
            for (letter in letters) addView(letterKey(letter))
        }

    private fun newRow(): LinearLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(2), dp(2), dp(2), dp(2))
    }

    private fun letterKey(letter: Char): Button {
        val text = if (upperCase && mode == KeyboardInputMode.LATIN_QWERTY) {
            letter.uppercaseChar().toString()
        } else {
            letter.toString()
        }
        return key(text, false, 1f) { onText(text) }
    }

    private fun key(
        label: String,
        special: Boolean,
        weight: Float,
        action: () -> Unit,
    ): Button = Button(context).apply {
        text = label
        gravity = Gravity.CENTER
        isAllCaps = false
        setPadding(0, 0, 0, 0)
        AzooKeyViewStyle.styleKey(this, special = special)
        layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
