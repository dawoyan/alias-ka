package com.davoyans.alias_ka.data

import android.content.Context
import com.davoyans.alias_ka.domain.AliasWord
import org.json.JSONArray

interface WordQueueStore {
    var lastDisplayedWord: String?
    var isSyncInProgress: Boolean
    val queueSize: Int
    fun words(): List<AliasWord>
    fun appendWords(incoming: List<AliasKaWord>)
    fun removeWord(text: String)
}

class SharedPrefsWordQueueStore(ctx: Context) : WordQueueStore {
    private val prefs = ctx.getSharedPreferences("supabase_word_queue", Context.MODE_PRIVATE)

    override var lastDisplayedWord: String?
        get() = prefs.getString("last_displayed_word", null)
        set(v) = prefs.edit().run { if (v == null) remove("last_displayed_word") else putString("last_displayed_word", v); apply() }

    override var isSyncInProgress: Boolean
        get() = prefs.getBoolean("is_sync_in_progress", false)
        set(v) = prefs.edit().putBoolean("is_sync_in_progress", v).apply()

    override val queueSize: Int get() = loadRaw().length()

    override fun words(): List<AliasWord> {
        val arr = loadRaw()
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            AliasWord(o.getString("word"), o.optString("hint").takeIf { it.isNotEmpty() })
        }
    }

    override fun appendWords(incoming: List<AliasKaWord>) {
        val arr = loadRaw()
        val existing = HashSet<String>()
        for (i in 0 until arr.length()) existing += arr.getJSONObject(i).getString("word").lowercase()
        val ldw = lastDisplayedWord?.lowercase()
        for (w in incoming) {
            val key = w.word.lowercase()
            if (key !in existing && key != ldw) {
                arr.put(org.json.JSONObject().apply {
                    put("word", w.word)
                    if (w.hint != null) put("hint", w.hint) else put("hint", "")
                })
                existing += key
            }
        }
        prefs.edit().putString("word_queue", arr.toString()).apply()
    }

    override fun removeWord(text: String) {
        val arr = loadRaw()
        val result = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!o.getString("word").equals(text, ignoreCase = true)) result.put(o)
        }
        prefs.edit().putString("word_queue", result.toString()).apply()
    }

    private fun loadRaw(): JSONArray {
        val raw = prefs.getString("word_queue", null) ?: return JSONArray()
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }
}
