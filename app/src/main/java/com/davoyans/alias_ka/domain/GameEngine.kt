package com.davoyans.alias_ka.domain

import java.util.UUID

class GameEngine(private val words: WordSetRepository = BundledWordSetRepository()) {
 fun create(gameName:String, ownTeam:String, settings:GameSettings=GameSettings()):AliasGameSession {
  require(ownTeam.isNotBlank())
  return AliasGameSession(UUID.randomUUID().toString(), gameName.ifBlank { "Alias-ka Party" }, listOf(Team(UUID.randomUUID().toString(), ownTeam.trim())), settings=settings)
 }
 fun addTeam(s:AliasGameSession,name:String,connection:TeamConnection=TeamConnection.Manual):AliasGameSession { require(name.isNotBlank()); require(s.teams.none { it.name.equals(name.trim(),true) }); return s.copy(teams=s.teams+Team(UUID.randomUUID().toString(),name.trim(),connection=connection),version=s.version+1) }
 fun rename(s:AliasGameSession,id:String,name:String):AliasGameSession { require(name.isNotBlank()); require(s.teams.none { it.id!=id && it.name.equals(name.trim(),true) }); return s.copy(teams=s.teams.map { if(it.id==id) it.copy(name=name.trim()) else it },version=s.version+1) }
 fun remove(s:AliasGameSession,id:String)=s.copy(teams=s.teams.filterNot{it.id==id},currentTeamIndex=0,version=s.version+1)
 fun move(s:AliasGameSession,index:Int,delta:Int):AliasGameSession { val to=(index+delta).coerceIn(s.teams.indices); val list=s.teams.toMutableList(); val t=list.removeAt(index); list.add(to,t); return s.copy(teams=list,version=s.version+1) }
 fun startRound(s:AliasGameSession,now:Long):AliasGameSession {require(s.teams.size>=2);val (board,used)=createBoard(emptySet());val round=RoundState(UUID.randomUUID().toString(),s.teams[s.currentTeamIndex].id,now,now+s.settings.roundSeconds*1000L,board,currentWordId=board[0].id,usedWordTexts=used);return s.copy(state=GameState.ROUND_RUNNING,currentRound=round,protestState=null,version=s.version+1)}
 fun correct(s:AliasGameSession):AliasGameSession {val r=s.currentRound?:return s;val id=r.currentWordId?:r.selectedSkippedWordId?:return s;val word=r.words.firstOrNull{it.id==id}?:return s;if(word.status==WordStatus.CORRECT){val sanitized=r.words.map{if(it.id==id)it.copy(isActive=false,shouldBlink=false)else it};return s.copy(currentRound=r.copy(words=sanitized,currentWordId=sanitized.firstOrNull{it.isActive&&it.status!=WordStatus.CORRECT}?.id))};if(!word.isActive)return s;val marked=r.copy(words=r.words.map{if(it.id==id)it.copy(status=WordStatus.CORRECT,isActive=false,shouldBlink=false,isBlurred=false)else it});val next=if(r.phase==RoundPhase.NORMAL_PASS)advanceNormal(marked)else if(marked.words.all{it.status==WordStatus.CORRECT})loadNextPage(marked)else resumeReplay(marked);return s.copy(teams=score(s,r.teamId,1),currentRound=next,version=s.version+1)}
 fun skip(s:AliasGameSession):AliasGameSession {val r=s.currentRound?:return s;val id=r.currentWordId?:r.selectedSkippedWordId?:return s;val word=r.words.firstOrNull{it.id==id}?:return s;if(!word.isActive||word.status==WordStatus.CORRECT)return s;val marked=r.copy(words=r.words.map{if(it.id==id)it.copy(status=WordStatus.SKIPPED,wasSkipped=true,isBlurred=false,isActive=false,shouldBlink=false)else it});return s.copy(currentRound=if(r.phase==RoundPhase.NORMAL_PASS)advanceNormal(marked)else resumeReplay(marked),version=s.version+1)}
 fun selectSkipped(s:AliasGameSession,wordId:String):AliasGameSession {val r=s.currentRound!!;require(r.phase==RoundPhase.UNGUESSED_REPLAY&&r.words.any{it.id==wordId&&it.status==WordStatus.SKIPPED});return s.copy(currentRound=r.copy(currentWordId=wordId,selectedSkippedWordId=wordId,words=r.words.map{if(it.id==wordId)it.copy(isActive=true,shouldBlink=true,blinkCount=2)else it.copy(isActive=false,shouldBlink=false)}),version=s.version+1)}
 fun guessedSkipped(s:AliasGameSession)=correct(s)
 fun protest(s:AliasGameSession,wordId:String,byTeamId:String):AliasGameSession {
  val r=s.currentRound?:return s
  if(r.words.none{it.id==wordId&&it.status==WordStatus.CORRECT})return s
  if(byTeamId==r.teamId)return s
  if(s.protestState!=null)return s
  return s.copy(protestState=ProtestState(UUID.randomUUID().toString(),r.roundId,wordId,byTeamId,r.teamId),version=s.version+1)
 }
 fun admitProtest(s:AliasGameSession):AliasGameSession {
  val p=s.protestState?:return s
  val r=s.currentRound?:return s
  if(r.words.none{it.id==p.wordId&&it.status==WordStatus.CORRECT})return s.copy(protestState=null,version=s.version+1)
  return s.copy(teams=score(s,r.teamId,-1),currentRound=r.copy(words=r.words.map{if(it.id==p.wordId)it.copy(status=WordStatus.FINAL_WRONG)else it}),protestState=null,version=s.version+1)
 }
 fun rejectProtest(s:AliasGameSession):AliasGameSession {
  if(s.protestState==null)return s
  return s.copy(protestState=null,version=s.version+1)
 }
 fun endRound(s:AliasGameSession):AliasGameSession {val r=s.currentRound!!;val all=(r.completedWords+r.words).map{if(it.status==WordStatus.SHOWN||it.status==WordStatus.SKIPPED)it.copy(status=WordStatus.NOT_COUNTED,isActive=false,shouldBlink=false)else it.copy(isActive=false,shouldBlink=false)};return s.copy(state=GameState.ROUND_REVIEW,teams=s.teams.map{if(it.id==r.teamId)it.copy(roundsPlayed=it.roundsPlayed+1)else it},currentRound=r.copy(currentWordId=null,selectedSkippedWordId=null,words=all,completedWords=emptyList()),protestState=null,version=s.version+1)}
 fun next(s:AliasGameSession):AliasGameSession { val next=(s.currentTeamIndex+1)%s.teams.size; val reached=s.finishing||s.teams.any{it.score>=s.settings.targetScore}; val equal=s.teams.map{it.roundsPlayed}.distinct().size==1; val leader=s.teams.sortedByDescending{it.score}; val winner=if(reached&&equal&&leader.size>1&&leader[0].score>leader[1].score)leader[0].id else null; return s.copy(currentTeamIndex=next,currentRound=null,finishing=reached,winnerTeamId=winner,protestState=null,state=if(winner!=null)GameState.GAME_OVER else if(reached)GameState.FINISHING_CURRENT_CYCLE else GameState.LOBBY,version=s.version+1) }
 fun isActiveTeamController(s:AliasGameSession,ownTeamId:String?,isHost:Boolean):Boolean {
  val r=s.currentRound?:return false
  val activeTeamIsManual=s.teams.firstOrNull{it.id==r.teamId}?.connection is TeamConnection.Manual
  return r.teamId==ownTeamId||(isHost&&activeTeamIsManual)
 }
 private fun score(s:AliasGameSession,teamId:String,delta:Int)=s.teams.map{if(it.id==teamId)it.copy(score=(it.score+delta).coerceAtLeast(0))else it}
 private fun advanceNormal(r:RoundState):RoundState {if(r.normalWordsShownCount>=10)return if(r.words.any{it.status==WordStatus.SKIPPED})resumeReplay(r)else loadNextPage(r);val nextIndex=r.normalWordsShownCount;val next=r.words[nextIndex];return r.copy(normalWordsShownCount=nextIndex+1,currentWordId=next.id,words=r.words.map{if(it.id==next.id)it.copy(isBlurred=false,isActive=true,shouldBlink=true,blinkCount=2)else it.copy(isActive=false,shouldBlink=false)})}
 private fun resumeReplay(r:RoundState)=r.copy(phase=RoundPhase.UNGUESSED_REPLAY,currentWordId=null,selectedSkippedWordId=null,words=r.words.map{it.copy(isActive=false,shouldBlink=it.status==WordStatus.SKIPPED)})
 private fun loadNextPage(r:RoundState):RoundState {val(board,used)=createBoard(r.usedWordTexts);return r.copy(words=board,normalWordsShownCount=1,currentWordId=board[0].id,selectedSkippedWordId=null,phase=RoundPhase.NORMAL_PASS,completedWords=r.completedWords+r.words,usedWordTexts=used,pageNumber=r.pageNumber+1)}
 fun lookupHint(wordText:String):String?=words.active().words.firstOrNull{it.text==wordText}?.hint
 private fun createBoard(previouslyUsed:Set<String>):Pair<List<RoundWord>,Set<String>> {val source=words.active().words;val available=source.filter{it.text.lowercase() !in previouslyUsed}.shuffled();val selected=mutableListOf<AliasWord>();selected+=available.take(10);if(selected.size<10){val refill=source.shuffled();var i=0;while(selected.size<10){selected+=refill[i%refill.size];i++}};val board=selected.mapIndexed{i,w->RoundWord(UUID.randomUUID().toString(),w.text,boardIndex=i)}.toMutableList();board[0]=board[0].copy(isBlurred=false,isActive=true,shouldBlink=true,blinkCount=2);return board to (previouslyUsed+selected.map{it.text.lowercase()})}
}
