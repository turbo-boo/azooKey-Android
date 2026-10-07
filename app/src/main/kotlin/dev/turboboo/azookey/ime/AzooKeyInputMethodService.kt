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

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        controller.reset()
        candidateCoordinator.clear()
    }

    override fun onFinishInput() {
        controller.reset()
        candidateCoordinator.clear()
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
                        controller.selectCandidate(text, connection)
                        candidateCoordinator.clear()
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
                    }
                }

                override fun onEnter() {
                    withEditorConnection { connection ->
                        controller.commit(connection)
                        candidateCoordinator.clear()
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
        keyboardView = view
        candidateCoordinator.request(controller.composingText)
        return view
    }

    override fun onDestroy() {
        candidateWorker.shutdownNow()
        keyboardView = null
        super.onDestroy()
    }

    private fun refreshCandidates() {
        candidateCoordinator.request(controller.composingText)
    }

    private inline fun withEditorConnection(block: (EditorConnection) -> Unit) {
        val connection = currentInputConnection ?: return
        block(AndroidEditorConnection(connection))
    }
}
