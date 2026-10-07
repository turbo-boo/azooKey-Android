package dev.turboboo.azookey.ime

import android.content.pm.ApplicationInfo
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val candidateWorker = Executors.newSingleThreadExecutor()
    private lateinit var candidateCoordinator: CandidateCoordinator
    private var keyboardView: JapaneseFlickKeyboardView? = null
    private var activeEditorInfo: EditorInfo? = null

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
        activeEditorInfo = info
        keyboardView?.refreshTheme()
        updateEnterKeyPresentation()
    }


    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        activeEditorInfo = attribute
        controller.reset()
        candidateCoordinator.clear()
        updateEnterKeyPresentation()
    }

    override fun onFinishInput() {
        controller.reset()
        candidateCoordinator.clear()
        activeEditorInfo = null
        updateEnterKeyPresentation()
        super.onFinishInput()
    }

    override fun onCreateInputView(): View {
        val view = JapaneseFlickKeyboardView(
            context = this,
            callbacks = object : JapaneseFlickKeyboardView.Callbacks {
                override fun onText(text: String) {
                    withEditorConnection { connection ->
                        controller.input(text, connection)
                        refreshCandidates()
                    }
                }

                override fun onCandidate(text: String) {
                    withEditorConnection { connection ->
                        candidateCoordinator.complete(text)
                        controller.selectCandidate(text, connection)
                        candidateCoordinator.clear()
                        updateEnterKeyPresentation()
                    }
                }

                override fun onDelete() {
                    withEditorConnection { connection ->
                        controller.backspace(connection)
                        refreshCandidates()
                    }
                }

                override fun onChangeCharacterType() {
                    withEditorConnection { connection ->
                        controller.changeCharacterType(connection)
                        refreshCandidates()
                    }
                }

                override fun onSpace() {
                    withEditorConnection { connection ->
                        controller.space(connection)
                        candidateCoordinator.clear()
                        updateEnterKeyPresentation()
                    }
                }

                override fun onEnter() {
                    withEditorConnection { connection ->
                        val hadComposition = controller.composingText.isNotEmpty()
                        controller.commit(connection)
                        candidateCoordinator.clear()
                        updateEnterKeyPresentation()

                        if (!hadComposition && !sendDefaultEditorAction(true)) {
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
        keyboardView = view
        updateEnterKeyPresentation()
        candidateCoordinator.request(controller.composingText)
        return view
    }

    override fun onDestroy() {
        candidateWorker.shutdownNow()
        keyboardView = null
        super.onDestroy()
    }

    private fun refreshCandidates() {
        updateEnterKeyPresentation()
        candidateCoordinator.request(controller.composingText)
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
