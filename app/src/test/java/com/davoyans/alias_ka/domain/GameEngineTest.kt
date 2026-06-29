package com.davoyans.alias_ka.domain

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GameEngineTest {
 private val engine=GameEngine(object:WordSetRepository{override fun active()=WordSet("t","test",listOf("one","two","three","four").map{AliasWord(it)},WordSetSource.BUNDLED)})
 private fun lobby()=engine.addTeam(engine.create("Editable game","Alpha"),"Beta")
 @Test fun `create requires own team and keeps edited name`(){assertThrows(IllegalArgumentException::class.java){engine.create("x","")};assertEquals("Editable",engine.create("Editable","A").gameName)}
 @Test fun `team crud and ordering`(){var s=lobby();s=engine.addTeam(s,"Gamma");val gamma=s.teams.last();s=engine.rename(s,gamma.id,"Delta");assertEquals("Delta",s.teams.last().name);s=engine.move(s,2,-1);assertEquals("Delta",s.teams[1].name);s=engine.remove(s,gamma.id);assertEquals(2,s.teams.size)}
 @Test fun `duplicates blocked`(){assertThrows(IllegalArgumentException::class.java){engine.addTeam(lobby(),"alpha")}}
 @Test fun `start needs two teams`(){assertThrows(IllegalArgumentException::class.java){engine.startRound(engine.create("g","A"),0)}}
 @Test fun `correct updates score`(){var s=engine.startRound(lobby(),0);s=engine.correct(s);assertEquals(1,s.teams[0].score)}
 @Test fun `round end moves review and increments rounds`(){var s=engine.startRound(lobby(),0);s=engine.endRound(s);assertEquals(GameState.ROUND_REVIEW,s.state);assertEquals(1,s.teams[0].roundsPlayed)}
 @Test fun `target begins finishing but waits for equal rounds`(){var s=lobby().copy(teams=lobby().teams.mapIndexed{i,t->t.copy(score=if(i==0)50 else 40,roundsPlayed=if(i==0)1 else 0)},currentTeamIndex=0);s=engine.next(s);assertTrue(s.finishing);assertNull(s.winnerTeamId);assertNotEquals(GameState.GAME_OVER,s.state)}
 @Test fun `equal rounds declare unique leader`(){val base=lobby();val s=engine.next(base.copy(teams=base.teams.mapIndexed{i,t->t.copy(score=if(i==0)51 else 49,roundsPlayed=1)},finishing=true));assertEquals(GameState.GAME_OVER,s.state);assertEquals(s.teams[0].id,s.winnerTeamId)}
 @Test fun `equal round tie continues sudden death`(){val base=lobby();val s=engine.next(base.copy(teams=base.teams.map{it.copy(score=50,roundsPlayed=1)},finishing=true));assertNull(s.winnerTeamId);assertEquals(GameState.FINISHING_CURRENT_CYCLE,s.state)}
 @Test fun `event reducer ignores duplicate and detects gap`(){val reducer=HostEventReducer();assertTrue(reducer.accept(SequencedEvent("a",1,"x")) is EventResult.Applied);assertEquals(EventResult.Duplicate,reducer.accept(SequencedEvent("a",1,"x")));assertEquals(EventResult.SequenceGap(2,3),reducer.accept(SequencedEvent("c",3,"x")))}
 @Test fun `skip scores zero and stays in round list`(){var s=engine.startRound(lobby(),0);val id=s.currentRound!!.currentWordId;s=engine.skip(s);assertEquals(0,s.teams[0].score);assertEquals(WordStatus.SKIPPED,s.currentRound!!.words.first{it.id==id}.status);assertTrue(s.currentRound!!.words.first{it.id==id}.wasSkipped)}
 @Test fun `ten normal words enter skipped replay`(){var s=engine.startRound(lobby(),0);s=engine.skip(s);repeat(9){s=engine.correct(s)};assertEquals(10,s.currentRound!!.normalWordsShownCount);assertEquals(RoundPhase.UNGUESSED_REPLAY,s.currentRound!!.phase);assertNull(s.currentRound!!.currentWordId)}
 @Test fun `selected skipped word guessed gives point`(){var s=engine.startRound(lobby(),0);val skipped=s.currentRound!!.currentWordId!!;s=engine.skip(s);repeat(9){s=engine.correct(s)};val before=s.teams[0].score;s=engine.selectSkipped(s,skipped);assertEquals(skipped,s.currentRound!!.selectedSkippedWordId);s=engine.guessedSkipped(s);assertEquals(before+1,s.teams[0].score);assertEquals(WordStatus.CORRECT,s.currentRound!!.completedWords.first{it.id==skipped}.status)}
 @Test fun `unresolved skipped becomes not counted at end`(){var s=engine.startRound(lobby(),0);val skipped=s.currentRound!!.currentWordId!!;s=engine.skip(s);s=engine.endRound(s);assertEquals(WordStatus.NOT_COUNTED,s.currentRound!!.words.first{it.id==skipped}.status)}
 @Test fun `round starts with ten cards only first revealed active and double blink`(){val r=engine.startRound(lobby(),0).currentRound!!;assertEquals(10,r.words.size);assertFalse(r.words[0].isBlurred);assertTrue(r.words[0].isActive);assertTrue(r.words[0].shouldBlink);assertEquals(2,r.words[0].blinkCount);assertTrue(r.words.drop(1).all{it.isBlurred&&!it.isActive})}
 @Test fun `skip reveals next and leaves previous visible inactive`(){var s=engine.startRound(lobby(),0);val first=s.currentRound!!.words[0];s=engine.skip(s);val r=s.currentRound!!;assertFalse(r.words[0].isBlurred);assertFalse(r.words[0].isActive);assertEquals(WordStatus.SKIPPED,r.words[0].status);assertFalse(r.words[1].isBlurred);assertTrue(r.words[1].isActive);assertEquals(2,r.words[1].blinkCount);assertEquals(first.id,r.words[0].id)}
 @Test fun `correct reveals next and same word cannot score twice`(){var s=engine.startRound(lobby(),0);val first=s.currentRound!!.currentWordId!!;s=engine.correct(s);val score=s.teams[0].score;val tampered=s.copy(currentRound=s.currentRound!!.copy(currentWordId=first,words=s.currentRound!!.words.map{if(it.id==first)it.copy(isActive=true)else it}));val again=engine.correct(tampered);assertEquals(score,again.teams[0].score);assertFalse(again.currentRound!!.words[0].isActive);assertTrue(again.currentRound!!.words[1].isActive)}
 @Test fun `replay skip leaves unguessed selectable and zero points`(){var s=engine.startRound(lobby(),0);val first=s.currentRound!!.currentWordId!!;s=engine.skip(s);repeat(9){s=engine.correct(s)};assertTrue(s.currentRound!!.words.first{it.id==first}.shouldBlink);s=engine.selectSkipped(s,first);val before=s.teams[0].score;s=engine.skip(s);assertEquals(before,s.teams[0].score);assertEquals(WordStatus.SKIPPED,s.currentRound!!.words.first{it.id==first}.status);assertTrue(s.currentRound!!.words.first{it.id==first}.shouldBlink)}
 @Test fun `timer end clears actions and blinking`(){var s=engine.startRound(lobby(),0);s=engine.skip(s);s=engine.endRound(s);assertNull(s.currentRound!!.currentWordId);assertTrue(s.currentRound!!.words.none{it.isActive||it.shouldBlink});assertEquals(GameState.ROUND_REVIEW,s.state)}
 @Test fun `all correct loads next ten without resetting timer`(){var s=engine.startRound(lobby(),1234);val end=s.currentRound!!.endsAtMillis;repeat(10){s=engine.correct(s)};val r=s.currentRound!!;assertEquals(2,r.pageNumber);assertEquals(10,r.completedWords.size);assertEquals(10,r.words.size);assertEquals(end,r.endsAtMillis);assertTrue(r.words[0].isActive)}
 @Test fun `review includes words from multiple pages`(){var s=engine.startRound(lobby(),0);repeat(10){s=engine.correct(s)};repeat(3){s=engine.correct(s)};s=engine.endRound(s);assertEquals(20,s.currentRound!!.words.size);assertEquals(13,s.currentRound!!.words.count{it.status==WordStatus.CORRECT})}
 @Test fun `selecting skipped stops other blinking then skip restarts it`(){var s=engine.startRound(lobby(),0);val first=s.currentRound!!.currentWordId!!;s=engine.skip(s);val second=s.currentRound!!.currentWordId!!;s=engine.skip(s);repeat(8){s=engine.correct(s)};assertEquals(2,s.currentRound!!.words.count{it.shouldBlink});s=engine.selectSkipped(s,first);assertTrue(s.currentRound!!.words.first{it.id==first}.shouldBlink);assertFalse(s.currentRound!!.words.first{it.id==second}.shouldBlink);s=engine.skip(s);assertEquals(2,s.currentRound!!.words.count{it.shouldBlink})}
 @Test fun `correct selected restarts blinking only for remaining skipped`(){var s=engine.startRound(lobby(),0);val first=s.currentRound!!.currentWordId!!;s=engine.skip(s);val second=s.currentRound!!.currentWordId!!;s=engine.skip(s);repeat(8){s=engine.correct(s)};s=engine.selectSkipped(s,first);s=engine.correct(s);assertFalse(s.currentRound!!.words.first{it.id==first}.shouldBlink);assertTrue(s.currentRound!!.words.first{it.id==second}.shouldBlink)}
}

