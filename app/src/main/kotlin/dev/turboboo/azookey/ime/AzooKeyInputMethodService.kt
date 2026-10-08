package dev.turboboo.azookey.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.KeyEvent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AzooKeyInputMethodService : InputMethodService() {
    private val controller = ImeController()
    private val inputRouter = ImeTextInputRouter(controller)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val candidateWorker = Executors.newSingleThreadExecutor()
    private lateinit var candidateCoordinator: CandidateCoordinator
    private var keyboardView: JapaneseFlickKeyboardView? = null
    private var activeEditorInfo: EditorInfo? = null
    private var inputPolicy = InputFieldPolicy.DEFAULT

    override fun onCreate() {
        super.onCreate()
        val predictionShadow = if (
            applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        ) {
            RustPredictionShadow()
        } else {
            null
        }
        candidateCoordinator = CandidateCoordinator(
            provider = SwiftCandidateProvider(
                context = this,
                predictionShadow = predictionShadow,
            ),
            workerExecutor = candidateWorker,
            mainExecutor = Executor { command ->
                mainHandler.post(command)
            },
            onCandidates = { candidates ->
                keyboardView?.setCandidates(candidates)
            },
        )
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        activeEditorInfo = info ?: activeEditorInfo
        inputPolicy = InputFieldPolicy.from(activeEditorInfo)
        keyboardView?.refreshTheme()
        if (!restarting) {
            keyboardView?.selectInputMode(
                inputPolicy.preferredMode ?: KeyboardInputMode.HIRAGANA,
            )
        }
        updateEnterKeyPresentation()
    }


    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        activeEditorInfo = attribute
        inputPolicy = InputFieldPolicy.from(attribute)
        controller.reset()
        inputRouter.reset()
        candidateCoordinator.clear()
        updateEnterKeyPresentation()
    }

    override fun onFinishInput() {
        controller.reset()
        inputRouter.reset()
        candidateCoordinator.clear()
        activeEditorInfo = null
        inputPolicy = InputFieldPolicy.DEFAULT
        updateEnterKeyPresentation()
        super.onFinishInput()
    }

    override fun onCreateInputView(): View {
        val view = JapaneseFlickKeyboardView(
            context = this,
            callbacks = object : JapaneseFlickKeyboardView.Callbacks {
                override fun onText(text: String) {
                    withEditorConnection { connection ->
                        val needsCandidates = inputRouter.input(
                            text = text,
                            mode = keyboardView?.inputMode ?: KeyboardInputMode.HIRAGANA,
                            policy = inputPolicy,
                            connection = connection,
                        )
                        if (needsCandidates) {
                            refreshCandidates()
                        } else {
                            candidateCoordinator.clear()
                            updateEnterKeyPresentation()
                        }
                    }
                }

                override fun onCandidate(text: String) {
                    if (!inputPolicy.allowSuggestions ||
                        keyboardView?.inputMode !in listOf(
                            KeyboardInputMode.HIRAGANA,
                            KeyboardInputMode.JAPANESE_QWERTY,
                        ) ||
                        controller.composingText.isEmpty()
                    ) {
                        return
                    }
                    withEditorConnection { connection ->
                        if (inputPolicy.allowLearning) {
                            candidateCoordinator.complete(text)
                        }
                        controller.selectCandidate(text, connection)
                        candidateCoordinator.clear()
                        updateEnterKeyPresentation()
                    }
                }

                override fun onDelete() {
                    withEditorConnection { connection ->
                        inputRouter.backspace(connection)
                        refreshCandidates()
                    }
                }

                override fun onChangeCharacterType() {
                    withEditorConnection { connection ->
                        if (!inputPolicy.directInput) {
                            controller.changeCharacterType(connection)
                            refreshCandidates()
                        }
                    }
                }

                override fun onSpace() {
                    withEditorConnection { connection ->
                        inputRouter.flush(connection)
                        controller.space(connection)
                        candidateCoordinator.clear()
                        updateEnterKeyPresentation()
                    }
                }

                override fun onEnter() {
                    withEditorConnection { connection ->
                        val hadComposition =
                            controller.composingText.isNotEmpty() || inputRouter.hasPendingRomaji
                        inputRouter.flush(connection)
                        controller.commit(connection)
                        candidateCoordinator.clear()
                        updateEnterKeyPresentation()

                        if (!hadComposition && !sendDefaultEditorAction(true)) {
                            connection.commitText("\n")
                        }
                    }
                }

                override fun onKeyboardModeChanged(mode: KeyboardInputMode) {
                    withEditorConnection { connection ->
                        inputRouter.flush(connection)
                        controller.commit(connection)
                    }
                    candidateCoordinator.clear()
                    updateEnterKeyPresentation()
                }

                override fun onMoveCursor(direction: Int) {
                    val connection = currentInputConnection ?: return
                    withEditorConnection { editor ->
                        inputRouter.flush(editor)
                        controller.commit(editor)
                    }
                    candidateCoordinator.clear()
                    val keyCode =
                        if (direction < 0) KeyEvent.KEYCODE_DPAD_LEFT
                        else KeyEvent.KEYCODE_DPAD_RIGHT
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                    updateEnterKeyPresentation()
                }

                override fun onPaste() {
                    val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .primaryClip ?: return
                    val text = clip.getItemAt(0).coerceToText(this@AzooKeyInputMethodService)
                        ?.toString() ?: return
                    if (text.isEmpty()) return
                    withEditorConnection { editor ->
                        inputRouter.flush(editor)
                        controller.commit(editor)
                        editor.commitText(text)
                    }
                    candidateCoordinator.clear()
                    updateEnterKeyPresentation()
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
        keyboardView = view
        updateEnterKeyPresentation()
        if (inputPolicy.allowSuggestions &&
            keyboardView?.inputMode in listOf(
                KeyboardInputMode.HIRAGANA,
                KeyboardInputMode.JAPANESE_QWERTY,
            )
        ) {
            candidateCoordinator.request(controller.composingText)
        } else {
            candidateCoordinator.clear()
        }
        return view
    }

    override fun onDestroy() {
        candidateWorker.shutdownNow()
        keyboardView = null
        super.onDestroy()
    }

    private fun refreshCandidates() {
        updateEnterKeyPresentation()
        if (inputPolicy.allowSuggestions &&
            keyboardView?.inputMode in listOf(
                KeyboardInputMode.HIRAGANA,
                KeyboardInputMode.JAPANESE_QWERTY,
            )
        ) {
            candidateCoordinator.request(controller.composingText)
        } else {
            candidateCoordinator.clear()
        }
    }

    private fun updateEnterKeyPresentation() {
        val imeOptions =
            activeEditorInfo?.imeOptions
                ?: currentInputEditorInfo?.imeOptions
                ?: EditorInfo.IME_ACTION_NONE
        keyboardView?.setEnterKeyLabel(
            EnterKeyPresentation.label(
                imeOptions = imeOptions,
                hasComposition = controller.composingText.isNotEmpty(),
            ),
        )
    }

    private inline fun withEditorConnection(block: (EditorConnection) -> Unit) {
        val connection = currentInputConnection ?: return
        block(AndroidEditorConnection(connection))
    }
}
