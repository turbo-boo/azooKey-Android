package dev.turboboo.azookey.ime

import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateCoordinatorTest {
    @Test
    fun requestPublishesProviderCandidates() {
        val published = mutableListOf<List<String>>()
        val coordinator = CandidateCoordinator(
            provider = CandidateProvider { input -> listOf("$input-1", "$input-2") },
            workerExecutor = Executor(Runnable::run),
            mainExecutor = Executor(Runnable::run),
            onCandidates = published::add,
        )

        coordinator.request("かな")

        assertEquals(
            listOf(listOf("かな-1", "かな-2")),
            published,
        )
    }

    @Test
    fun newerRequestSuppressesStaleResult() {
        val worker = QueuedExecutor()
        val main = QueuedExecutor()
        val published = mutableListOf<List<String>>()
        val coordinator = CandidateCoordinator(
            provider = CandidateProvider { input -> listOf(input.uppercase()) },
            workerExecutor = worker,
            mainExecutor = main,
            onCandidates = published::add,
        )

        coordinator.request("old")
        coordinator.request("new")

        worker.runAll()
        main.runAll()

        assertEquals(listOf(listOf("NEW")), published)
    }

    @Test
    fun emptyInputClearsCandidatesWithoutCallingProvider() {
        var providerCalls = 0
        val published = mutableListOf<List<String>>()
        val coordinator = CandidateCoordinator(
            provider = CandidateProvider {
                providerCalls++
                listOf("unexpected")
            },
            workerExecutor = Executor(Runnable::run),
            mainExecutor = Executor(Runnable::run),
            onCandidates = published::add,
        )

        coordinator.request("")

        assertEquals(0, providerCalls)
        assertEquals(listOf(emptyList<String>()), published)
    }

    @Test
    fun emptyInputResetsProviderStateOnWorker() {
        var resets = 0
        val worker = QueuedExecutor()
        val provider = object : CandidateProvider {
            override fun candidates(input: String): List<String> = listOf(input)

            override fun reset() {
                resets++
            }
        }
        val coordinator = CandidateCoordinator(
            provider = provider,
            workerExecutor = worker,
            mainExecutor = Executor(Runnable::run),
            onCandidates = {},
        )

        coordinator.request("")
        assertEquals(0, resets)

        worker.runAll()
        assertEquals(1, resets)
    }

    @Test
    fun completionRunsOnCandidateWorker() {
        val worker = QueuedExecutor()
        val completed = mutableListOf<String>()
        val provider = object : CandidateProvider {
            override fun candidates(input: String): List<String> = emptyList()

            override fun complete(candidate: String) {
                completed += candidate
            }
        }
        val coordinator = CandidateCoordinator(
            provider = provider,
            workerExecutor = worker,
            mainExecutor = Executor(Runnable::run),
            onCandidates = {},
        )

        coordinator.complete("変換")
        assertEquals(emptyList<String>(), completed)

        worker.runAll()
        assertEquals(listOf("変換"), completed)
    }

    @Test
    fun completionAndClearPreserveWorkerOrder() {
        val worker = QueuedExecutor()
        val events = mutableListOf<String>()
        val provider = object : CandidateProvider {
            override fun candidates(input: String): List<String> = emptyList()

            override fun complete(candidate: String) {
                events += "complete:$candidate"
            }

            override fun reset() {
                events += "reset"
            }
        }
        val coordinator = CandidateCoordinator(
            provider = provider,
            workerExecutor = worker,
            mainExecutor = Executor(Runnable::run),
            onCandidates = {},
        )

        coordinator.complete("変換")
        coordinator.clear()
        worker.runAll()

        assertEquals(listOf("complete:変換", "reset"), events)
    }

    @Test
    fun providerFailurePublishesEmptyCandidates() {
        val published = mutableListOf<List<String>>()
        val coordinator = CandidateCoordinator(
            provider = CandidateProvider { error("converter failure") },
            workerExecutor = Executor(Runnable::run),
            mainExecutor = Executor(Runnable::run),
            onCandidates = published::add,
        )

        coordinator.request("かな")

        assertEquals(listOf(emptyList<String>()), published)
    }

    private class QueuedExecutor : Executor {
        private val queue = ArrayDeque<Runnable>()

        override fun execute(command: Runnable) {
            queue.addLast(command)
        }

        fun runAll() {
            while (queue.isNotEmpty()) {
                queue.removeFirst().run()
            }
        }
    }
}