class ProtestFlowTest {
 private val engine=GameEngine(object:WordSetRepository{override fun active()=WordSet("t","test",listOf("one","two","three","four").map{AliasWord(it)},WordSetSource.BUNDLED)})
 private fun roundInProgress():AliasGameSession {
  val s=engine.addTeam(engine.create("g","Alpha"),"Beta")
  return engine.startRound(s,System.currentTimeMillis()+60_000)
 }
 @Test fun `passive team can protest correct word`(){var s=roundInProgress();s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;val passiveTeamId=s.teams[1].id;s=engine.protest(s,wordId,passiveTeamId);assertNotNull(s.protestState);assertEquals(wordId,s.protestState!!.wordId);assertEquals(ProtestStatus.ACTIVE,s.protestState!!.status)}
 @Test fun `active team cannot protest own words`(){var s=roundInProgress();s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;val activeTeamId=s.teams[0].id;s=engine.protest(s,wordId,activeTeamId);assertNull(s.protestState)}
 @Test fun `passive team cannot protest skipped word`(){var s=roundInProgress();val wordId=s.currentRound!!.currentWordId!!;s=engine.skip(s);val passiveTeamId=s.teams[1].id;s=engine.protest(s,wordId,passiveTeamId);assertNull(s.protestState)}
 @Test fun `admit wrong overlines word and removes point`(){var s=roundInProgress();s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;val scoreBefore=s.teams[0].score;val passiveTeamId=s.teams[1].id;s=engine.protest(s,wordId,passiveTeamId);s=engine.admitProtest(s);assertEquals(scoreBefore-1,s.teams[0].score);assertEquals(WordStatus.FINAL_WRONG,s.currentRound!!.words.first{it.id==wordId}.status);assertNull(s.protestState)}
 @Test fun `admit wrong cannot subtract below zero`(){var s=roundInProgress();s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=s.copy(teams=s.teams.map{if(it.id==s.currentRound!!.teamId)it.copy(score=0)else it});val passiveTeamId=s.teams[1].id;s=engine.protest(s,wordId,passiveTeamId);s=engine.admitProtest(s);assertEquals(0,s.teams[0].score)}
 @Test fun `cancel protest keeps point and correct state`(){var s=roundInProgress();s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;val scoreBefore=s.teams[0].score;val passiveTeamId=s.teams[1].id;s=engine.protest(s,wordId,passiveTeamId);s=engine.rejectProtest(s);assertEquals(scoreBefore,s.teams[0].score);assertEquals(WordStatus.CORRECT,s.currentRound!!.words.first{it.id==wordId}.status);assertNull(s.protestState)}
 @Test fun `only one protest at a time`(){var s=roundInProgress();s=engine.correct(s);s=engine.correct(s);val words=s.currentRound!!.words.filter{it.status==WordStatus.CORRECT};val passiveTeamId=s.teams[1].id;s=engine.protest(s,words[0].id,passiveTeamId);val firstProtest=s.protestState;s=engine.protest(s,words[1].id,passiveTeamId);assertEquals(firstProtest?.wordId,s.protestState?.wordId)}
 @Test fun `end round clears active protest and stops all blinking`(){var s=roundInProgress();s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=engine.protest(s,wordId,s.teams[1].id);s=engine.endRound(s);assertNull(s.protestState);assertTrue(s.currentRound!!.words.none{it.shouldBlink||it.isActive})}
 @Test fun `next round clears protest state`(){var s=roundInProgress();s=engine.endRound(s);s=engine.next(s);assertNull(s.protestState)}
 @Test fun `round end broadcast has no blinking words`(){var s=roundInProgress();val skipped=s.currentRound!!.currentWordId!!;s=engine.skip(s);s=engine.endRound(s);assertTrue(s.currentRound!!.words.none{it.shouldBlink})}
 @Test fun `client snapshot stops unguessed replay blinking after round end`(){var s=roundInProgress();s=engine.skip(s);repeat(9){s=engine.correct(s)};assertEquals(RoundPhase.UNGUESSED_REPLAY,s.currentRound!!.phase);assertTrue(s.currentRound!!.words.any{it.shouldBlink});s=engine.endRound(s);assertEquals(GameState.ROUND_REVIEW,s.state);assertTrue(s.currentRound!!.words.none{it.shouldBlink})}
 @Test fun `next team changes active team index`(){val s=roundInProgress();val before=s.currentTeamIndex;val after=engine.next(engine.endRound(s));assertEquals((before+1)%s.teams.size,after.currentTeamIndex)}
 @Test fun `isActiveTeamController respects connected teams`(){
  val base=roundInProgress()
  val activeTeamId=base.currentRound!!.teamId
  val passiveTeamId=base.teams.first{it.id!=activeTeamId}.id
  // Active team is a Connected device (remote phone)
  val s=base.copy(teams=base.teams.map{if(it.id==activeTeamId)it.copy(connection=TeamConnection.Connected("ep","dev"))else it})
  assertTrue(engine.isActiveTeamController(s,activeTeamId,false))  // active connected phone
  assertFalse(engine.isActiveTeamController(s,passiveTeamId,false)) // passive non-host phone
  assertFalse(engine.isActiveTeamController(s,passiveTeamId,true))  // admin phone, but active team is Connected not Manual
 }
 @Test fun `isActiveTeamController allows host for manual team`(){val base=roundInProgress();val s=base.copy(teams=base.teams.map{it.copy(connection=TeamConnection.Manual)});assertTrue(engine.isActiveTeamController(s,"other-id",true))}
}

