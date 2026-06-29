package com.davoyans.alias_ka

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davoyans.alias_ka.domain.*
import com.davoyans.alias_ka.nearby.*
import com.davoyans.alias_ka.data.SharedPrefsWordQueueStore
import com.davoyans.alias_ka.data.SupabaseSyncRepository
import android.util.Base64
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

enum class Screen { HOME, CREATE, LOBBY, JOIN, ROUND, REVIEW, SCOREBOARD, SETTINGS, ABOUT, GAME_OVER, DIAGNOSTICS }
enum class WordTransferStatus { IDLE, RECEIVING, COMPLETE, FAILED }
enum class NearbyOperation { SHARE, JOIN }
enum class ClientMode { IDLE, JOINING, CONNECTED, DISCONNECTED, HOST }
enum class ClientEvent { GAME_ENDED, CONNECTION_LOST }

class AppViewModel(app:Application):AndroidViewModel(app),NearbyConnectionsGameTransport.Listener {
 private val store=SharedPrefsWordQueueStore(app)
 private val syncRepo=SupabaseSyncRepository(store=store,scope=viewModelScope,onLog=::log)
 private val engine=GameEngine(syncRepo)
 private val prefs=app.getSharedPreferences("alias-ka",0)
 val transport=NearbyConnectionsGameTransport(app,this)
 private val logFmt=SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

 var screen by mutableStateOf(Screen.HOME)
 var session by mutableStateOf<AliasGameSession?>(null)
 var activeSessionId by mutableStateOf<String?>(null)
 var hostEndpointId by mutableStateOf<String?>(null)
 var discovered by mutableStateOf(listOf<DiscoveredRoom>())
 var logs by mutableStateOf(listOf<String>())
 var sharing by mutableStateOf(false)
 var error by mutableStateOf<String?>(null)
 var lastTechnicalError by mutableStateOf<String?>(null)
 var clientConnected by mutableStateOf(false)
 var clientTeamName by mutableStateOf("")
 var isHost by mutableStateOf(false)
 var ownTeamId by mutableStateOf<String?>(null)
 var isDiscovering by mutableStateOf(false)
 var transferStatus by mutableStateOf(WordTransferStatus.IDLE)
 var transferReceived by mutableIntStateOf(0)
 var transferTotal by mutableIntStateOf(0)
 var receivedHostWords by mutableStateOf<List<String>>(emptyList())
 var pendingNearbyOperation by mutableStateOf<NearbyOperation?>(null)
 var clientEvent by mutableStateOf<ClientEvent?>(null)
 private var transferChunks=mutableMapOf<Int,List<String>>()
 private var transferExpectedChunks=0
 private var transferChecksum=""
 private var transferCompleteSignal=false

 var hintWordId by mutableStateOf<String?>(null)
 var hintText by mutableStateOf<String?>(null)
 var isSyncing by mutableStateOf(false)
 val syncQueueSize:Int get()=syncRepo.queueSize

 val suggestions=listOf("Alias-ka Party","Կինոյի երեկո","Խոսող ծիրան","Ուրախ կեֆ")
 init{log("INFO APP_STARTED version=${BuildConfig.VERSION_NAME}")}

 var roundSeconds by mutableIntStateOf(prefs.getInt("roundSeconds",60))
 var targetScore by mutableIntStateOf(prefs.getInt("targetScore",50))
 val lastGame get()=prefs.getString("lastGame",null)
 val lastTeam get()=prefs.getString("lastTeam",null)

 val clientMode: ClientMode get() = when {
  isHost -> ClientMode.HOST
  activeSessionId!=null && clientConnected -> ClientMode.CONNECTED
  activeSessionId!=null && !clientConnected -> ClientMode.DISCONNECTED
  isDiscovering -> ClientMode.JOINING
  else -> ClientMode.IDLE
 }

