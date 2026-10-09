package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatingPressControllerTest {
    @Test
    fun quickReleaseProducesSingleTap() {
        val scheduler = FakeRepeatScheduler()
        val calls = mutableListOf<String>()
        val controller = RepeatingPressController(
            initialDelayMillis = 400,
            repeatIntervalMillis = 60,
            scheduler = scheduler,
            onTap = { calls += "tap" },
            onRepeat = { calls += "repeat" },
        )

        controller.onDown()
        controller.onUp()

        assertEquals(listOf("tap"), calls)
        scheduler.runNext()
        assertEquals(listOf("tap"), calls)
    }

    @Test
    fun holdingPastInitialDelayRepeatsAndSuppressesTap() {
        val scheduler = FakeRepeatScheduler()
        val calls = mutableListOf<String>()
        val controller = RepeatingPressController(
            initialDelayMillis = 400,
            repeatIntervalMillis = 60,
            scheduler = scheduler,
            onTap = { calls += "tap" },
            onRepeat = { calls += "repeat" },
        )

        controller.onDown()
        scheduler.runNext()
        scheduler.runNext()
        scheduler.runNext()
        controller.onUp()

        assertEquals(
            listOf("repeat", "repeat", "repeat"),
            calls,
        )
    }

    @Test
    fun cancelStopsPendingAndRepeatedActionsWithoutTap() {
        val scheduler = FakeRepeatScheduler()
        val calls = mutableListOf<String>()
        val controller = RepeatingPressController(
            initialDelayMillis = 400,
            repeatIntervalMillis = 60,
            scheduler = scheduler,
            onTap = { calls += "tap" },
            onRepeat = { calls += "repeat" },
        )

        controller.onDown()
        scheduler.runNext()
        controller.onCancel()
        scheduler.runNext()

        assertEquals(listOf("repeat"), calls)
    }

    @Test
    fun repeatUsesConfiguredDelays() {
        val scheduler = FakeRepeatScheduler()
        val controller = RepeatingPressController(
            initialDelayMillis = 375,
            repeatIntervalMillis = 55,
            scheduler = scheduler,
            onTap = {},
            onRepeat = {},
        )

        controller.onDown()
        assertEquals(listOf(375L), scheduler.scheduledDelays)

        scheduler.runNext()
        assertEquals(listOf(375L, 55L), scheduler.scheduledDelays)
    }

    private class FakeRepeatScheduler : RepeatScheduler {
        val scheduledDelays = mutableListOf<Long>()
        private val tasks = ArrayDeque<FakeTask>()

        override fun schedule(delayMillis: Long, action: () -> Unit): RepeatTask {
            scheduledDelays += delayMillis
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