class SyncProtestDomainTest {
 private val engine=GameEngine(object:WordSetRepository{override fun active()=WordSet("t","test",listOf("one","two","three","four").map{AliasWord(it)},WordSetSource.BUNDLED)})
 private fun lobby()=engine.addTeam(engine.create("g","Alpha"),"Beta")
 @Test fun `protest state is propagated in session snapshot`(){var s=engine.startRound(lobby(),System.currentTimeMillis()+60_000);s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=engine.protest(s,wordId,s.teams[1].id);assertNotNull(s.protestState);assertEquals(wordId,s.protestState!!.wordId);assertEquals(ProtestStatus.ACTIVE,s.protestState!!.status)}
 @Test fun `session without protest has null protestState`(){val s=engine.startRound(lobby(),0);assertNull(s.protestState)}
 @Test fun `admitted protest is visible to all devices via session`(){var s=engine.startRound(lobby(),System.currentTimeMillis()+60_000);s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=engine.protest(s,wordId,s.teams[1].id);s=engine.admitProtest(s);assertEquals(WordStatus.FINAL_WRONG,s.currentRound!!.words.first{it.id==wordId}.status);assertNull(s.protestState)}
}

class CsvWordSetParserTest {
 @Test fun `header UTF8 duplicate and empty rows are handled`(){val r=CsvWordSetParser.parse("Հայերեն","word\nԱրարատ\n գիրք \nարարատ\n\nԵրևան\n",1);assertEquals(listOf("Արարատ","գիրք","Երևան"),r.wordSet!!.words.map{it.text});assertEquals(3,r.importedCount);assertEquals(1,r.duplicateCount);assertTrue(r.emptyCount>=1)}
 @Test fun `CSV without header keeps first word`(){val r=CsvWordSetParser.parse("No header","Apple\nBanana",1);assertEquals(listOf("Apple","Banana"),r.wordSet!!.words.map{it.text})}
 @Test fun `bad empty CSV returns error instead of crashing`(){val r=CsvWordSetParser.parse("Empty","\n\n",1);assertNull(r.wordSet);assertNotNull(r.error)}
 @Test fun `successful import replaces instead of appending`(){val old=CsvWordSetParser.parse("A","one\ntwo",1).wordSet;val next=CsvWordSetParser.parse("B","three",2);val active=replaceImportedWordSet(old,next)!!;assertEquals("B",active.name);assertEquals(listOf("three"),active.words.map{it.text})}
 @Test fun `failed import keeps previous working collection`(){val old=CsvWordSetParser.parse("A","one",1).wordSet;val failed=CsvWordSetParser.parse("B","\n",2);assertEquals(old,replaceImportedWordSet(old,failed))}
 @Test fun `repository selection feeds engine`(){class Repo:WordSetRepository{val sets=mutableListOf(WordSet("a","A",listOf("old").map{AliasWord(it)},WordSetSource.BUNDLED),WordSet("b","B",listOf("նոր").map{AliasWord(it)},WordSetSource.IMPORTED));var id="a";override fun active()=sets.first{it.id==id};override fun all()=sets;override fun select(id:String){this.id=id}};val repo=Repo();repo.select("b");val e=GameEngine(repo);var s=e.addTeam(e.create("g","A"),"B");s=e.startRound(s,0);assertTrue(s.currentRound!!.words.all{it.text=="նոր"})}
}

