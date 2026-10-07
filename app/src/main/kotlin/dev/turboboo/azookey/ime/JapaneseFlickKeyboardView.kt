package dev.turboboo.azookey.ime

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
import kotlin.math.roundToInt

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

    private data class KeyCell(
        val row: Int,
        val column: Int,
        val rowSpan: Int,
    )

    private val density = resources.displayMetrics.density
    private var currentMetrics: AzooKeyVisualDesign.Metrics? = null
    private val candidateRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(5), 0, dp(5), 0)
    }
    private val candidateArea: HorizontalScrollView = createCandidateArea()
    private val flickSuggestionPopup = FlickSuggestionPopup(context)
    private var currentGrid: GridLayout? = null
    private var keyboardMode = KeyboardMode.HIRAGANA
    private var latinUppercase = false

    init {
        orientation = VERTICAL
        setPadding(0, 0, 0, 0)
        setBackgroundColor(AzooKeyViewStyle.palette(context).background)

        addView(candidateArea)
        addView(createGrid().also { currentGrid = it })
    }

    fun setCandidates(candidates: List<String>) {
        candidateRow.removeAllViews()

        candidates
            .asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .take(10)
            .forEach { candidate ->
                val button = Button(context).apply {
                    text = candidate
                    gravity = Gravity.CENTER
                    AzooKeyViewStyle.styleCandidate(this)
                    setOnClickListener {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        callbacks.onCandidate(candidate)
                    }
                }
                candidateRow.addView(
                    button,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        candidateButtonHeight(),
                    ).apply {
                        marginEnd = dp(AzooKeyVisualDesign.CANDIDATE_SPACING_DP.toInt())
                    },
                )
            }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        if (width > 0) {
            val metrics = AzooKeyVisualDesign.phonePortrait(
                widthPx = width.toFloat(),
                density = density,
            )
            currentMetrics = metrics
            applyMetrics(metrics)

            val desiredHeight = metrics.keyboardHeightPx.roundToInt()
            val resolvedHeight = when (View.MeasureSpec.getMode(heightMeasureSpec)) {
                View.MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(heightMeasureSpec)
                View.MeasureSpec.AT_MOST ->
                    minOf(desiredHeight, View.MeasureSpec.getSize(heightMeasureSpec))
                else -> desiredHeight
            }
            super.onMeasure(
                widthMeasureSpec,
                View.MeasureSpec.makeMeasureSpec(
                    resolvedHeight,
                    View.MeasureSpec.EXACTLY,
                ),
            )
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun createCandidateArea(): HorizontalScrollView =
        HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            clipToPadding = false
            contentDescription = "Prediction candidates"
            setBackgroundColor(AzooKeyViewStyle.palette(context).resultBackground)
            addView(
                candidateRow,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }

    private fun candidateButtonHeight(): Int =
        currentMetrics?.candidateButtonHeightPx?.roundToInt() ?: dp(28)

    private fun applyMetrics(metrics: AzooKeyVisualDesign.Metrics) {
        val barSectionHeight =
            (metrics.keyboardBarHeightPx +
                AzooKeyVisualDesign.KEYBOARD_VERTICAL_PADDING_DP * density)
                .roundToInt()
        candidateArea.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            barSectionHeight,
        )
        candidateArea.setPadding(0, dp(6), 0, dp(6))

        for (index in 0 until candidateRow.childCount) {
            val child = candidateRow.getChildAt(index)
            val params = child.layoutParams as LinearLayout.LayoutParams
            params.height = metrics.candidateButtonHeightPx.roundToInt()
            child.layoutParams = params
        }

        val grid = currentGrid ?: return
        val gridHeight =
            (metrics.keyboardHeightPx - barSectionHeight).roundToInt()
        grid.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            gridHeight,
        )
        val horizontalInset = metrics.horizontalInsetPx.roundToInt()
        grid.setPadding(horizontalInset, 0, horizontalInset, 0)

        for (index in 0 until grid.childCount) {
            val child = grid.getChildAt(index)
            val cell = child.tag as? KeyCell ?: continue
            val params = child.layoutParams as GridLayout.LayoutParams
            params.width = metrics.keyWidthPx.roundToInt()
            params.height =
                (
                    metrics.keyHeightPx * cell.rowSpan +
                        metrics.verticalSpacingPx * (cell.rowSpan - 1)
                    ).roundToInt()
            params.setMargins(
                0,
                0,
                if (cell.column < 4) metrics.horizontalSpacingPx.roundToInt() else 0,
                if (cell.row + cell.rowSpan < 4) {
                    metrics.verticalSpacingPx.roundToInt()
                } else {
                    0
                },
            )
            child.layoutParams = params
        }
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
            selected = keyboardMode == KeyboardMode.NUMBER_SYMBOLS,
            onClick = { switchKeyboardMode(KeyboardMode.NUMBER_SYMBOLS) },
        )
        addSpecialKey(
            label = "ABC",
            row = 1,
            column = 0,
            selected = keyboardMode == KeyboardMode.LATIN,
            onClick = { switchKeyboardMode(KeyboardMode.LATIN) },
        )
        addSpecialKey(
            label = "あいう",
            row = 2,
            column = 0,
            selected = keyboardMode == KeyboardMode.HIRAGANA,
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

        addSpecialKey(
            "小ﾞﾟ",
            row = 3,
            column = 1,
            special = false,
            onClick = callbacks::onChangeCharacterType,
        )
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

        addSpecialKey(
            "a/A",
            row = 3,
            column = 1,
            special = false,
            selected = latinUppercase,
            onClick = ::toggleLatinCase,
        )
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
        addSpecialKey(
            "空白",
            row = 1,
            column = 4,
            special = false,
            onClick = callbacks::onSpace,
        )
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
        currentGrid?.let(::removeView)
        val grid = createGrid()
        currentGrid = grid
        addView(grid, 1)
        currentMetrics?.let(::applyMetrics)
    }

    private fun GridLayout.addFlickKey(
        key: FlickKey,
        row: Int,
        column: Int,
    ) {
        val button = FlickKeyButton(
            context = context,
            key = key,
            onInput = callbacks::onText,
            onSuggestion = { anchor, suggestion ->
                flickSuggestionPopup.update(
                    anchor = anchor,
                    key = key,
                    state = suggestion,
                )
            },
        ).apply {
            tag = KeyCell(row, column, 1)
            AzooKeyViewStyle.styleKey(
                button = this,
                special = false,
            )
        }
        addView(
            button,
            keyLayoutParams(row, column),
        )
    }

    private fun GridLayout.addRepeatingDeleteKey(
        row: Int,
        column: Int,
    ) {
        val button = RepeatingActionButton(
            context = context,
            label = "⌫",
            onAction = callbacks::onDelete,
        ).apply {
            tag = KeyCell(row, column, 1)
            AzooKeyViewStyle.styleKey(
                button = this,
                special = true,
            )
        }
        addView(
            button,
            keyLayoutParams(row, column),
        )
    }

    private fun GridLayout.addSpecialKey(
        label: String,
        row: Int,
        column: Int,
        rowSpan: Int = 1,
        enabled: Boolean = true,
        special: Boolean = true,
        selected: Boolean = false,
        onClick: (() -> Unit)? = null,
    ) {
        val button = Button(context).apply {
            text = label
            gravity = Gravity.CENTER
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.45f
            setPadding(0, 0, 0, 0)
            tag = KeyCell(row, column, rowSpan)
            AzooKeyViewStyle.styleKey(
                button = this,
                special = special,
                selected = selected,
            )
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick?.invoke()
            }
        }
        addView(
            button,
            keyLayoutParams(row, column, rowSpan),
        )
    }

    private fun keyLayoutParams(
        row: Int,
        column: Int,
        rowSpan: Int = 1,
    ): GridLayout.LayoutParams =
        GridLayout.LayoutParams(
            GridLayout.spec(row, rowSpan),
            GridLayout.spec(column, 1),
        ).apply {
            width = 0
            height = 0
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

    override fun onDetachedFromWindow() {
        flickSuggestionPopup.dismiss()
        super.onDetachedFromWindow()
    }
}

