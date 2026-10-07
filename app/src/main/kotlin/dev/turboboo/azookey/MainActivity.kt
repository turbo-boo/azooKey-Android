package dev.turboboo.azookey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = dp(24)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        root.addView(
            TextView(this).apply {
                text = getString(R.string.setup_description)
                textSize = 18f
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            Button(this).apply {
                text = getString(R.string.enable_keyboard)
                isAllCaps = false
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            Button(this).apply {
                text = getString(R.string.user_dictionary)
                isAllCaps = false
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, UserDictionaryActivity::class.java))
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            Button(this).apply {
                text = getString(R.string.reset_learning)
                isAllCaps = false
                setOnClickListener {
                    val reset = runCatching {
                        val dictionaryRoot = java.io.File(
                            noBackupFilesDir,
                            "azookey-converter/dictionary-4d418525b090cf49c219819d05a7e3cc2a4346eb",
                        )
                        dev.turboboo.azookey.converter.AzooKeyAndroidJNI.resetLearningMemory(
                            dictionaryRoot.absolutePath,
                        )
                    }.getOrDefault(false)
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        if (reset) R.string.reset_learning_done else R.string.reset_learning_failed,
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            Button(this).apply {
                text = getString(R.string.choose_keyboard)
                isAllCaps = false
                setOnClickListener {
                    getSystemService(InputMethodManager::class.java)
                        .showInputMethodPicker()
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