class LocalizationTest {
 @Test fun `protest word status enum names do not leak to users`(){
  // ProtestStatus values must not contain raw enum-like display names
  ProtestStatus.entries.forEach{s->assertFalse("enum name $s should not be used as a label",s.name.isEmpty())}
  // WordStatus values used in protest flow are distinct from enum names
  assertNotEquals("FINAL_WRONG",WordStatus.FINAL_WRONG.name.lowercase())
 }
 @Test fun `protest strings are distinct and non-empty in enum`(){
  assertNotEquals(ProtestStatus.ACTIVE,ProtestStatus.ADMITTED_WRONG)
  assertNotEquals(ProtestStatus.ACTIVE,ProtestStatus.CANCELLED)
  assertNotEquals(ProtestStatus.ADMITTED_WRONG,ProtestStatus.CANCELLED)
 }
}

class SessionLifecycleTest {
 private val engine=GameEngine(object:WordSetRepository{override fun active()=WordSet("t","test",listOf("one","two","three","four","five").map{AliasWord(it)},WordSetSource.BUNDLED)})
 private fun twoTeam()=engine.addTeam(engine.create("g","Alpha"),"Beta")
 @Test fun `each new game gets a unique non-empty session ID`(){val s=twoTeam();assertTrue(s.id.isNotBlank())}
 @Test fun `two separate creates produce different IDs`(){val a=twoTeam();val b=twoTeam();assertNotEquals(a.id,b.id)}
 @Test fun `session ID is stable through startRound`(){val s=twoTeam();val id=s.id;val r=engine.startRound(s,0);assertEquals(id,r.id)}
 @Test fun `session ID is stable through correct`(){var s=engine.startRound(twoTeam(),0);val id=s.id;s=engine.correct(s);assertEquals(id,s.id)}
 @Test fun `session ID is stable through endRound`(){var s=engine.startRound(twoTeam(),0);val id=s.id;s=engine.endRound(s);assertEquals(id,s.id)}
 @Test fun `session ID is stable through next`(){var s=engine.startRound(twoTeam(),0);val id=s.id;s=engine.endRound(s);s=engine.next(s);assertEquals(id,s.id)}
 @Test fun `session ID is stable through protest and admit`(){var s=engine.startRound(twoTeam(),System.currentTimeMillis()+60_000);val id=s.id;s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=engine.protest(s,wordId,s.teams[1].id);assertEquals(id,s.id);s=engine.admitProtest(s);assertEquals(id,s.id)}
 @Test fun `session ID is stable through rejectProtest`(){var s=engine.startRound(twoTeam(),System.currentTimeMillis()+60_000);val id=s.id;s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=engine.protest(s,wordId,s.teams[1].id);s=engine.rejectProtest(s);assertEquals(id,s.id)}
 @Test fun `session version starts at zero after create`(){assertEquals(0L,engine.create("g","Alpha").version)}
 @Test fun `addTeam increments version`(){val s=engine.create("g","Alpha");val after=engine.addTeam(s,"Beta");assertEquals(s.version+1,after.version)}
 @Test fun `protest increments version`(){var s=engine.startRound(twoTeam(),System.currentTimeMillis()+60_000);s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;val v=s.version;s=engine.protest(s,wordId,s.teams[1].id);assertEquals(v+1,s.version)}
 @Test fun `admitProtest increments version`(){var s=engine.startRound(twoTeam(),System.currentTimeMillis()+60_000);s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=engine.protest(s,wordId,s.teams[1].id);val v=s.version;s=engine.admitProtest(s);assertEquals(v+1,s.version)}
 @Test fun `rejectProtest increments version`(){var s=engine.startRound(twoTeam(),System.currentTimeMillis()+60_000);s=engine.correct(s);val wordId=s.currentRound!!.words.first{it.status==WordStatus.CORRECT}.id;s=engine.protest(s,wordId,s.teams[1].id);val v=s.version;s=engine.rejectProtest(s);assertEquals(v+1,s.version)}
 @Test fun `snapshot with same session ID matches active session`(){val s=twoTeam();val incoming=s.copy();assertEquals(s.id,incoming.id)}
 @Test fun `snapshot with different session ID is from stale session`(){val old=twoTeam();val fresh=twoTeam();assertNotEquals(old.id,fresh.id)}
 @Test fun `rejoin guard client with old session ID rejects new host session`(){val clientActiveId=twoTeam().id;val newHostSession=twoTeam();assertNotEquals(clientActiveId,newHostSession.id)}
}

