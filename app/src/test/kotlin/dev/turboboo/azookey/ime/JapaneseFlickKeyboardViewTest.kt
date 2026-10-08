package dev.turboboo.azookey.ime

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Button
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JapaneseFlickKeyboardViewTest {
    @Test
    fun centerTapAndLeftFlickReachTextCallback() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)
        val key = findButton(view, "あ")

        gesture(
            key,
            MotionEvent.ACTION_DOWN to Point(400f, 200f),
            MotionEvent.ACTION_UP to Point(400f, 200f),
        )
        gesture(
            key,
            MotionEvent.ACTION_DOWN to Point(400f, 200f),
            MotionEvent.ACTION_UP to Point(0f, 200f),
        )

        assertEquals(listOf("あ", "い"), callbacks.textInputs)
    }

    @Test
    fun cancelledFlickProducesNoText() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)
        val key = findButton(view, "あ")

        gesture(
            key,
            MotionEvent.ACTION_DOWN to Point(400f, 200f),
            MotionEvent.ACTION_CANCEL to Point(0f, 200f),
        )

        assertTrue(callbacks.textInputs.isEmpty())
    }

    @Test
    fun candidateButtonsReachCandidateCallback() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)

        view.setCandidates(listOf("仮名", "かな"))
        findButton(view, "仮名").performClick()

        assertEquals(listOf("仮名"), callbacks.candidateSelections)
    }

    @Test
    fun measuredKeyboardUsesUpstreamPhonePortraitMetrics() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)
        val width = 370
        val density = view.resources.displayMetrics.density
        val expected = AzooKeyVisualDesign.phonePortrait(
            widthPx = width.toFloat(),
            density = density,
        )

        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )

        assertEquals(expected.keyboardHeightPx.toInt(), view.measuredHeight)
        assertEquals(
            expected.keyWidthPx.toInt(),
            findButton(view, "あ").measuredWidth,
        )
    }

    @Test
    fun candidateBarCanExpandAndCollapse() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)
        view.setCandidates(
            listOf("候補1", "候補2", "候補3", "候補4", "候補5", "候補6"),
        )

        val expand = findViewByDescription(view, "候補を展開")
        expand.performClick()

        assertEquals(
            "候補を閉じる",
            findViewByDescription(view, "候補を閉じる").contentDescription,
        )
        assertTrue(
            findViewByDescription(view, "展開候補一覧").visibility == View.VISIBLE,
        )

        findViewByDescription(view, "候補を閉じる").performClick()

        assertEquals(
            "候補を展開",
            findViewByDescription(view, "候補を展開").contentDescription,
        )
    }

    @Test
    fun deleteLongPressRepeatsAndStopsOnRelease() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)
        val delete = findButton(view, "⌫")

        dispatch(delete, MotionEvent.ACTION_DOWN, 100f, 100f)
        shadowOf(Looper.getMainLooper()).idleFor(
            Duration.ofMillis(ViewConfiguration.getLongPressTimeout().toLong() + 1L),
        )
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(180L))

        assertTrue(callbacks.deleteCount >= 2)

        dispatch(delete, MotionEvent.ACTION_UP, 100f, 100f)
        val countAfterRelease = callbacks.deleteCount
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(180L))

        assertEquals(countAfterRelease, callbacks.deleteCount)
    }

    @Test
    fun latinTabUsesAzooKeyEnglishFlickLayout() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)

        findButton(view, "ABC").performClick()
        val ghi = findButton(view, "GHI")

        gesture(
            ghi,
            MotionEvent.ACTION_DOWN to Point(400f, 200f),
            MotionEvent.ACTION_UP to Point(400f, 200f),
        )
        gesture(
            ghi,
            MotionEvent.ACTION_DOWN to Point(400f, 200f),
            MotionEvent.ACTION_UP to Point(0f, 200f),
        )

        assertEquals(listOf("g", "h"), callbacks.textInputs)
    }

    @Test
    fun latinUpperLowerKeyTogglesLetterOutput() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)

        findButton(view, "ABC").performClick()
        findButton(view, "a/A").performClick()
        findButton(view, "GHI").performClick()

        assertEquals(listOf("G"), callbacks.textInputs)
    }

    @Test
    fun numberSymbolsTabUsesAzooKeyNumberFlickLayout() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)

        findButton(view, "☆123").performClick()
        val one = findButton(view, "1☆♪→")

        gesture(
            one,
            MotionEvent.ACTION_DOWN to Point(400f, 200f),
            MotionEvent.ACTION_UP to Point(400f, 200f),
        )
        gesture(
            one,
            MotionEvent.ACTION_DOWN to Point(400f, 200f),
            MotionEvent.ACTION_UP to Point(0f, 200f),
        )

        assertEquals(listOf("1", "☆"), callbacks.textInputs)
    }

    @Test
    fun hiraganaTabReturnsFromAnotherMode() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)

        findButton(view, "ABC").performClick()
        findButton(view, "あいう").performClick()
        findButton(view, "あ").performClick()

        assertEquals(listOf("あ"), callbacks.textInputs)
    }

    @Test
    fun switchingModesReportsModeAndDoesNotReportRepeatedSelection() {
        val callbacks = RecordingCallbacks()
        val view = createView(callbacks)

        assertEquals(KeyboardInputMode.HIRAGANA, view.inputMode)
        findButton(view, "ABC").performClick()
        findButton(view, "ABC").performClick()
        findButton(view, "☆123").performClick()
        findButton(view, "あいう").performClick()

        assertEquals(
            listOf(
                KeyboardInputMode.LATIN,
                KeyboardInputMode.NUMBER_SYMBOLS,
                KeyboardInputMode.HIRAGANA,
            ),
            callbacks.modeChanges,
        )
        assertEquals(KeyboardInputMode.HIRAGANA, view.inputMode)
    }

    private fun createView(callbacks: RecordingCallbacks): JapaneseFlickKeyboardView {
        val context: Context = RuntimeEnvironment.getApplication()
        return JapaneseFlickKeyboardView(context, callbacks)
    }

    private fun findViewByDescription(
        root: View,
        description: String,
    ): View {
        if (root.contentDescription?.toString() == description) {
            return root
        }

        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                runCatching {
                    return findViewByDescription(
                        root.getChildAt(index),
                        description,
                    )
                }
            }
        }

        error("View not found: $description")
    }

    private fun findButton(root: View, label: String): Button {
        if (root is Button && root.text.toString() == label) {
            return root
        }

        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                runCatching {
                    return findButton(root.getChildAt(index), label)
                }
            }
        }

        error("Button not found: $label")
    }

    private fun gesture(
        view: View,
        vararg events: Pair<Int, Point>,
    ) {
        events.forEach { (action, point) ->
            dispatch(view, action, point.x, point.y)
        }
    }

    private fun dispatch(
        view: View,
        action: Int,
        x: Float,
        y: Float,
    ) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private data class Point(
        val x: Float,
        val y: Float,
    )

    private class RecordingCallbacks : JapaneseFlickKeyboardView.Callbacks {
        val textInputs = mutableListOf<String>()
        val candidateSelections = mutableListOf<String>()
        var deleteCount = 0

        override fun onText(text: String) {
            textInputs += text
        }

        override fun onCandidate(text: String) {
            candidateSelections += text
        }

        override fun onDelete() {
            deleteCount++
        }

        override fun onChangeCharacterType() = Unit

        override fun onSpace() = Unit

        override fun onEnter() = Unit

        val modeChanges = mutableListOf<KeyboardInputMode>()

        override fun onNextKeyboard() = Unit

        override fun onKeyboardModeChanged(mode: KeyboardInputMode) {
            modeChanges += mode
        }
    }
}
