package com.davoyans.alias_ka.domain

enum class WordSetSource { BUNDLED, IMPORTED }
data class AliasWord(val text: String, val hint: String? = null)
data class WordSet(val id: String, val name: String, val words: List<AliasWord>, val source: WordSetSource, val createdAtMillis:Long=0, val updatedAtMillis:Long=0)
data class CsvImportResult(val wordSet:WordSet?,val importedCount:Int,val duplicateCount:Int,val emptyCount:Int,val error:String?=null)
enum class WordStatus { SHOWN, CORRECT, SKIPPED, DISPUTED, FINAL_CORRECT, FINAL_WRONG, NOT_COUNTED }
enum class RoundPhase { NORMAL_PASS, UNGUESSED_REPLAY }
enum class GameState { LOBBY, ROUND_RUNNING, ROUND_REVIEW, FINISHING_CURRENT_CYCLE, GAME_OVER, PAUSED_HOST_DISCONNECTED }
enum class ProtestStatus { ACTIVE, ADMITTED_WRONG, CANCELLED }
data class ProtestState(val protestId:String, val roundId:String, val wordId:String, val protestedByTeamId:String, val activeTeamId:String, val status:ProtestStatus=ProtestStatus.ACTIVE)
sealed interface TeamConnection { data object Manual : TeamConnection; data class Connected(val endpointId:String, val deviceName:String, val isConnected:Boolean=true):TeamConnection }
data class Team(val id:String, val name:String, val score:Int=0, val roundsPlayed:Int=0, val connection:TeamConnection=TeamConnection.Manual)
data class RoundWord(val id:String,val text:String,val status:WordStatus=WordStatus.SHOWN,val disputedByTeamIds:Set<String> = emptySet(),val wasSkipped:Boolean=false,val boardIndex:Int=0,val isBlurred:Boolean=true,val isActive:Boolean=false,val shouldBlink:Boolean=false,val blinkCount:Int=0)
data class RoundState(val roundId:String,val teamId:String,val startedAtMillis:Long,val endsAtMillis:Long,val words:List<RoundWord>,val normalWordsShownCount:Int=1,val currentWordId:String?=null,val selectedSkippedWordId:String?=null,val phase:RoundPhase=RoundPhase.NORMAL_PASS,val completedWords:List<RoundWord> = emptyList(),val usedWordTexts:Set<String> = emptySet(),val pageNumber:Int=1)
data class GameSettings(val roundSeconds:Int=60, val targetScore:Int=50)
data class AliasGameSession(val id:String, val gameName:String, val teams:List<Team>, val settings:GameSettings=GameSettings(), val state:GameState=GameState.LOBBY, val currentTeamIndex:Int=0, val currentRound:RoundState?=null, val finishing:Boolean=false, val winnerTeamId:String?=null, val protestState:ProtestState?=null, val version:Long=0)

interface WordSetRepository { fun active(): WordSet; fun all():List<WordSet> = listOf(active()); fun select(id:String)=Unit; fun import(name:String,csv:String)=CsvImportResult(null,0,0,0,"Import unsupported"); fun rename(id:String,name:String)=Unit; fun delete(id:String)=Unit }
class BundledWordSetRepository : WordSetRepository {
 override fun active() = WordSet("starter", "Alias-ka Starter", listOf("արև","գիրք","Երևան","ընկեր","տոն","կինո","ժպիտ","լեռ","սուրճ","երաժշտություն","дом","море","праздник","друг","театр","машина","улыбка","подарок","солнце","книга","apple","garden","music","family","party","mountain","camera","coffee","journey","rainbow","bridge","football","planet","kitchen","doctor","school","dance","river","summer","winter","telephone","window","market","artist","airport","bicycle","chocolate","museum","village","dragon","castle","island","forest","clock","piano","train","moon","star","cloud","flower").map{AliasWord(it)}, WordSetSource.BUNDLED)
}

object CsvWordSetParser {
 fun parse(name:String,csv:String,now:Long=System.currentTimeMillis()):CsvImportResult {
  var empty=0;var duplicates=0;val seen=linkedSetOf<String>();val words=mutableListOf<AliasWord>()
  val lines=csv.removePrefix("﻿").lines().toMutableList()
  val firstRow=lines.firstOrNull()?.trim()?.lowercase()
  val hasHints=firstRow?.startsWith("word,hint")==true||firstRow?.startsWith("word\thint")==true
  val hasWordHeader=firstRow?.equals("word",true)==true||hasHints
  if(hasWordHeader)lines.removeAt(0)
  val sep=if(hasHints&&(firstRow?.contains(',')!=false)) ',' else '\t'
  lines.forEach{raw->
   val cols=raw.split(sep,limit=2)
   val word=cols[0].trim().removeSurrounding("\"")
   val hint=if(hasHints&&cols.size>1)cols[1].trim().removeSurrounding("\"").takeIf{it.isNotEmpty()}else null
   if(word.isEmpty())empty++ else if(!seen.add(word.lowercase()))duplicates++ else words+=AliasWord(word,hint)
  }
  if(words.isEmpty())return CsvImportResult(null,0,duplicates,empty,"No words found")
  return CsvImportResult(WordSet("imported-$now",name.ifBlank{"Imported words"},words,WordSetSource.IMPORTED,now,now),words.size,duplicates,empty)
 }
}
fun replaceImportedWordSet(previous:WordSet?,result:CsvImportResult):WordSet? = result.wordSet ?: previous
