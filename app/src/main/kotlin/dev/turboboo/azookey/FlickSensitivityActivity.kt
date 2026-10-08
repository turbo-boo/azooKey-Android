package dev.turboboo.azookey

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import dev.turboboo.azookey.ime.FlickSensitivityStore

class FlickSensitivityActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = FlickSensitivityStore(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val title = TextView(this).apply {
            text = getString(R.string.flick_sensitivity)
            textSize = 24f
        }
        layout.addView(title)
        layout.addView(TextView(this).apply {
            text = getString(R.string.flick_sensitivity_description)
            textSize = 15f
            setPadding(0, dp(12), 0, dp(20))
        })
        val value = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER_HORIZONTAL
        }
        layout.addView(value)
        fun updateValue(percent: Int) {
            value.text = getString(R.string.flick_sensitivity_value, percent)
        }
        val slider = SeekBar(this).apply {
            max = FlickSensitivityStore.MAX_PERCENT - FlickSensitivityStore.MIN_PERCENT
            progress = store.percent() - FlickSensitivityStore.MIN_PERCENT
        }
        updateValue(store.percent())
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val percent = progress + FlickSensitivityStore.MIN_PERCENT
                store.setPercent(percent)
                updateValue(percent)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        layout.addView(slider, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        layout.addView(Button(this).apply {
            text = getString(R.string.flick_sensitivity_reset)
            isAllCaps = false
            setOnClickListener {
                store.setPercent(FlickSensitivityStore.DEFAULT_PERCENT)
                slider.progress = FlickSensitivityStore.DEFAULT_PERCENT -
                    FlickSensitivityStore.MIN_PERCENT
                updateValue(FlickSensitivityStore.DEFAULT_PERCENT)
            }
        })
        setContentView(layout)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
