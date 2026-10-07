package dev.turboboo.azookey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDictionarySynchronizerTest {
    @Test
    fun unchangedSnapshotIsAppliedOnlyOnce() {
        var snapshot = """[{"reading":"テスト","word":"試験"}]"""
        val applied = mutableListOf<Pair<String, String>>()
        val synchronizer = UserDictionarySynchronizer(
            snapshot = { snapshot },
            apply = { path, json ->
                applied += path to json
                true
            },
        )

        assertTrue(synchronizer.sync("/dictionary"))
        assertTrue(synchronizer.sync("/dictionary"))
        assertEquals(
            listOf("/dictionary" to snapshot),
            applied,
        )
    }

    @Test
    fun changedSnapshotIsAppliedAgain() {
        var snapshot = "[]"
        var applyCount = 0
        val synchronizer = UserDictionarySynchronizer(
            snapshot = { snapshot },
            apply = { _, _ ->
                applyCount += 1
                true
            },
        )

        assertTrue(synchronizer.sync("/dictionary"))
        snapshot = """[{"reading":"テスト","word":"試験"}]"""
        assertTrue(synchronizer.sync("/dictionary"))

        assertEquals(2, applyCount)
    }

    @Test
    fun failedApplyIsRetried() {
        var attempts = 0
        val synchronizer = UserDictionarySynchronizer(
            snapshot = { "[]" },
            apply = { _, _ ->
                attempts += 1
                attempts > 1
            },
        )

        assertFalse(synchronizer.sync("/dictionary"))
        assertTrue(synchronizer.sync("/dictionary"))
        assertEquals(2, attempts)
    }

    @Test
    fun dictionaryPathChangeForcesResync() {
        var applyCount = 0
        val synchronizer = UserDictionarySynchronizer(
            snapshot = { "[]" },
            apply = { _, _ ->
                applyCount += 1
                true
            },
        )

        assertTrue(synchronizer.sync("/one"))
        assertTrue(synchronizer.sync("/two"))
        assertEquals(2, applyCount)
    }
}