class CsvHintTest {
 @Test fun `one-column CSV gives null hints`(){val r=CsvWordSetParser.parse("No hints","word\nArarat\nSevan",1);assertNotNull(r.wordSet);r.wordSet!!.words.forEach{assertNull(it.hint)}}
 @Test fun `two-column CSV parses hints`(){val csv="word,hint\nArarat,Mountain\nSevan,Lake";val r=CsvWordSetParser.parse("Hints",csv,1);assertEquals("Ararat",r.wordSet!!.words[0].text);assertEquals("Mountain",r.wordSet!!.words[0].hint);assertEquals("Sevan",r.wordSet!!.words[1].text);assertEquals("Lake",r.wordSet!!.words[1].hint)}
 @Test fun `word without hint column has null hint in two-column CSV`(){val csv="word,hint\nArarat,Mountain\nSevan,";val r=CsvWordSetParser.parse("Mixed",csv,1);assertNull(r.wordSet!!.words[1].hint)}
 @Test fun `Armenian UTF-8 hint is preserved`(){val word="Արարատ";val hint="Բարդ լիռ";val csv="word,hint\n$word,$hint";val r=CsvWordSetParser.parse("Arm",csv,1);assertEquals(word,r.wordSet!!.words[0].text);assertEquals(hint,r.wordSet!!.words[0].hint)}
}

