package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.FlickDirection

/*
 * The 500 ms all-suggestion delay and one-direction transition mirror
 * azooKey's UnifiedGenericKeyView press lifecycle.
 *
 * Upstream:
 *   azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 *   AzooKeyCore/Sources/KeyboardViews/View/UnifiedKey/UnifiedGenericKeyView.swift
 * Original implementation author: Keita Miwa (ensan) and azooKey contributors
 *
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
internal sealed interface FlickSuggestionState {
    object All : FlickSuggestionState

    data class Direction(
        val direction: FlickDirection,
    ) : FlickSuggestionState
}

internal class FlickSuggestionController(
    private val scheduler: RepeatScheduler,
    private val onSuggestion: (FlickSuggestionState?) -> Unit,
) {
    private var pressed = false
    private var pendingAll: RepeatTask? = null
    private var currentDirection: FlickDirection? = null

    fun onDown() {
        if (pressed) {
            return
        }

        pressed = true
        currentDirection = null
        pendingAll?.cancel()
        pendingAll = scheduler.schedule(
            AzooKeyVisualDesign.ALL_FLICK_SUGGEST_DELAY_MS,
        ) {
            if (pressed && currentDirection == null) {
                onSuggestion(FlickSuggestionState.All)
            }
        }
    }

    fun onDirection(direction: FlickDirection) {
        if (!pressed || direction == FlickDirection.CENTER) {
            return
        }
        if (currentDirection == direction) {
            return
        }

        currentDirection = direction
        pendingAll?.cancel()
        pendingAll = null
        onSuggestion(FlickSuggestionState.Direction(direction))
    }

    fun onUp() {
        finish()
    }

    fun onCancel() {
        finish()
    }

    private fun finish() {
        if (!pressed) {
            return
        }

        pressed = false
        currentDirection = null
        pendingAll?.cancel()
        pendingAll = null
        onSuggestion(null)
    }
}
