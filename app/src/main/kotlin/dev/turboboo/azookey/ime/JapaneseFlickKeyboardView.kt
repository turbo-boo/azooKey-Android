package dev.turboboo.azookey.ime

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import dev.turboboo.azookey.core.EnglishFlickLayout
import dev.turboboo.azookey.core.FlickDirection
import dev.turboboo.azookey.core.FlickDirectionResolver
import dev.turboboo.azookey.core.FlickKey
import dev.turboboo.azookey.core.JapaneseFlickLayout
import dev.turboboo.azookey.core.JapaneseSymbolsFlickLayout
import dev.turboboo.azookey.core.NumberSymbolsFlickLayout
import dev.turboboo.azookey.core.PointF2

/*
 * The 4-row x 5-column flick grids are ported from azooKey's built-in
 * Japanese, English, and number/symbol Custards.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/KeyboardViews/Custard/FlickJapaneseCustard.swift
 *   AzooKeyCore/Sources/KeyboardViews/Custard/FlickEnglishCustard.swift
 *   AzooKeyCore/Sources/KeyboardViews/Custard/FlickNumberSymbolsCustard.swift
 * Original implementation author: Keita Miwa (ensan)
 *
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
class JapaneseFlickKeyboardView(
    context: Context,
    private val callbacks: Callbacks,
) : LinearLayout(context) {
    interface Callbacks {
        fun onText(text: String)
        fun onCandidate(text: String)
        fun onDelete()
        fun onChangeCharacterType()
        fun onSpace()
        fun onEnter()
        fun onNextKeyboard()
    }

    private enum class KeyboardMode {
        HIRAGANA,
        LATIN,
        NUMBER_SYMBOLS,
    }

    private val keyHeight = dp(54)
    private val gap = dp(2)
    private val candidateRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        minimumHeight = dp(42)
    }
    private var keyboardMode = KeyboardMode.HIRAGANA
    private var latinUppercase = false

    init {
        orientation = VERTICAL
        setPadding(gap, gap, gap, gap)

        addView(createCandidateArea())
        addView(createGrid())
    }

    fun setCandidates(candidates: List<String>) {
        candidateRow.removeAllViews()

        candidates
            .asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .take(10)
            .forEach { candidate ->
                candidateRow.addView(
                    Button(context).apply {
                        text = candidate
                        isAllCaps = false
                        textSize = 16f
                        gravity = Gravity.CENTER
                        setPadding(dp(12), 0, dp(12), 0)
                        setOnClickListener {
                            callbacks.onCandidate(candidate)
                        }
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(42),
                    ),
                )
            }
    }

    private fun createCandidateArea(): View =
        HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            contentDescription = "Prediction candidates"
            addView(
                candidateRow,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(42),
                ),
            )
        }

    private fun createGrid(): GridLayout =
        GridLayout(context).apply {
            rowCount = 4
            columnCount = 5
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false

            addModeKeys()
            when (keyboardMode) {
                KeyboardMode.HIRAGANA -> addHiraganaKeys()
                KeyboardMode.LATIN -> addLatinKeys()
                KeyboardMode.NUMBER_SYMBOLS -> addNumberSymbolKeys()
            }
            addEditorKeys()
        }

    private fun GridLayout.addModeKeys() {
        addSpecialKey(
            label = "☆123",
            row = 0,
            column = 0,
            onClick = { switchKeyboardMode(KeyboardMode.NUMBER_SYMBOLS) },
        )
        addSpecialKey(
            label = "ABC",
            row = 1,
            column = 0,
            onClick = { switchKeyboardMode(KeyboardMode.LATIN) },
        )
        addSpecialKey(
            label = "あいう",
            row = 2,
            column = 0,
            onClick = { switchKeyboardMode(KeyboardMode.HIRAGANA) },
        )
        addSpecialKey("🌐", row = 3, column = 0, onClick = callbacks::onNextKeyboard)
    }

    private fun GridLayout.addHiraganaKeys() {
        addFlickKey(kana("あ"), row = 0, column = 1)
        addFlickKey(kana("か"), row = 0, column = 2)
        addFlickKey(kana("さ"), row = 0, column = 3)

        addFlickKey(kana("た"), row = 1, column = 1)
        addFlickKey(kana("な"), row = 1, column = 2)
        addFlickKey(kana("は"), row = 1, column = 3)

        addFlickKey(kana("ま"), row = 2, column = 1)
        addFlickKey(kana("や"), row = 2, column = 2)
        addFlickKey(kana("ら"), row = 2, column = 3)

        addSpecialKey("小ﾞﾟ", row = 3, column = 1, onClick = callbacks::onChangeCharacterType)
        addFlickKey(kana("わ"), row = 3, column = 2)
        addFlickKey(JapaneseSymbolsFlickLayout.key, row = 3, column = 3)
    }

    private fun GridLayout.addLatinKeys() {
        addFlickKey(english("@#/&_"), row = 0, column = 1)
        addFlickKey(english("ABC"), row = 0, column = 2)
        addFlickKey(english("DEF"), row = 0, column = 3)

        addFlickKey(english("GHI"), row = 1, column = 1)
        addFlickKey(english("JKL"), row = 1, column = 2)
        addFlickKey(english("MNO"), row = 1, column = 3)

        addFlickKey(english("PQRS"), row = 2, column = 1)
        addFlickKey(english("TUV"), row = 2, column = 2)
        addFlickKey(english("WXYZ"), row = 2, column = 3)

        addSpecialKey("a/A", row = 3, column = 1, onClick = ::toggleLatinCase)
        addFlickKey(english("'\"()"), row = 3, column = 2)
        addFlickKey(english(".,?!"), row = 3, column = 3)
    }

    private fun GridLayout.addNumberSymbolKeys() {
        addFlickKey(numberSymbol("1☆♪→"), row = 0, column = 1)
        addFlickKey(numberSymbol("2¥$€"), row = 0, column = 2)
        addFlickKey(numberSymbol("3%°#"), row = 0, column = 3)

        addFlickKey(numberSymbol("4○＊・"), row = 1, column = 1)
        addFlickKey(numberSymbol("5+×÷"), row = 1, column = 2)
        addFlickKey(numberSymbol("6<=>"), row = 1, column = 3)

        addFlickKey(numberSymbol("7「」:"), row = 2, column = 1)
        addFlickKey(numberSymbol("8〒々〆"), row = 2, column = 2)
        addFlickKey(numberSymbol("9^|\\"), row = 2, column = 3)

        addFlickKey(numberSymbol("()[]"), row = 3, column = 1)
        addFlickKey(numberSymbol("0〜…"), row = 3, column = 2)
        addFlickKey(numberSymbol(".,-/"), row = 3, column = 3)
    }

    private fun GridLayout.addEditorKeys() {
        addRepeatingDeleteKey(row = 0, column = 4)
        addSpecialKey("空白", row = 1, column = 4, onClick = callbacks::onSpace)
        addSpecialKey(
            "↵",
            row = 2,
            column = 4,
            rowSpan = 2,
            onClick = callbacks::onEnter,
        )
    }

    private fun switchKeyboardMode(mode: KeyboardMode) {
        if (keyboardMode == mode) {
            return
        }

        keyboardMode = mode
        replaceGrid()
    }

    private fun toggleLatinCase() {
        latinUppercase = !latinUppercase
        replaceGrid()
    }

    private fun replaceGrid() {
        if (childCount > 1) {
            removeViewAt(1)
        }
        addView(createGrid(), 1)
    }

    private fun GridLayout.addFlickKey(
        key: FlickKey,
        row: Int,
        column: Int,
    ) {
        addView(
            FlickKeyButton(
                context = context,
                key = key,
                onInput = callbacks::onText,
            ),
            keyLayoutParams(row, column),
        )
    }

    private fun GridLayout.addRepeatingDeleteKey(
        row: Int,
        column: Int,
    ) {
        addView(
            RepeatingActionButton(
                context = context,
                label = "⌫",
                onAction = callbacks::onDelete,
            ),
            keyLayoutParams(row, column),
        )
    }

    private fun GridLayout.addSpecialKey(
        label: String,
        row: Int,
        column: Int,
        rowSpan: Int = 1,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
    ) {
        addView(
            Button(context).apply {
                text = label
                isAllCaps = false
                textSize = 15f
                gravity = Gravity.CENTER
                isEnabled = enabled
                alpha = if (enabled) 1f else 0.45f
                setPadding(0, 0, 0, 0)
                setOnClickListener { onClick?.invoke() }
            },
            keyLayoutParams(row, column, rowSpan),
        )
    }

    private fun keyLayoutParams(
        row: Int,
        column: Int,
        rowSpan: Int = 1,
    ): GridLayout.LayoutParams =
        GridLayout.LayoutParams(
            GridLayout.spec(row, rowSpan, 1f),
            GridLayout.spec(column, 1, 1f),
        ).apply {
            width = 0
            height = keyHeight * rowSpan
            setMargins(gap, gap, gap, gap)
        }

    private fun kana(center: String): FlickKey =
        requireNotNull(JapaneseFlickLayout.key(center)) {
            "Missing built-in kana key: $center"
        }

    private fun english(label: String): FlickKey =
        requireNotNull(EnglishFlickLayout.key(label, uppercase = latinUppercase)) {
            "Missing built-in English key: $label"
        }

    private fun numberSymbol(label: String): FlickKey =
        requireNotNull(NumberSymbolsFlickLayout.key(label)) {
            "Missing built-in number/symbol key: $label"
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

private class FlickKeyButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val key: FlickKey,
    private val onInput: (String) -> Unit,
) : Button(context, attrs) {
    private val resolver = FlickDirectionResolver(
        thresholdPx = 28f * resources.displayMetrics.density,
    )
    private var downPoint: PointF2? = null

    init {
        text = key.label
        isAllCaps = false
        textSize = 20f
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
        setTextColor(Color.BLACK)

        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downPoint = PointF2(event.rawX, event.rawY)
                    isPressed = true
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val start = downPoint
                    downPoint = null
                    isPressed = false

                    if (start != null) {
                        val direction = resolver.resolve(
                            start,
                            PointF2(event.rawX, event.rawY),
                        )
                        if (direction == FlickDirection.CENTER) {
                            performClick()
                        } else {
                            key.output(direction)?.let(onInput)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    downPoint = null
                    isPressed = false
                    true
                }

                else -> true
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        onInput(key.center)
        return true
    }
}

/*
 * azooKey's built-in flick delete key repeats delete while long-pressed.
 * The Android scheduling implementation is local to this port.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/CustardKit/CustardKit.swift
 * Original implementation author: Keita Miwa (ensan)
 * License: MIT
 */
private class RepeatingActionButton(
    context: Context,
    label: String,
    private val onAction: () -> Unit,
) : Button(context) {
    private val controller = RepeatingPressController(
        initialDelayMillis = ViewConfiguration.getLongPressTimeout().toLong(),
        repeatIntervalMillis = 60L,
        scheduler = HandlerRepeatScheduler(Handler(Looper.getMainLooper())),
        onTap = ::performClick,
        onRepeat = onAction,
    )

    init {
        text = label
        isAllCaps = false
        textSize = 15f
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)

        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isPressed = true
                    controller.onDown()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    isPressed = false
                    controller.onUp()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    isPressed = false
                    controller.onCancel()
                    true
                }

                else -> true
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        onAction()
        return true
    }

    override fun onDetachedFromWindow() {
        controller.onCancel()
        super.onDetachedFromWindow()
    }
}

private class HandlerRepeatScheduler(
    private val handler: Handler,
) : RepeatScheduler {
    override fun schedule(delayMillis: Long, action: () -> Unit): RepeatTask {
        val runnable = Runnable(action)
        handler.postDelayed(runnable, delayMillis)
        return object : RepeatTask {
            override fun cancel() {
                handler.removeCallbacks(runnable)
            }
        }
    }
}
