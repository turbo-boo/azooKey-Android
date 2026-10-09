package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.FlickDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class FlickSuggestionControllerTest {
    @Test
    fun holdingShowsAllSuggestionsAfterUpstreamDelay() {
        val scheduler = FakeScheduler()
        val events = mutableListOf<FlickSuggestionState?>()
        val controller = FlickSuggestionController(
            scheduler = scheduler,
            onSuggestion = events::add,
        )

        controller.onDown()

        assertEquals(
            listOf(AzooKeyVisualDesign.ALL_FLICK_SUGGEST_DELAY_MS),
            scheduler.delays,
        )
        assertEquals(emptyList<FlickSuggestionState?>(), events)

        scheduler.runNext()

        assertEquals(listOf(FlickSuggestionState.All), events)
    }

    @Test
    fun directionalFlickCancelsPendingAllSuggestion() {
        val scheduler = FakeScheduler()
        val events = mutableListOf<FlickSuggestionState?>()
        val controller = FlickSuggestionController(
            scheduler = scheduler,
            onSuggestion = events::add,
        )

        controller.onDown()
        controller.onDirection(FlickDirection.LEFT)
        scheduler.runNext()

        assertEquals(
            listOf(FlickSuggestionState.Direction(FlickDirection.LEFT)),
            events,
        )
    }

    @Test
    fun movingBetweenDirectionsUpdatesSuggestion() {
        val scheduler = FakeScheduler()
        val events = mutableListOf<FlickSuggestionState?>()
        val controller = FlickSuggestionController(
            scheduler = scheduler,
            onSuggestion = events::add,
        )

        controller.onDown()
        controller.onDirection(FlickDirection.LEFT)
        controller.onDirection(FlickDirection.TOP)
        controller.onDirection(FlickDirection.TOP)

        assertEquals(
            listOf(
                FlickSuggestionState.Direction(FlickDirection.LEFT),
                FlickSuggestionState.Direction(FlickDirection.TOP),
            ),
            events,
        )
    }

    @Test
    fun releaseDismissesSuggestion() {
        val scheduler = FakeScheduler()
        val events = mutableListOf<FlickSuggestionState?>()
        val controller = FlickSuggestionController(
            scheduler = scheduler,
            onSuggestion = events::add,
        )

        controller.onDown()
        scheduler.runNext()
        controller.onUp()

        assertEquals(
            listOf(FlickSuggestionState.All, null),
            events,
        )
    }

    private class FakeScheduler : RepeatScheduler {
        val delays = mutableListOf<Long>()
        private val tasks = ArrayDeque<FakeTask>()

        override fun schedule(
            delayMillis: Long,
            action: () -> Unit,
        ): RepeatTask {
            delays += delayMillis
            return FakeTask(action).also(tasks::addLast)
        }

        fun runNext() {
            val task = tasks.removeFirstOrNull() ?: return
            if (!task.cancelled) {
                task.action()
            }
        }

        private class FakeTask(
            val action: () -> Unit,
        ) : RepeatTask {
            var cancelled = false

            override fun cancel() {
                cancelled = true
            }
        }
    }
}
