package com.davoyans.alias_ka.nearby

import com.davoyans.alias_ka.domain.*
import org.json.JSONArray
import org.json.JSONObject

object SessionCodec {
 fun encode(s:AliasGameSession)=JSONObject().apply {
  put("id",s.id);put("name",s.gameName);put("state",s.state.name);put("index",s.currentTeamIndex);put("finishing",s.finishing);put("winner",s.winnerTeamId);put("version",s.version);put("seconds",s.settings.roundSeconds);put("target",s.settings.targetScore)
  put("teams",JSONArray().apply{s.teams.forEach{t->put(JSONObject().apply{put("id",t.id);put("name",t.name);put("score",t.score);put("rounds",t.roundsPlayed);put("connected",t.connection is TeamConnection.Connected)})}})
  s.currentRound?.let{r->put("round",JSONObject().apply{put("id",r.roundId);put("team",r.teamId);put("start",r.startedAtMillis);put("end",r.endsAtMillis);put("normalCount",r.normalWordsShownCount);put("current",r.currentWordId);put("selected",r.selectedSkippedWordId);put("phase",r.phase.name);put("page",r.pageNumber);put("used",JSONArray(r.usedWordTexts.toList()));put("words",encodeWords(r.words));put("completed",encodeWords(r.completedWords))})}
  s.protestState?.let{p->put("protest",JSONObject().apply{put("protestId",p.protestId);put("roundId",p.roundId);put("wordId",p.wordId);put("byTeam",p.protestedByTeamId);put("activeTeam",p.activeTeamId);put("status",p.status.name)})}
 }.toString()
 fun decode(raw:String):AliasGameSession { val o=JSONObject(raw);val teams=o.getJSONArray("teams").objects().map{t->Team(t.getString("id"),t.getString("name"),t.getInt("score"),t.getInt("rounds"),if(t.getBoolean("connected"))TeamConnection.Connected("remote","Android device")else TeamConnection.Manual)};val round=o.optJSONObject("round")?.let{r->RoundState(r.getString("id"),r.getString("team"),r.getLong("start"),r.getLong("end"),decodeWords(r.getJSONArray("words")),r.getInt("normalCount"),r.optString("current").ifBlank{null},r.optString("selected").ifBlank{null},RoundPhase.valueOf(r.getString("phase")),decodeWords(r.optJSONArray("completed")?:JSONArray()),(r.optJSONArray("used")?:JSONArray()).strings().toSet(),r.optInt("page",1))};val protest=o.optJSONObject("protest")?.let{p->ProtestState(p.getString("protestId"),p.getString("roundId"),p.getString("wordId"),p.getString("byTeam"),p.getString("activeTeam"),ProtestStatus.valueOf(p.getString("status")))};return AliasGameSession(o.getString("id"),o.getString("name"),teams,GameSettings(o.getInt("seconds"),o.getInt("target")),GameState.valueOf(o.getString("state")),o.getInt("index"),round,o.getBoolean("finishing"),o.optString("winner").ifBlank{null},protest,o.getLong("version")) }
 private fun encodeWords(words:List<RoundWord>)=JSONArray().apply{words.forEach{w->put(JSONObject().apply{put("id",w.id);put("text",w.text);put("status",w.status.name);put("skipped",w.wasSkipped);put("index",w.boardIndex);put("blurred",w.isBlurred);put("active",w.isActive);put("blink",w.shouldBlink);put("blinkCount",w.blinkCount);put("disputed",JSONArray(w.disputedByTeamIds.toList()))})}}
 private fun decodeWords(a:JSONArray)=a.objects().map{w->RoundWord(w.getString("id"),w.getString("text"),WordStatus.valueOf(w.getString("status")),w.getJSONArray("disputed").strings().toSet(),w.optBoolean("skipped"),w.optInt("index"),w.optBoolean("blurred"),w.optBoolean("active"),w.optBoolean("blink"),w.optInt("blinkCount"))}
 private fun JSONArray.objects()=List(length()){getJSONObject(it)}
 private fun JSONArray.strings()=List(length()){getString(it)}
}
