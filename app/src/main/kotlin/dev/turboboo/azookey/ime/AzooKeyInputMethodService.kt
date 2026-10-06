package dev.turboboo.azookey.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

class AzooKeyInputMethodService : InputMethodService() {
    private val controller = ImeController()

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        controller.reset()
    }

    override fun onFinishInput() {
        controller.reset()
        super.onFinishInput()
    }

    override fun onCreateInputView(): View =
        JapaneseFlickKeyboardView(
            context = this,
            callbacks = object : JapaneseFlickKeyboardView.Callbacks {
                override fun onText(text: String) {
                    withEditorConnection { controller.input(text, it) }
                }

                override fun onDelete() {
                    withEditorConnection { controller.backspace(it) }
                }

                override fun onChangeCharacterType() {
                    withEditorConnection { controller.changeCharacterType(it) }
                }

                override fun onSpace() {
                    withEditorConnection { controller.space(it) }
                }

                override fun onEnter() {
                    withEditorConnection { connection ->
                        controller.commit(connection)
                        if (!sendDefaultEditorAction(true)) {
                            connection.commitText("\n")
                        }
                    }
                }

                override fun onNextKeyboard() {
                    val switched = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        switchToNextInputMethod(false)
                    } else {
                        false
                    }
                    if (!switched) {
                        getSystemService(InputMethodManager::class.java)
                            .showInputMethodPicker()
                    }
                }
            },
        )

    private inline fun withEditorConnection(block: (EditorConnection) -> Unit) {
        val connection = currentInputConnection ?: return
        block(AndroidEditorConnection(connection))
    }
}
