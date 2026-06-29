package com.davoyans.alias_ka

import com.davoyans.alias_ka.data.AliasKaWord
import com.davoyans.alias_ka.data.SupabaseSyncRepository
import com.davoyans.alias_ka.data.WordFetcher
import com.davoyans.alias_ka.data.WordQueueStore
import com.davoyans.alias_ka.domain.AliasWord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseSyncTest {

    private lateinit var store: InMemoryWordQueueStore
    private var fetchCallCount = 0

    private val fakeFetcher = WordFetcher { _, count ->
        fetchCallCount++
        (1..count).map { AliasKaWord("id$it", null, "word$it", null) }
    }

    @Before fun setUp() {
        store = InMemoryWordQueueStore()
        fetchCallCount = 0
    }

    // Test 1: opening the app does not trigger any fetch
    @Test fun `app open does not fetch when user does not play`() = runTest {
        SupabaseSyncRepository(store, fakeFetcher, this)
        // no ensureWordsAvailableForGame call → no fetch
        advanceUntilIdle()
        assertEquals(0, fetchCallCount)
    }

    // Test 2: first game start with empty queue fetches 150 words
    @Test fun `first game with empty queue fetches 150 words`() = runTest {
        val repo = SupabaseSyncRepository(store, fakeFetcher, this)
        repo.ensureWordsAvailableForGame(minNeeded = 150)
        assertEquals(1, fetchCallCount)
        assertEquals(150, store.queueSize)
    }

    // Test 3: marking word displayed updates cursor and removes word from queue
    @Test fun `marking word displayed updates lastDisplayedWord and removes from queue`() = runTest {
        val repo = SupabaseSyncRepository(store, fakeFetcher, this)
        repo.ensureWordsAvailableForGame(minNeeded = 5)
        val firstWord = store.words().first().text
        repo.markWordDisplayed(firstWord)
        assertEquals(firstWord, store.lastDisplayedWord)
        assertEquals(4, store.queueSize)
        assertTrue(store.words().none { it.text == firstWord })
    }

    // Test 4: when 50 words remain, prefetch fires; StandardTestDispatcher makes launch lazy
    @Test fun `when 50 words remain prefetch fetches more`() = runTest {
        repeat(50) { store.appendWords(listOf(AliasKaWord(null, null, "initial$it", null))) }
        val repo = SupabaseSyncRepository(store, fakeFetcher, this)
        repo.prefetchIfLow(threshold = 50, fetchCount = 10)
        advanceUntilIdle()
        assertEquals(1, fetchCallCount)
        assertTrue("queue should grow beyond 50", store.queueSize > 50)
    }

    // Test 5: second prefetchIfLow call is blocked by isSyncInProgress guard
    @Test fun `concurrent prefetch only makes one request`() = runTest {
        val repo = SupabaseSyncRepository(store, fakeFetcher, this)
        // StandardTestDispatcher keeps launch lazy → both calls run before coroutine starts
        repo.prefetchIfLow(threshold = 100, fetchCount = 10) // sets isSyncInProgress=true, enqueues
        repo.prefetchIfLow(threshold = 100, fetchCount = 10) // isSyncInProgress=true → no-op
        advanceUntilIdle()                                    // now runs the single queued coroutine
        assertEquals(1, fetchCallCount)
    }

    // Test 6: failed sync leaves existing queue unchanged
    @Test fun `failed sync keeps existing queue`() = runTest {
        repeat(30) { store.appendWords(listOf(AliasKaWord(null, null, "existing$it", null))) }
        val failingFetcher = WordFetcher { _, _ -> throw Exception("Network error") }
        val repo = SupabaseSyncRepository(store, failingFetcher, this)
        repo.ensureWordsAvailableForGame(minNeeded = 150)
        assertEquals(30, store.queueSize)
        assertEquals(0, fetchCallCount)
    }
}

private class InMemoryWordQueueStore : WordQueueStore {
    override var lastDisplayedWord: String? = null
    override var isSyncInProgress: Boolean = false
    private val _queue = mutableListOf<AliasWord>()
    override val queueSize: Int get() = _queue.size
    override fun words(): List<AliasWord> = _queue.toList()
    override fun appendWords(incoming: List<AliasKaWord>) {
        val existing = _queue.map { it.text.lowercase() }.toHashSet()
        val ldw = lastDisplayedWord?.lowercase()
        incoming.filter { it.word.lowercase() !in existing && it.word.lowercase() != ldw }
            .forEach { _queue.add(AliasWord(it.word, it.hint)) }
    }
    override fun removeWord(text: String) {
        _queue.removeAll { it.text.equals(text, ignoreCase = true) }
    }
}
