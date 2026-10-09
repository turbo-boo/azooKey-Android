package dev.turboboo.azookey.ime

/*
 * The press/long-press behavior is based on azooKey's built-in flick delete key:
 * a normal press deletes once and long-press repeats the delete action.
 *
 * Upstream:
 *   azooKey/azooKey
 *   AzooKeyCore/Sources/CustardKit/CustardKit.swift
 * Original implementation author: Keita Miwa (ensan)
 * License: MIT
 *
 * The scheduler/state-machine implementation below is Android-port-specific.
 */
internal interface RepeatTask {
    fun cancel()
}

internal fun interface RepeatScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): RepeatTask
}

internal class RepeatingPressController(
    private val initialDelayMillis: Long,
    private val repeatIntervalMillis: Long,
    private val scheduler: RepeatScheduler,
    private val onTap: () -> Unit,
    private val onRepeat: () -> Unit,
) {
    private var pressed = false
    private var repeating = false
    private var scheduledTask: RepeatTask? = null

    init {
        require(initialDelayMillis >= 0L) { "initialDelayMillis must not be negative" }
        require(repeatIntervalMillis > 0L) { "repeatIntervalMillis must be positive" }
    }

    fun onDown() {
        if (pressed) {
            return
        }

        pressed = true
        repeating = false
        scheduledTask = scheduler.schedule(initialDelayMillis) {
            if (pressed) {
                repeating = true
                onRepeat()
                scheduleNextRepeat()
            }
        }
    }

    fun onUp() {
        if (!pressed) {
            return
        }

        pressed = false
        scheduledTask?.cancel()
        scheduledTask = null

        val shouldTap = !repeating
        repeating = false
        if (shouldTap) {
            onTap()
        }
    }

    fun onCancel() {
        pressed = false
        repeating = false
        scheduledTask?.cancel()
        scheduledTask = null
    }

    private fun scheduleNextRepeat() {
        scheduledTask = scheduler.schedule(repeatIntervalMillis) {
            if (pressed) {
                onRepeat()
                scheduleNextRepeat()
            }
        }
    }
}
