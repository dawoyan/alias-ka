package com.davoyans.alias_ka.data

import android.content.Context
import com.davoyans.alias_ka.domain.*
import org.json.JSONArray
import org.json.JSONObject

class PersistentWordSetRepository(context:Context):WordSetRepository {
 private val prefs=context.getSharedPreferences("alias-ka-wordsets",0);private val bundled=BundledWordSetRepository().active();private var imported=load().takeLast(1)
 override fun active():WordSet=imported.singleOrNull()?:bundled
 override fun all()=if(imported.isEmpty())listOf(bundled) else imported
 override fun select(id:String){require(all().any{it.id==id});prefs.edit().putString("active",id).apply()}
 override fun import(name:String,csv:String):CsvImportResult {val result=CsvWordSetParser.parse(name,csv);val replacement=replaceImportedWordSet(imported.singleOrNull(),result);if(result.wordSet!=null){imported=listOf(replacement!!);save()};return result}
 override fun rename(id:String,name:String){require(name.isNotBlank());imported=imported.map{if(it.id==id)it.copy(name=name.trim(),updatedAtMillis=System.currentTimeMillis())else it};save()}
 override fun delete(id:String){imported=imported.filterNot{it.id==id};if(prefs.getString("active",null)==id)select(bundled.id);save()}
 private fun save(){prefs.edit().putString("items",JSONArray().apply{imported.forEach{ws->put(JSONObject().apply{put("id",ws.id);put("name",ws.name);put("words",JSONArray().apply{ws.words.forEach{w->put(JSONObject().apply{put("text",w.text);if(w.hint!=null)put("hint",w.hint)})}});put("created",ws.createdAtMillis);put("updated",ws.updatedAtMillis)})}}.toString()).apply()}
 private fun load():List<WordSet> = runCatching{val a=JSONArray(prefs.getString("items","[]"));List(a.length()){i->val o=a.getJSONObject(i);val wa=o.getJSONArray("words");WordSet(o.getString("id"),o.getString("name"),List(wa.length()){j->val item=wa.get(j);if(item is JSONObject)AliasWord(item.getString("text"),item.optString("hint").ifEmpty{null})else AliasWord(item.toString())},WordSetSource.IMPORTED,o.optLong("created"),o.optLong("updated"))}}.getOrDefault(emptyList())
}
