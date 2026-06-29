package com.davoyans.alias_ka.data

import com.davoyans.alias_ka.domain.AliasWord
import com.davoyans.alias_ka.domain.BundledWordSetRepository
import com.davoyans.alias_ka.domain.WordSet
import com.davoyans.alias_ka.domain.WordSetRepository
import com.davoyans.alias_ka.domain.WordSetSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SupabaseSyncRepository(
    private val store: WordQueueStore,
    private val fetcher: WordFetcher = SupabaseClient,
    private val scope: CoroutineScope,
    private val onLog: (String) -> Unit = {}
) : WordSetRepository {

    val queueSize: Int get() = store.queueSize

    override fun active(): WordSet {
        val words = store.words().ifEmpty { BundledWordSetRepository().active().words }
        return WordSet(id = "supabase", name = "Alias-ka", words = words, source = WordSetSource.SUPABASE)
    }

    override fun all(): List<WordSet> = listOf(active())

    fun markWordDisplayed(word: String) {
        store.lastDisplayedWord = word
        store.removeWord(word)
    }

    suspend fun ensureWordsAvailableForGame(minNeeded: Int = 150) {
        if (store.queueSize >= minNeeded) return
        if (store.isSyncInProgress) return
        store.isSyncInProgress = true
        try {
            onLog("INFO SUPABASE_SYNC_START lastWord=${store.lastDisplayedWord} count=$minNeeded")
            val words = fetcher.fetchWords(store.lastDisplayedWord, minNeeded)
            store.appendWords(words)
            onLog("INFO SUPABASE_SYNC_DONE received=${words.size} queue=${store.queueSize}")
        } catch (e: Exception) {
            onLog("WARN SUPABASE_SYNC_FAILED ${e.message}")
        } finally {
            store.isSyncInProgress = false
        }
    }

    fun prefetchIfLow(threshold: Int = 50, fetchCount: Int = 200) {
        if (store.queueSize > threshold) return
        if (store.isSyncInProgress) return
        store.isSyncInProgress = true
        scope.launch {
            try {
                onLog("INFO SUPABASE_PREFETCH_START lastWord=${store.lastDisplayedWord} count=$fetchCount")
                val words = fetcher.fetchWords(store.lastDisplayedWord, fetchCount)
                store.appendWords(words)
                onLog("INFO SUPABASE_PREFETCH_DONE received=${words.size} queue=${store.queueSize}")
            } catch (e: Exception) {
                onLog("WARN SUPABASE_PREFETCH_FAILED ${e.message}")
            } finally {
                store.isSyncInProgress = false
            }
        }
    }
}
