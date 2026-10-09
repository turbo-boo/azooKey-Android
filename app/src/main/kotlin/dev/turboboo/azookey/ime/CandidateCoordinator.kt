package dev.turboboo.azookey.ime

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

fun interface CandidateProvider {
    fun candidates(input: String): List<String>

    fun complete(candidate: String) {}

    fun reset() {}
}

class CandidateCoordinator(
    private val provider: CandidateProvider,
    private val workerExecutor: Executor,
    private val mainExecutor: Executor,
    private val onCandidates: (List<String>) -> Unit,
) {
    private val generation = AtomicLong()

    fun request(input: String) {
        val requestGeneration = generation.incrementAndGet()

        if (input.isEmpty()) {
            workerExecutor.execute {
                runCatching(provider::reset)
            }
            publishIfCurrent(requestGeneration, emptyList())
            return
        }

        workerExecutor.execute {
            // Typing can outrun the native converter. Do not spend native work
            // on queued generations whose results cannot be displayed.
            if (generation.get() != requestGeneration) {
                return@execute
            }
            val candidates = runCatching {
                provider.candidates(input)
            }.getOrDefault(emptyList())

            publishIfCurrent(requestGeneration, candidates)
        }
    }

    fun complete(candidate: String) {
        workerExecutor.execute {
            runCatching {
                provider.complete(candidate)
            }
        }
    }

    fun clear() {
        request("")
    }

    private fun publishIfCurrent(
        requestGeneration: Long,
        candidates: List<String>,
    ) {
        mainExecutor.execute {
            if (generation.get() == requestGeneration) {
                onCandidates(candidates)
            }
        }
    }
}
