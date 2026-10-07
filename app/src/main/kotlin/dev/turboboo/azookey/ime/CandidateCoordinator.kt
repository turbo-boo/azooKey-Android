package dev.turboboo.azookey.ime

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

fun interface CandidateProvider {
    fun candidates(input: String): List<String>

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
            val candidates = runCatching {
                provider.candidates(input)
            }.getOrDefault(emptyList())

            publishIfCurrent(requestGeneration, candidates)
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
