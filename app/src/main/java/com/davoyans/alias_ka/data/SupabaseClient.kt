package com.davoyans.alias_ka.data

import com.davoyans.alias_ka.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AliasKaWord(val id: String?, val date: String?, val word: String, val hint: String?)

fun interface WordFetcher {
    suspend fun fetchWords(lastWord: String?, count: Int): List<AliasKaWord>
}

object SupabaseClient : WordFetcher {
    override suspend fun fetchWords(lastWord: String?, count: Int): List<AliasKaWord> =
        withContext(Dispatchers.IO) {
            val conn = URL("${BuildConfig.SUPABASE_URL}/functions/v1/alias-ka-next-words")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            val body = JSONObject().apply {
                if (lastWord != null) put("last_word", lastWord) else put("last_word", JSONObject.NULL)
                put("count", count)
            }.toString().toByteArray()
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText().orEmpty()
            conn.disconnect()
            if (code !in 200..299) throw Exception("HTTP $code")
            val json = JSONObject(text)
            if (!json.optBoolean("ok", false)) throw Exception(json.optString("error", "sync failed"))
            val arr = json.getJSONArray("words")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                AliasKaWord(
                    o.optString("id").takeIf { it.isNotEmpty() },
                    o.optString("date").takeIf { it.isNotEmpty() },
                    o.getString("word"),
                    o.optString("hint").takeIf { it.isNotEmpty() }
                )
            }
        }
}
