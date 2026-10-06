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

    private fun createView(callbacks: RecordingCallbacks): JapaneseFlickKeyboardView {
        val context: Context = RuntimeEnvironment.getApplication()
        return JapaneseFlickKeyboardView(context, callbacks)
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
        var deleteCount = 0

        override fun onText(text: String) {
            textInputs += text
        }

        override fun onDelete() {
            deleteCount++
        }

        override fun onChangeCharacterType() = Unit

        override fun onSpace() = Unit

        override fun onEnter() = Unit

        override fun onNextKeyboard() = Unit
    }
}
