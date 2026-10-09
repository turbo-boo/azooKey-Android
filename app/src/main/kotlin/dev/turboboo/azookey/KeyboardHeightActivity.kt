package dev.turboboo.azookey

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import dev.turboboo.azookey.ime.KeyboardHeightStore

class KeyboardHeightActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = KeyboardHeightStore(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.keyboard_height)
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.keyboard_height_description)
            textSize = 15f
            setPadding(0, dp(12), 0, dp(20))
        })
        val value = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(value)
        fun update(percent: Int) {
            value.text = getString(R.string.keyboard_height_value, percent)
        }
        val slider = SeekBar(this).apply {
            max = KeyboardHeightStore.MAX_PERCENT - KeyboardHeightStore.MIN_PERCENT
            progress = store.percent() - KeyboardHeightStore.MIN_PERCENT
        }
        update(store.percent())
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val percent = progress + KeyboardHeightStore.MIN_PERCENT
                store.setPercent(percent)
                update(percent)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        root.addView(slider, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        root.addView(Button(this).apply {
            text = getString(R.string.keyboard_height_reset)
            isAllCaps = false
            setOnClickListener {
                store.setPercent(KeyboardHeightStore.DEFAULT_PERCENT)
                slider.progress = KeyboardHeightStore.DEFAULT_PERCENT -
                    KeyboardHeightStore.MIN_PERCENT
                update(KeyboardHeightStore.DEFAULT_PERCENT)
            }
        })
        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
