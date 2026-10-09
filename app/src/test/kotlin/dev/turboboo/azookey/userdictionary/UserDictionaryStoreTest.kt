package dev.turboboo.azookey.userdictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UserDictionaryStoreTest {
    private lateinit var store: UserDictionaryStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(UserDictionaryStore.PREFERENCES_NAME, 0)
            .edit()
            .clear()
            .commit()
        store = UserDictionaryStore(context)
    }

    @Test
    fun addNormalizesReadingAndPersists() {
        assertTrue(store.add("ゆーざーじしょ", "ユーザー辞書"))

        assertEquals(
            listOf(
                UserDictionaryEntry(
                    reading = "ユーザージショ",
                    word = "ユーザー辞書",
                ),
            ),
            UserDictionaryStore(RuntimeEnvironment.getApplication()).entries(),
        )
    }

    @Test
    fun duplicateEntryIsNotAddedTwice() {
        assertTrue(store.add("てすと", "試験"))
        assertFalse(store.add("テスト", "試験"))

        assertEquals(1, store.entries().size)
    }

    @Test
    fun removeDeletesOnlyRequestedEntry() {
        store.add("えー", "A")
        store.add("びー", "B")

        assertTrue(
            store.remove(
                UserDictionaryEntry(
                    reading = "エー",
                    word = "A",
                ),
            ),
        )

        assertEquals(
            listOf(UserDictionaryEntry(reading = "ビー", word = "B")),
            store.entries(),
        )
    }

    @Test
    fun jsonSnapshotUsesNormalizedWireFormat() {
        store.add("かすたむ", "カスタム")

        assertEquals(
            """[{"reading":"カスタム","word":"カスタム"}]""",
            store.json(),
        )
    }
}