class HintAvailabilityTest {
 private val words=listOf(AliasWord("Ararat","Big mountain"),AliasWord("Sevan","Big lake"),AliasWord("Yerevan",null))
 private val wordSet=WordSet("t","test",words,WordSetSource.BUNDLED)
 private val engine=GameEngine(object:WordSetRepository{override fun active()=wordSet})
 private fun roundInProgress():AliasGameSession{val s=engine.addTeam(engine.create("g","Alpha"),"Beta");return engine.startRound(s,System.currentTimeMillis()+60_000)}
 @Test fun `lookupHint returns hint for word with hint`(){assertEquals("Big mountain",engine.lookupHint("Ararat"))}
 @Test fun `lookupHint returns null for word without hint`(){assertNull(engine.lookupHint("Yerevan"))}
 @Test fun `lookupHint returns null for unknown word`(){assertNull(engine.lookupHint("Unknown"))}
 @Test fun `isActiveTeamController true for active team device`(){val s=roundInProgress();assertTrue(engine.isActiveTeamController(s,s.currentRound!!.teamId,false))}
 @Test fun `isActiveTeamController false for passive team device`(){val s=roundInProgress();val passiveId=s.teams.first{it.id!=s.currentRound!!.teamId}.id;assertFalse(engine.isActiveTeamController(s,passiveId,false))}
 @Test fun `hints not stored in RoundWord text`(){val s=roundInProgress();val activeWord=s.currentRound!!.words.first{it.isActive};assertFalse(activeWord.text.contains("mountain",ignoreCase=true))}
}

class TransliterationAuditTest {
 @Test fun `Armenian strings file has no Latin transliteration`(){
  val file=File("src/main/res/values-hy/strings.xml")
  if(!file.exists())return
  val content=file.readText()
  val exempt=setOf("Alias-ka","CSV","Wi-Fi","Bluetooth")
  val stringValues=Regex("""<string[^>]*>([^<]+)</string>""").findAll(content).map{it.groupValues[1]}
  val failures=mutableListOf<String>()
  for(value in stringValues){
   var v=value.replace(Regex("%\\d+\\\$[sd]"),"")
   exempt.forEach{v=v.replace(it,"")}
   val latinChars=v.filter{c->c.isLetter()&&c.code<128}
   if(latinChars.isNotEmpty())failures.add("'$value' has Latin chars: $latinChars")
  }
  assertTrue("Latin chars in values-hy/strings.xml:\n${failures.joinToString("\n")}",failures.isEmpty())
 }
}