private class FlickKeyButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val key: FlickKey,
    private val onInput: (String) -> Unit,
    private val onSuggestion: (View, FlickSuggestionState?) -> Unit,
) : Button(context, attrs) {
    private val resolver = FlickDirectionResolver(
        thresholdPx = 25f * resources.displayMetrics.density,
    )
    private val suggestionController = FlickSuggestionController(
        scheduler = HandlerRepeatScheduler(Handler(Looper.getMainLooper())),
        onSuggestion = { suggestion ->
            onSuggestion(this, suggestion)
        },
    )
    private var downPoint: PointF2? = null

    init {
        text = key.label
        isAllCaps = false
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)

        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downPoint = PointF2(event.rawX, event.rawY)
                    isPressed = true
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    suggestionController.onDown()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val start = downPoint
                    if (start != null) {
                        val direction = resolver.resolve(
                            start,
                            PointF2(event.rawX, event.rawY),
                        )
                        if (
                            direction != FlickDirection.CENTER &&
                            key.output(direction) != null
                        ) {
                            suggestionController.onDirection(direction)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val start = downPoint
                    downPoint = null
                    isPressed = false
                    suggestionController.onUp()

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
                    suggestionController.onCancel()
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

    override fun onDetachedFromWindow() {
        suggestionController.onCancel()
        super.onDetachedFromWindow()
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
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