 fun updateRoundSeconds(v:Int){roundSeconds=v.coerceIn(15,180);prefs.edit().putInt("roundSeconds",roundSeconds).apply()}
 fun updateTargetScore(v:Int){targetScore=v.coerceIn(5,200);prefs.edit().putInt("targetScore",targetScore).apply()}
 fun clearLogs(){logs=emptyList();log("INFO LOGS_CLEARED userAction=true")}

 fun create(game:String,team:String){runCatching{engine.create(game,team,GameSettings(roundSeconds,targetScore))}.onSuccess{prefs.edit().putString("lastGame",game).putString("lastTeam",team).apply();session=it;activeSessionId=it.id;isHost=true;ownTeamId=it.teams.first().id;screen=Screen.LOBBY;log("INFO SESSION_CREATED id=${it.id} game=${it.gameName}")}.onFailure{error="Team name is required"}}
 fun add(name:String){mutate{engine.addTeam(it,name)}}
 fun rename(id:String,name:String){mutate{engine.rename(it,id,name)}}
 fun remove(id:String){mutate{engine.remove(it,id)}}
 fun move(i:Int,d:Int){mutate{engine.move(it,i,d)}}
 fun share(){if(sharing){transport.stopAdvertising();sharing=false;log("INFO ADVERTISING_STOPPED");return};log("INFO PERMISSION_CHECK_STARTED operation=share");if(!hasNearbyPermissions(NearbyOperation.SHARE)){pendingNearbyOperation=NearbyOperation.SHARE;return};startSharing()}
 fun discover(){screen=Screen.JOIN;log("INFO JOIN_GAME_OPENED");if(!hasNearbyPermissions(NearbyOperation.JOIN))pendingNearbyOperation=NearbyOperation.JOIN else refreshDiscovery()}
 fun refreshDiscovery(){if(!hasNearbyPermissions(NearbyOperation.JOIN)){pendingNearbyOperation=NearbyOperation.JOIN;return};transport.stopDiscovery();discovered=emptyList();isDiscovering=true;transport.startDiscovery();log("INFO DISCOVERY_REFRESHED")}
 fun connect(room:DiscoveredRoom){log("INFO CONNECTION_REQUESTED endpoint=${room.endpointId} room=${room.name}");transport.connect(room.endpointId)}
 fun resumeNearbyOperation(){val op=pendingNearbyOperation;pendingNearbyOperation=null;log("INFO PERMISSION_GRANTED operation=$op");when(op){NearbyOperation.SHARE->startSharing();NearbyOperation.JOIN->refreshDiscovery();null->Unit}}
 fun cancelNearbyPermission(){val op=pendingNearbyOperation;pendingNearbyOperation=null;permissionDenied(op?.name?.lowercase()?:"nearby")}
 fun submitClientTeam(){
  if(clientTeamName.isNotBlank()){
   isHost=false;activeSessionId=null;transferStatus=WordTransferStatus.RECEIVING;transferReceived=0
   transferChunks.clear();transferExpectedChunks=0;transferChecksum="";transferCompleteSignal=false
   transport.sendEvent(GameEvent("TEAM_JOIN_REQUESTED",clientTeamName.trim()))
   log("INFO JOIN_HANDSHAKE_STARTED team=${clientTeamName.trim()}")
  }
 }
 fun retryWordTransfer(){transferStatus=WordTransferStatus.RECEIVING;transport.sendEvent(GameEvent("WORD_COLLECTION_RETRY",clientTeamName.trim()));log("INFO WORD_TRANSFER_RETRY")}
 fun permissionDenied(operation:String){val detail="Runtime permission denied for $operation";lastTechnicalError=detail;error=detail;log("ERROR $detail")}

 fun start(){
  val s=session?:return
  if(s.teams.size<2){error="Add at least two teams to start";return}
  if(isSyncing)return
  viewModelScope.launch{
   isSyncing=true
   syncRepo.ensureWordsAvailableForGame()
   isSyncing=false
   session=engine.startRound(s,System.currentTimeMillis())
   trackActiveWord()
   screen=Screen.ROUND
   broadcast()
   log("INFO ROUND_STARTED session=${s.id}")
  }
 }

