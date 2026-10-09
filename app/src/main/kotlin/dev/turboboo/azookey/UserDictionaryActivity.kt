package dev.turboboo.azookey

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.turboboo.azookey.userdictionary.UserDictionaryEntry
import dev.turboboo.azookey.userdictionary.UserDictionaryStore

class UserDictionaryActivity : Activity() {
    private lateinit var store: UserDictionaryStore
    private lateinit var entriesContainer: LinearLayout
    private lateinit var readingInput: EditText
    private lateinit var wordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = UserDictionaryStore(this)
        val padding = dp(20)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        content.addView(
            TextView(this).apply {
                text = getString(R.string.user_dictionary)
                textSize = 24f
            },
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.user_dictionary_description)
                textSize = 15f
                setPadding(0, dp(8), 0, dp(16))
            },
        )

        readingInput = EditText(this).apply {
            hint = getString(R.string.user_dictionary_reading_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
        }
        content.addView(
            readingInput,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        wordInput = EditText(this).apply {
            hint = getString(R.string.user_dictionary_word_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
        }
        content.addView(
            wordInput,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        content.addView(
            Button(this).apply {
                text = getString(R.string.user_dictionary_add)
                isAllCaps = false
                setOnClickListener {
                    val added = store.add(
                        reading = readingInput.text.toString(),
                        word = wordInput.text.toString(),
                    )
                    if (added) {
                        readingInput.text.clear()
                        wordInput.text.clear()
                        renderEntries()
                    } else {
                        Toast.makeText(
                            this@UserDictionaryActivity,
                            R.string.user_dictionary_invalid,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        entriesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, 0)
        }
        content.addView(
            entriesContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        setContentView(
            ScrollView(this).apply {
                addView(
                    content,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )
        renderEntries()
    }

    private fun renderEntries() {
        entriesContainer.removeAllViews()
        val entries = store.entries()
        if (entries.isEmpty()) {
            entriesContainer.addView(
                TextView(this).apply {
                    text = getString(R.string.user_dictionary_empty)
                },
            )
            return
        }

        entries.forEach { entry ->
            entriesContainer.addView(entryRow(entry))
        }
    }

    private fun entryRow(entry: UserDictionaryEntry): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                TextView(this@UserDictionaryActivity).apply {
                    text = "${entry.reading}  →  ${entry.word}"
                    textSize = 16f
                },
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            addView(
                Button(this@UserDictionaryActivity).apply {
                    text = getString(R.string.user_dictionary_delete)
                    isAllCaps = false
                    setOnClickListener {
                        store.remove(entry)
                        renderEntries()
                    }
                },
            )
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
