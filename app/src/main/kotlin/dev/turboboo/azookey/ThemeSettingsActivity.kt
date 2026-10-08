package dev.turboboo.azookey

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import dev.turboboo.azookey.ime.JapaneseFlickKeyboardView
import dev.turboboo.azookey.ime.KeyboardTheme
import dev.turboboo.azookey.ime.KeyboardThemeStore

class ThemeSettingsActivity : Activity() {
    private lateinit var store: KeyboardThemeStore
    private lateinit var originalTheme: KeyboardTheme
    private lateinit var draft: KeyboardTheme
    private lateinit var previewContainer: LinearLayout
    private lateinit var imageButton: Button
    private lateinit var removeImageButton: Button
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = KeyboardThemeStore(this)
        originalTheme = store.load()
        draft = originalTheme
        setContentView(buildContent())
        renderPreview()
    }

    private fun buildContent(): View {
        val padding = dp(16)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        content.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                addView(
                    Button(this@ThemeSettingsActivity).apply {
                        text = getString(R.string.theme_cancel)
                        isAllCaps = false
                        setOnClickListener { cancelEditing() }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f,
                    ),
                )
                addView(
                    TextView(this@ThemeSettingsActivity).apply {
                        text = getString(R.string.keyboard_theme)
                        textSize = 20f
                        gravity = Gravity.CENTER
                    },
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        2f,
                    ),
                )
                addView(
                    Button(this@ThemeSettingsActivity).apply {
                        text = getString(R.string.theme_save)
                        isAllCaps = false
                        setOnClickListener {
                            saved = true
                            store.save(draft)
                            finish()
                        }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f,
                    ),
                )
            },
        )

        content.addView(
            TextView(this).apply {
                text = getString(R.string.theme_description)
                textSize = 14f
                setPadding(0, dp(8), 0, dp(12))
            },
        )

        section(content, getString(R.string.theme_background))
        colorRow(
            content,
            getString(R.string.theme_background_color),
            draft.backgroundColor,
        ) { color ->
            updateDraft(draft.copy(backgroundColor = color))
        }

        imageButton = Button(this).apply {
            text = getString(R.string.theme_background_image)
            isAllCaps = false
            setOnClickListener(::pickBackgroundImage)
        }
        content.addView(imageButton, matchWrap())

        removeImageButton = Button(this).apply {
            text = getString(R.string.theme_background_image_remove)
            isAllCaps = false
            setTextColor(Color.RED)
            setOnClickListener {
                val builtIn = KeyboardTheme.builtIn(this@ThemeSettingsActivity)
                updateDraft(
                    draft.copy(
                        backgroundImageUri = null,
                        backgroundColor = builtIn.backgroundColor,
                        resultBackgroundColor = builtIn.resultBackgroundColor,
                    ),
                )
                updateImageControls()
            }
        }
        content.addView(removeImageButton, matchWrap())
        updateImageControls()

        section(content, getString(R.string.theme_text))
        sliderRow(
            content = content,
            label = getString(R.string.theme_font_weight),
            min = 1,
            max = 9,
            initial = draft.fontWeight,
        ) { value ->
            updateDraft(draft.copy(fontWeight = value))
        }

        section(content, getString(R.string.theme_candidates))
        colorRow(
            content,
            getString(R.string.theme_candidate_text_color),
            draft.resultTextColor,
        ) { color ->
            updateDraft(draft.copy(resultTextColor = color))
        }
        colorRow(
            content,
            getString(R.string.theme_candidate_background_color),
            draft.resultBackgroundColor,
        ) { color ->
            updateDraft(draft.copy(resultBackgroundColor = color))
        }

        section(content, getString(R.string.theme_keys))
        colorRow(
            content,
            getString(R.string.theme_key_text_color),
            draft.textColor,
        ) { color ->
            updateDraft(draft.copy(textColor = color))
        }
        colorRow(
            content,
            getString(R.string.theme_normal_key_color),
            draft.normalKeyColor,
        ) { color ->
            updateDraft(
                draft.copy(
                    normalKeyColor = color,
                    pressedKeyColor = KeyboardTheme.derivePressedColor(color),
                ),
            )
        }
        colorRow(
            content,
            getString(R.string.theme_special_key_color),
            draft.specialKeyColor,
        ) { color ->
            updateDraft(draft.copy(specialKeyColor = color))
        }
        colorRow(
            content,
            getString(R.string.theme_border_color),
            draft.borderColor,
        ) { color ->
            updateDraft(draft.copy(borderColor = color))
        }
        floatSliderRow(
            content = content,
            label = getString(R.string.theme_border_width),
            max = 10f,
            initial = draft.borderWidthDp,
        ) { value ->
            updateDraft(draft.copy(borderWidthDp = value))
        }

        content.addView(
            Button(this).apply {
                text = getString(R.string.theme_reset)
                isAllCaps = false
                setTextColor(Color.RED)
                setOnClickListener {
                    updateDraft(KeyboardTheme.builtIn(this@ThemeSettingsActivity))
                    setContentView(buildContent())
                    renderPreview()
                }
            },
            matchWrap(),
        )

        previewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        content.addView(
            previewContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        return ScrollView(this).apply {
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }

    private fun section(
        parent: LinearLayout,
        title: String,
    ) {
        parent.addView(
            TextView(this).apply {
                text = title
                textSize = 13f
                alpha = 0.7f
                setPadding(0, dp(18), 0, dp(6))
            },
        )
    }

    private fun colorRow(
        parent: LinearLayout,
        label: String,
        initial: Int,
        onChanged: (Int) -> Unit,
    ) {
        val swatch = Button(this).apply {
            isAllCaps = false
            updateColorButton(this, initial)
        }

        parent.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                addView(
                    TextView(this@ThemeSettingsActivity).apply {
                        text = label
                        textSize = 16f
                    },
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f,
                    ),
                )
                addView(
                    swatch,
                    LinearLayout.LayoutParams(
                        dp(112),
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }.also {
                swatch.setOnClickListener {
                    showColorPicker(
                        title = label,
                        initial = colorFromButton(swatch),
                    ) { color ->
                        updateColorButton(swatch, color)
                        onChanged(color)
                    }
                }
            },
            matchWrap(),
        )
    }

    private fun sliderRow(
        content: LinearLayout,
        label: String,
        min: Int,
        max: Int,
        initial: Int,
        onChanged: (Int) -> Unit,
    ) {
        val value = TextView(this).apply {
            text = initial.toString()
            gravity = Gravity.END
        }
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = initial - min
        }

        content.addView(
            TextView(this).apply {
                text = label
                textSize = 16f
            },
        )
        content.addView(value, matchWrap())
        content.addView(seek, matchWrap())

        seek.setOnSeekBarChangeListener(
            simpleSeekListener { progress ->
                val resolved = progress + min
                value.text = resolved.toString()
                onChanged(resolved)
            },
        )
    }

    private fun floatSliderRow(
        content: LinearLayout,
        label: String,
        max: Float,
        initial: Float,
        onChanged: (Float) -> Unit,
    ) {
        val value = TextView(this).apply {
            text = String.format("%.1f", initial)
            gravity = Gravity.END
        }
        val seek = SeekBar(this).apply {
            this.max = (max * 10f).toInt()
            progress = (initial * 10f).toInt()
        }

        content.addView(
            TextView(this).apply {
                text = label
                textSize = 16f
            },
        )
        content.addView(value, matchWrap())
        content.addView(seek, matchWrap())

        seek.setOnSeekBarChangeListener(
            simpleSeekListener { progress ->
                val resolved = progress / 10f
                value.text = String.format("%.1f", resolved)
                onChanged(resolved)
            },
        )
    }

    private fun simpleSeekListener(
        onProgress: (Int) -> Unit,
    ): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean,
            ) {
                if (fromUser) {
                    onProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }

    private fun updateDraft(theme: KeyboardTheme) {
        draft = theme
        store.save(draft)
        renderPreview()
    }

    private fun renderPreview() {
        if (!::previewContainer.isInitialized) {
            return
        }

        previewContainer.removeAllViews()
        previewContainer.addView(
            JapaneseFlickKeyboardView(
                context = this,
                callbacks = object : JapaneseFlickKeyboardView.Callbacks {
                    override fun onText(text: String) = Unit
                    override fun onCandidate(text: String) = Unit
                    override fun onDelete() = Unit
                    override fun onChangeCharacterType() = Unit
                    override fun onSpace() = Unit
                    override fun onEnter() = Unit
                    override fun onNextKeyboard() = Unit
                    override fun onMoveCursor(direction: Int) = Unit
                    override fun onPaste() = Unit
                    override fun onKeyboardModeChanged(mode: dev.turboboo.azookey.ime.KeyboardInputMode) = Unit
                },
            ).apply {
                setCandidates(listOf("変換", "候補", "予測"))
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun pickBackgroundImage(view: View) {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
            },
            REQUEST_BACKGROUND_IMAGE,
        )
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (
            requestCode != REQUEST_BACKGROUND_IMAGE ||
            resultCode != RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        updateDraft(
            draft.copy(
                backgroundImageUri = uri.toString(),
                backgroundColor = Color.TRANSPARENT,
                resultBackgroundColor = Color.TRANSPARENT,
            ),
        )
        updateImageControls()
    }

    private fun updateImageControls() {
        if (!::removeImageButton.isInitialized) {
            return
        }
        val hasImage = draft.backgroundImageUri != null
        imageButton.text =
            if (hasImage) "画像を選び直す" else getString(R.string.theme_background_image)
        removeImageButton.visibility = if (hasImage) View.VISIBLE else View.GONE
    }

    private fun showColorPicker(
        title: String,
        initial: Int,
        onSelected: (Int) -> Unit,
    ) {
        val preview = View(this).apply {
            setBackgroundColor(initial)
        }
        val hex = EditText(this).apply {
            isEnabled = false
            gravity = Gravity.CENTER
        }

        fun componentRow(
            label: String,
            initialValue: Int,
        ): Pair<LinearLayout, SeekBar> {
            val seek = SeekBar(this).apply {
                max = 255
                progress = initialValue
            }
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(this@ThemeSettingsActivity).apply {
                        text = label
                        gravity = Gravity.CENTER
                    },
                    LinearLayout.LayoutParams(dp(28), dp(40)),
                )
                addView(
                    seek,
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f,
                    ),
                )
            } to seek
        }

        val (alphaRow, alpha) = componentRow("A", Color.alpha(initial))
        val (redRow, red) = componentRow("R", Color.red(initial))
        val (greenRow, green) = componentRow("G", Color.green(initial))
        val (blueRow, blue) = componentRow("B", Color.blue(initial))

        fun selectedColor(): Int =
            Color.argb(
                alpha.progress,
                red.progress,
                green.progress,
                blue.progress,
            )

        fun refresh() {
            val color = selectedColor()
            preview.setBackgroundColor(color)
            hex.setText("#%08X".format(color))
        }

        val listener = simpleSeekListener { refresh() }
        alpha.setOnSeekBarChangeListener(listener)
        red.setOnSeekBarChangeListener(listener)
        green.setOnSeekBarChangeListener(listener)
        blue.setOnSeekBarChangeListener(listener)
        refresh()

        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(
                preview,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48),
                ),
            )
            addView(hex, matchWrap())
            addView(alphaRow, matchWrap())
            addView(redRow, matchWrap())
            addView(greenRow, matchWrap())
            addView(blueRow, matchWrap())
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogContent)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onSelected(selectedColor())
            }
            .show()
    }

    private fun updateColorButton(
        button: Button,
        color: Int,
    ) {
        button.tag = color
        button.text = "#%08X".format(color)
        button.backgroundTintList = ColorStateList.valueOf(color)
        button.setTextColor(
            if (Color.luminance(color) > 0.5f) Color.BLACK else Color.WHITE,
        )
    }

    private fun colorFromButton(button: Button): Int =
        button.tag as? Int ?: Color.TRANSPARENT

    private fun cancelEditing() {
        store.save(originalTheme)
        finish()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        cancelEditing()
    }

    private fun matchWrap(): ViewGroup.LayoutParams =
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_BACKGROUND_IMAGE = 73
    }
}