 fun correct(){
  if(isHost){session=session?.let(engine::correct);trackActiveWord();broadcast()}
  else transport.sendEvent(GameEvent("WORD_MARKED_CORRECT"))
  log("INFO WORD_MARKED_CORRECT")
 }

 fun skip(){
  if(isHost){session=session?.let(engine::skip);trackActiveWord();broadcast()}
  else transport.sendEvent(GameEvent("WORD_SKIPPED"))
  log("INFO WORD_SKIPPED")
 }

 fun selectSkipped(id:String){if(isHost){session=session?.let{engine.selectSkipped(it,id)};broadcast()}else transport.sendEvent(GameEvent("SKIPPED_WORD_SELECTED",id))}
 fun guessedSkipped(){if(isHost){session=session?.let(engine::guessedSkipped);broadcast()}else transport.sendEvent(GameEvent("SKIPPED_WORD_GUESSED"))}

 fun protest(wordId:String){
  val s=session?:return
  val byTeamId=ownTeamId?:s.teams.getOrNull((s.currentTeamIndex+1)%s.teams.size)?.id?:return
  if(isHost){session=engine.protest(s,wordId,byTeamId);broadcast()}else transport.sendEvent(GameEvent("WORD_PROTESTED",wordId))
  log("INFO WORD_PROTESTED word=$wordId")
 }
 fun admitProtest(){if(isHost){session=session?.let(engine::admitProtest);broadcast()}else transport.sendEvent(GameEvent("PROTEST_ADMITTED"));log("INFO PROTEST_ADMITTED")}
 fun cancelProtest(){if(isHost){session=session?.let(engine::rejectProtest);broadcast()}else transport.sendEvent(GameEvent("PROTEST_CANCELLED"));log("INFO PROTEST_CANCELLED")}
 fun requestHint(wordId:String,wordText:String){
  if(isHost){val hint=engine.lookupHint(wordText);hintWordId=wordId;hintText=hint;log("INFO HINT_SHOWN wordId=$wordId")}
  else{transport.sendEvent(GameEvent("HINT_REQUESTED","$wordId|$wordText"));log("INFO HINT_REQUESTED wordId=$wordId")}
 }
 fun dismissHint(){hintWordId=null;hintText=null}
 fun endRound(){session=session?.let(engine::endRound);screen=Screen.REVIEW;broadcast()}
 fun next(){session=session?.let(engine::next);screen=if(session?.state==GameState.GAME_OVER)Screen.GAME_OVER else Screen.SCOREBOARD;broadcast()}

 fun beginNext(){
  if(isSyncing)return
  viewModelScope.launch{
   isSyncing=true
   syncRepo.ensureWordsAvailableForGame()
   isSyncing=false
   session=session?.let{engine.startRound(it,System.currentTimeMillis())}
   trackActiveWord()
   screen=Screen.ROUND
   broadcast()
  }
 }

 fun resetSame(){session=session?.copy(teams=session!!.teams.map{it.copy(score=0,roundsPlayed=0)},state=GameState.LOBBY,currentTeamIndex=0,currentRound=null,finishing=false,winnerTeamId=null,protestState=null);screen=Screen.LOBBY}
 fun home(){
  if(isHost&&session!=null){transport.sendEvent(GameEvent("SESSION_ENDED",session!!.id));log("INFO SESSION_ENDED id=${session!!.id}")}
  transport.stopAdvertising();transport.stopDiscovery();transport.stopAllEndpoints()
  sharing=false
  clearAllState()
  screen=Screen.HOME
 }
 private fun clearAllState(){
  session=null;activeSessionId=null;isHost=false;clientConnected=false;hostEndpointId=null;hintWordId=null;hintText=null
  clientTeamName="";ownTeamId=null;transferStatus=WordTransferStatus.IDLE;transferReceived=0
  transferTotal=0;receivedHostWords=emptyList();transferChunks.clear();transferExpectedChunks=0
  transferChecksum="";transferCompleteSignal=false;discovered=emptyList();isDiscovering=false
  log("INFO CLIENT_STATE_RESET")
 }
 private fun trackActiveWord(){
  if(!isHost)return
  val round=session?.currentRound?:return
  if(round.phase!=RoundPhase.NORMAL_PASS)return
  val active=round.words.firstOrNull{it.isActive}?:return
  syncRepo.markWordDisplayed(active.text)
  syncRepo.prefetchIfLow()
 }
 private fun mutate(block:(AliasGameSession)->AliasGameSession){val s=session?:return;runCatching{block(s)}.onSuccess{session=it;broadcast()}.onFailure{error=it.message}}
 private fun broadcast(){session?.let{transport.sendSnapshot(GameSnapshot(SessionCodec.encode(it)))}}
 override fun onAdvertisingStarted(){sharing=true;log("INFO ADVERTISING_STARTED")}
 override fun onDiscoveryStarted(){isDiscovering=true;log("INFO DISCOVERY_STARTED")}
 override fun onRoom(room:DiscoveredRoom){isDiscovering=false;if(discovered.none{it.endpointId==room.endpointId})discovered=discovered+room;log("INFO ENDPOINT_FOUND ${room.endpointId} name=${room.name}")}
 override fun onConnected(endpointId:String){
  clientConnected=true
  if(!isHost)hostEndpointId=endpointId
  log("INFO CONNECTED endpoint=$endpointId mode=${if(isHost)"host" else "client"}")
 }
 override fun onDisconnected(endpointId:String){
  log("WARN CLIENT_DISCONNECTED endpoint=$endpointId")
  if(!isHost&&activeSessionId!=null){
   clientEvent=ClientEvent.CONNECTION_LOST
   clearAllState()
   transport.stopDiscovery()
   screen=Screen.HOME
  } else {
   clientConnected=false
   hostEndpointId=null
  }
 }
 override fun onMessage(endpointId:String,message:String){when{
  message.startsWith("EVENT|TEAM_JOIN_REQUESTED|")&&isHost->{
   val name=message.substringAfterLast('|')
   session?.let{runCatching{engine.addTeam(it,name,TeamConnection.Connected(endpointId,"Android device"))}.onSuccess{s->session=s;sendWordCollection(endpointId);broadcast();log("INFO JOIN_HANDSHAKE_ACCEPTED endpoint=$endpointId team=$name")}}
  }
  message.startsWith("EVENT|WORD_COLLECTION_RETRY|")&&isHost->{sendWordCollection(endpointId)}
  message.startsWith("EVENT|WORD_COLLECTION_METADATA|")&&!isHost->{val p=message.removePrefix("EVENT|WORD_COLLECTION_METADATA|").split('|');transferTotal=p[1].toInt();transferExpectedChunks=p[2].toInt();transferChecksum=p[3];transferChunks.clear();transferReceived=0;transferCompleteSignal=false;transferStatus=WordTransferStatus.RECEIVING;log("INFO WORD_TRANSFER_STARTED total=$transferTotal")}
  message.startsWith("EVENT|WORD_COLLECTION_CHUNK|")&&!isHost->{val p=message.removePrefix("EVENT|WORD_COLLECTION_CHUNK|").split('|');val index=p[0].toInt();val decoded=String(Base64.decode(p[2],Base64.NO_WRAP),Charsets.UTF_8).split('\n').filter{it.isNotEmpty()};transferChunks[index]=decoded;transferReceived=transferChunks.values.sumOf{it.size};tryFinishTransfer()}
  message.startsWith("EVENT|WORD_COLLECTION_TRANSFER_COMPLETE|")&&!isHost->{transferCompleteSignal=true;tryFinishTransfer()}
  message.startsWith("EVENT|SESSION_ENDED|")&&!isHost->{
   val sessionId=message.substringAfterLast('|')
   if(activeSessionId==null||activeSessionId==sessionId){
    log("INFO HOST_GAME_STOPPED session=$sessionId")
    clientEvent=ClientEvent.GAME_ENDED
    clearAllState()
    transport.stopDiscovery()
    screen=Screen.HOME
   }
  }
  message=="EVENT|WORD_MARKED_CORRECT|"&&isHost->{session=session?.let(engine::correct);trackActiveWord();broadcast()}
  message=="EVENT|WORD_SKIPPED|"&&isHost->{session=session?.let(engine::skip);trackActiveWord();broadcast()}
  message.startsWith("EVENT|SKIPPED_WORD_SELECTED|")&&isHost->{val id=message.substringAfterLast('|');session=session?.let{engine.selectSkipped(it,id)};broadcast()}
  message=="EVENT|SKIPPED_WORD_GUESSED|"&&isHost->{session=session?.let(engine::guessedSkipped);broadcast()}
  message.startsWith("EVENT|WORD_PROTESTED|")&&isHost->{
   val wordId=message.substringAfterLast('|');val s=session
   val byTeamId=s?.teams?.firstOrNull{(it.connection as? TeamConnection.Connected)?.endpointId==endpointId}?.id
   if(s!=null&&byTeamId!=null){session=engine.protest(s,wordId,byTeamId);broadcast()}
  }
  message=="EVENT|PROTEST_ADMITTED|"&&isHost->{session=session?.let(engine::admitProtest);broadcast()}
  message=="EVENT|PROTEST_CANCELLED|"&&isHost->{session=session?.let(engine::rejectProtest);broadcast()}
  message.startsWith("EVENT|HINT_REQUESTED|")&&isHost->{
   val parts=message.removePrefix("EVENT|HINT_REQUESTED|").split('|',limit=2)
   val wordId=parts[0];val wordText=parts.getOrElse(1){""}
   val s=session;val byTeamId=s?.teams?.firstOrNull{(it.connection as? TeamConnection.Connected)?.endpointId==endpointId}?.id
   if(s!=null&&byTeamId!=null&&engine.isActiveTeamController(s,byTeamId,false)){
    val hint=engine.lookupHint(wordText)
    transport.sendEventTo(endpointId,GameEvent("HINT_RESPONSE","$wordId|${hint.orEmpty()}"))
    log("INFO HINT_SHOWN wordId=$wordId teamId=$byTeamId")
   }else{transport.sendEventTo(endpointId,GameEvent("HINT_DENIED","$wordId"));log("WARN HINT_DENIED_NOT_ACTIVE wordId=$wordId")}
  }
  message.startsWith("EVENT|HINT_RESPONSE|")&&!isHost->{
   val parts=message.removePrefix("EVENT|HINT_RESPONSE|").split('|',limit=2)
   hintWordId=parts[0];hintText=parts.getOrElse(1){""}.takeIf{it.isNotEmpty()}
   log("INFO HINT_SHOWN wordId=${parts[0]}")
  }
  message.startsWith("EVENT|HINT_DENIED|")&&!isHost->{
   val wordId=message.substringAfterLast('|');hintWordId=wordId;hintText=null;log("WARN HINT_DENIED_NO_HINT wordId=$wordId")
  }
  message.startsWith("SNAPSHOT|")&&!isHost->runCatching{SessionCodec.decode(message.removePrefix("SNAPSHOT|"))}.onSuccess{s->
   if(activeSessionId!=null&&s.id!=activeSessionId){log("INFO SNAPSHOT_IGNORED_OLD_SESSION incoming=${s.id} active=$activeSessionId");return@onSuccess}
   session=s
   if(activeSessionId==null){activeSessionId=s.id;log("INFO SESSION_ACCEPTED id=${s.id}")}
   ownTeamId=s.teams.firstOrNull{it.name.equals(clientTeamName,true)}?.id
   if(transferStatus==WordTransferStatus.COMPLETE)routeClientSnapshot(s)
  }.onFailure{onError("Snapshot apply failed: ${it.message}")}
 };log("DEBUG MESSAGE ${message.take(80)}")}
 override fun onError(message:String){if(message.startsWith("Advertising"))sharing=false;if(message.startsWith("Discovery"))isDiscovering=false;lastTechnicalError=message;error=message;log("ERROR $message")}
 private fun log(v:String){val ts=logFmt.format(Date());logs=(logs+"$ts $v").takeLast(200)}
 fun canCorrect():Boolean {
  val s=session?:return false;val round=s.currentRound?:return false
  if(System.currentTimeMillis()>=round.endsAtMillis)return false
  val activeTeamIsManual=s.teams.firstOrNull{it.id==round.teamId}?.connection is TeamConnection.Manual
  return round.teamId==ownTeamId||(isHost&&activeTeamIsManual)
 }
 fun canProtest(wordId:String):Boolean {
  val s=session?:return false;val round=s.currentRound?:return false
  if(s.protestState!=null)return false
  val word=round.words.firstOrNull{it.id==wordId}?:return false
  if(word.status!=WordStatus.CORRECT)return false
  return !canCorrect()
 }
 fun isActiveTeamPhone():Boolean {val s=session?:return false;return engine.isActiveTeamController(s,ownTeamId,isHost)}
 private fun sendWordCollection(endpointId:String){val set=syncRepo.active();val texts=set.words.map{it.text};val chunks=texts.chunked(40);val checksum=wordChecksum(texts);log("INFO WORD_TRANSFER_STARTED endpoint=$endpointId count=${texts.size}");transport.sendEventTo(endpointId,GameEvent("WORD_COLLECTION_METADATA","${set.id}|${texts.size}|${chunks.size}|$checksum"));chunks.forEachIndexed{i,chunk->val data=Base64.encodeToString(chunk.joinToString("\n").toByteArray(),Base64.NO_WRAP);transport.sendEventTo(endpointId,GameEvent("WORD_COLLECTION_CHUNK","$i|${chunks.size}|$data"))};transport.sendEventTo(endpointId,GameEvent("WORD_COLLECTION_TRANSFER_COMPLETE",checksum));log("INFO WORD_TRANSFER_COMPLETED endpoint=$endpointId")}
 private fun tryFinishTransfer(){if(!transferCompleteSignal||transferChunks.size!=transferExpectedChunks)return;val words=(0 until transferExpectedChunks).flatMap{transferChunks[it].orEmpty()};if(words.size==transferTotal&&wordChecksum(words)==transferChecksum){receivedHostWords=words;transferStatus=WordTransferStatus.COMPLETE;log("INFO WORD_TRANSFER_COMPLETE count=${words.size}");session?.let{routeClientSnapshot(it)}}else{transferStatus=WordTransferStatus.FAILED;onError("Word transfer checksum/count mismatch expected=$transferTotal actual=${words.size}")}}
 private fun routeClientSnapshot(s:AliasGameSession){
  screen=when(s.state){
   GameState.ROUND_RUNNING->Screen.ROUND
   GameState.ROUND_REVIEW->Screen.REVIEW
   GameState.GAME_OVER->Screen.GAME_OVER
   GameState.FINISHING_CURRENT_CYCLE->Screen.SCOREBOARD
   else->if(s.currentRound==null&&s.teams.any{it.roundsPlayed>0})Screen.SCOREBOARD else Screen.LOBBY
  }
  log("INFO CLIENT_ROUTED ${screen.name} session=${s.id} state=${s.state.name}")
 }
 private fun hasNearbyPermissions(operation:NearbyOperation):Boolean {val app=getApplication<Application>();return NearbyPermissionPolicy.requiredRuntime(Build.VERSION.SDK_INT,operation==NearbyOperation.SHARE).all{app.checkSelfPermission(it)==PackageManager.PERMISSION_GRANTED}}
 private fun startSharing(){val s=session?:return;if(syncRepo.active().words.isEmpty()){onError("Active word collection is empty");return};log("INFO SHARE_GAME_TAPPED room=${s.gameName}");transport.startAdvertising(HostRoom("Alias-ka - ${s.gameName}"))}
}
