package com.davoyans.alias_ka

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.widget.Toast
import android.content.pm.PackageManager
import android.content.ClipData
import android.content.ClipboardManager
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.davoyans.alias_ka.domain.*
import kotlinx.coroutines.delay


class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){applyLanguage(this,getSharedPreferences("alias-ka",0).getString("language",null));super.onCreate(b);setContent{AliasTheme{AliasApp()}}}}
private fun applyLanguage(context:Context,tag:String?){if(tag==null)return;val locale=Locale.forLanguageTag(tag);Locale.setDefault(locale);val config=context.resources.configuration;config.setLocale(locale);context.resources.updateConfiguration(config,context.resources.displayMetrics)}
private fun copyText(context:Context,label:String,text:String){(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(label,text));Toast.makeText(context,context.getString(R.string.copied),Toast.LENGTH_SHORT).show()}
private val Purple=Color(0xFF6246EA); private val Coral=Color(0xFFFF6584); private val Cream=Color(0xFFFFF8F0)
private fun teamColor(index:Int)=listOf(Color(0xFFFFE3E8),Color(0xFFE1F5FE),Color(0xFFE8F5E9),Color(0xFFFFF3CD))[index%4]
@Composable private fun AliasTheme(content: @Composable () -> Unit) {
 MaterialTheme(colorScheme=lightColorScheme(primary=Purple,secondary=Coral,background=Cream,surface=Color.White),content=content)
}

@Composable fun AliasApp(vm:AppViewModel= viewModel()){
 val context=LocalContext.current;val backPolicy=remember{BackNavigationPolicy()}
 val nearbyPermissions=NearbyPermissionPolicy.requiredRuntime(Build.VERSION.SDK_INT,vm.pendingNearbyOperation==NearbyOperation.SHARE).toTypedArray();val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->if(nearbyPermissions.all{result[it]==true||context.checkSelfPermission(it)==PackageManager.PERMISSION_GRANTED})vm.resumeNearbyOperation()else vm.cancelNearbyPermission()}
 BackHandler(enabled=true){when(backPolicy.decide(vm.screen,System.currentTimeMillis())){BackAction.SHOW_EXIT_HINT->Toast.makeText(context,context.getString(R.string.back_exit_hint),Toast.LENGTH_SHORT).show();BackAction.EXIT_APP->(context as? ComponentActivity)?.finish();BackAction.GO_HOME->vm.home();BackAction.GO_SETTINGS->vm.screen=Screen.SETTINGS;BackAction.GO_LOBBY->vm.screen=if(vm.session!=null)Screen.LOBBY else Screen.HOME;BackAction.GO_SCOREBOARD->vm.screen=Screen.SCOREBOARD;BackAction.BLOCK_ACTIVE_ROUND->Toast.makeText(context,context.getString(R.string.round_back_blocked),Toast.LENGTH_SHORT).show()}}
 Scaffold(containerColor=Cream){pad->Box(Modifier.padding(pad).fillMaxSize()){
  when(vm.screen){Screen.HOME->Home(vm);Screen.CREATE->Create(vm);Screen.LOBBY->Lobby(vm);Screen.JOIN->Join(vm);Screen.ROUND->Round(vm);Screen.REVIEW->Review(vm);Screen.SCOREBOARD->Scoreboard(vm);Screen.SETTINGS->Settings(vm);Screen.ABOUT->About(vm);Screen.GAME_OVER->GameOver(vm);Screen.DIAGNOSTICS->Diagnostics(vm)}
  vm.error?.let{AlertDialog(onDismissRequest={vm.error=null},confirmButton={TextButton(onClick={vm.error=null}){Text(stringResource(R.string.save))}},dismissButton={vm.lastTechnicalError?.let{detail->TextButton(onClick={copyText(context,"Alias-ka error",detail)}){Text(stringResource(R.string.copy))}}},text={Text(stringResource(R.string.operation_failed))})}
  vm.pendingNearbyOperation?.let{AlertDialog(onDismissRequest={vm.cancelNearbyPermission()},confirmButton={TextButton(onClick={permissionLauncher.launch(nearbyPermissions)}){Text(stringResource(R.string.allow_nearby))}},dismissButton={TextButton(onClick={vm.cancelNearbyPermission()}){Text(stringResource(R.string.cancel))}},text={Text(stringResource(R.string.permission_reason))})}
  vm.clientEvent?.let{ev->AlertDialog(onDismissRequest={vm.clientEvent=null},confirmButton={TextButton(onClick={vm.clientEvent=null}){Text(stringResource(R.string.save))}},text={Text(stringResource(if(ev==ClientEvent.GAME_ENDED)R.string.game_ended else R.string.connection_lost))})}
 }}
}
@Composable private fun Page(title:String,back:(()->Unit)?=null,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){if(back!=null)TextButton(onClick=back){Text("‹")};Text(title,fontSize=28.sp,fontWeight=FontWeight.Bold,color=Purple)};content()}}
@Composable private fun Home(vm:AppViewModel){Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("Alias-ka",fontSize=52.sp,fontWeight=FontWeight.Black,color=Purple);Text(stringResource(R.string.tagline),color=Coral);Spacer(Modifier.height(44.dp));BigButton(stringResource(R.string.create_game)){vm.screen=Screen.CREATE};BigButton(stringResource(R.string.join_game)){vm.discover()};OutlinedButton(onClick={vm.screen=Screen.SETTINGS},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.settings))};TextButton(onClick={vm.screen=Screen.ABOUT}){Text(stringResource(R.string.about))}}}
@Composable private fun BigButton(text:String,onClick:()->Unit){Button(onClick=onClick,modifier=Modifier.fillMaxWidth().padding(vertical=5.dp).height(58.dp),shape=RoundedCornerShape(18.dp)){Text(text,fontSize=19.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun Create(vm:AppViewModel){val suggestions=stringArrayResource(R.array.game_suggestions);var game by remember{mutableStateOf(vm.lastGame?:suggestions.random())};var team by remember{mutableStateOf(vm.lastTeam.orEmpty())};Page(stringResource(R.string.create_game),{vm.screen=Screen.HOME}){OutlinedTextField(game,{game=it},label={Text(stringResource(R.string.game_name))},modifier=Modifier.fillMaxWidth());OutlinedTextField(team,{team=it},label={Text(stringResource(R.string.team_name))},modifier=Modifier.fillMaxWidth());suggestions.forEach{s->AssistChip(onClick={game=s},label={Text(s)})};Spacer(Modifier.weight(1f));BigButton(stringResource(R.string.create_lobby)){vm.create(game,team)}}}
@Composable private fun Lobby(vm:AppViewModel){val s=vm.session;if(s==null){ClientLobby(vm);return};var dialog by remember{mutableStateOf<String?>(null)};var value by remember{mutableStateOf("")};Page(s.gameName,{vm.home()}){Row{AssistChip(onClick={},label={Text(stringResource(R.string.admin))});Spacer(Modifier.width(8.dp));AssistChip(onClick={},label={Text(stringResource(R.string.wordset))})};Text("${stringResource(R.string.target_score)}: ${s.settings.targetScore} · ${stringResource(R.string.round_seconds)}: ${s.settings.roundSeconds}");Text(stringResource(R.string.teams),fontSize=22.sp,fontWeight=FontWeight.Bold);LazyColumn(Modifier.weight(1f)){items(s.teams,key={it.id}){team->Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=teamColor(s.teams.indexOf(team)))){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(team.name,fontWeight=FontWeight.Bold);Text(if(team.connection is TeamConnection.Connected)stringResource(R.string.connected) else stringResource(R.string.manual),fontSize=12.sp,color=Purple)};TextButton(onClick={value=team.name;dialog=team.id}){Text(stringResource(R.string.edit))};TextButton(onClick={vm.remove(team.id)}){Text("×")}};Row{TextButton(onClick={vm.move(s.teams.indexOf(team),-1)}){Text("↑")};TextButton(onClick={vm.move(s.teams.indexOf(team),1)}){Text("↓")}}}}}};OutlinedButton(onClick={value="";dialog="add"},Modifier.fillMaxWidth()){Text(stringResource(R.string.add_team))};OutlinedButton(onClick={vm.share()},Modifier.fillMaxWidth()){Text(if(vm.sharing)stringResource(R.string.stop_sharing) else stringResource(R.string.share_game))};if(vm.sharing)Text(stringResource(R.string.sharing_room,s.gameName),color=Purple);if(vm.isSyncing)Text(stringResource(R.string.syncing_words),color=Coral);Button(onClick={vm.start()},enabled=s.teams.size>=2&&!vm.isSyncing,modifier=Modifier.fillMaxWidth().height(56.dp)){Text(stringResource(R.string.start_game))};if(s.teams.size<2)Text(stringResource(R.string.need_two_teams),color=Coral)}
 if(dialog!=null)AlertDialog(onDismissRequest={dialog=null},confirmButton={TextButton(onClick={if(dialog=="add") vm.add(value) else vm.rename(dialog!!,value);dialog=null}){Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick={dialog=null}){Text(stringResource(R.string.cancel))}},text={OutlinedTextField(value,{value=it},label={Text(stringResource(R.string.team_name))})})
}
@Composable private fun ClientLobby(vm:AppViewModel){Page(stringResource(R.string.join_game),{vm.home()}){Text(if(vm.clientConnected)stringResource(R.string.connected) else stringResource(R.string.waiting_admin));Text(stringResource(R.string.waiting_admin),fontSize=22.sp);Spacer(Modifier.weight(1f))}}
@Composable private fun Join(vm:AppViewModel){var selected by remember{mutableStateOf(false)};var team by remember{mutableStateOf("")};var drag by remember{mutableFloatStateOf(0f)};LaunchedEffect(vm.isDiscovering){if(vm.isDiscovering){delay(5_000);if(vm.discovered.isEmpty())vm.isDiscovering=false}}
 Page(stringResource(R.string.join_game),{vm.transport.stopDiscovery();vm.isDiscovering=false;vm.screen=Screen.HOME}){Column(Modifier.fillMaxSize().pointerInput(Unit){detectVerticalDragGestures(onDragEnd={if(drag>80)vm.refreshDiscovery();drag=0f}){_,amount->if(amount>0)drag+=amount}},verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(if(vm.isDiscovering)stringResource(R.string.searching)else stringResource(R.string.found_game),Modifier.weight(1f),fontWeight=FontWeight.Bold);TextButton(onClick={vm.refreshDiscovery()}){Text(stringResource(R.string.refresh))}};if(vm.isDiscovering)LinearProgressIndicator(Modifier.fillMaxWidth());if(!vm.isDiscovering&&vm.discovered.isEmpty())Text(stringResource(R.string.no_games));vm.discovered.forEach{room->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(room.name,Modifier.weight(1f));Button(onClick={vm.connect(room);selected=true}){Text(stringResource(R.string.join))}}}};if(selected&&vm.transferStatus==WordTransferStatus.IDLE){OutlinedTextField(team,{team=it;vm.clientTeamName=it},label={Text(stringResource(R.string.join_team_name_label))},modifier=Modifier.fillMaxWidth());Button(onClick={vm.submitClientTeam()},enabled=vm.clientConnected&&team.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.join))}};if(vm.transferStatus==WordTransferStatus.RECEIVING){Text(stringResource(R.string.receiving_words),fontWeight=FontWeight.Bold);LinearProgressIndicator({if(vm.transferTotal==0)0f else vm.transferReceived.toFloat()/vm.transferTotal},Modifier.fillMaxWidth());Text(stringResource(R.string.received_words,vm.transferReceived,vm.transferTotal))};if(vm.transferStatus==WordTransferStatus.COMPLETE)Text(stringResource(R.string.words_ready),color=Color(0xFF14805E),fontWeight=FontWeight.Bold);if(vm.transferStatus==WordTransferStatus.FAILED){Text(stringResource(R.string.transfer_failed),color=Color.Red);Button(onClick={vm.retryWordTransfer()}){Text(stringResource(R.string.retry))}}}}}

@Composable private fun ProtestDialog(vm:AppViewModel){
 val s=vm.session?:return
 val protest=s.protestState?:return
 if(protest.status!=ProtestStatus.ACTIVE)return
 val isActivePhone=vm.isActiveTeamPhone()
 if(!isActivePhone)return
 val round=s.currentRound?:return
 val word=round.words.firstOrNull{it.id==protest.wordId}?:return
 AlertDialog(
  onDismissRequest={},
  title={Text(stringResource(R.string.word_protested),fontWeight=FontWeight.Bold)},
  text={Text(word.text,fontSize=18.sp,fontWeight=FontWeight.Bold)},
  confirmButton={TextButton(onClick={vm.admitProtest()}){Text(stringResource(R.string.admit_wrong),color=Coral)}},
  dismissButton={TextButton(onClick={vm.cancelProtest()}){Text(stringResource(R.string.cancel))}}
 )
}

@Composable private fun HintDialog(vm:AppViewModel){
 val wordId=vm.hintWordId?:return
 AlertDialog(
  onDismissRequest={vm.dismissHint()},
  title={Text(stringResource(R.string.hint),fontWeight=FontWeight.Bold)},
  text={Text(vm.hintText?:stringResource(R.string.no_hint),fontSize=16.sp)},
  confirmButton={TextButton(onClick={vm.dismissHint()}){Text(stringResource(R.string.close))}}
 )
}

@Composable private fun Round(vm:AppViewModel){
 val s=vm.session?:return;val r=s.currentRound?:return
 var remaining by remember(r.roundId){mutableIntStateOf(((r.endsAtMillis-System.currentTimeMillis())/1000).toInt().coerceAtLeast(0))}
 val timerEnded=remaining==0
 LaunchedEffect(r.roundId){while(remaining>0){delay(250);remaining=((r.endsAtMillis-System.currentTimeMillis()+999)/1000).toInt().coerceAtLeast(0)};if(vm.isHost)vm.endRound()}
 val team=s.teams.first{it.id==r.teamId};val playerControls=vm.canCorrect()&&!timerEnded;val active=r.words.firstOrNull{it.isActive}
 ProtestDialog(vm)
 HintDialog(vm)
 Page(stringResource(R.string.round)){
  Card(colors=CardDefaults.cardColors(containerColor=teamColor(s.currentTeamIndex)),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(stringResource(R.string.active_team,team.name),fontSize=17.sp,fontWeight=FontWeight.Black,color=Purple);Text(stringResource(R.string.score,team.score),fontSize=14.sp,fontWeight=FontWeight.Bold)};Text(stringResource(R.string.time,remaining),fontSize=27.sp,fontWeight=FontWeight.Black,color=Coral)}}
  LinearProgressIndicator({remaining.toFloat()/s.settings.roundSeconds},Modifier.fillMaxWidth().height(7.dp),color=Coral)
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp),userScrollEnabled=false){items(r.words.sortedBy{it.boardIndex},key={it.id}){w->
   val isProtested=s.protestState?.wordId==w.id&&s.protestState?.status==ProtestStatus.ACTIVE
   BoardWordCard(w,r.phase,playerControls,timerEnded,isProtested,{if(r.phase==RoundPhase.UNGUESSED_REPLAY&&w.status==WordStatus.SKIPPED)vm.selectSkipped(w.id)},{vm.protest(w.id)},vm.canProtest(w.id),{vm.requestHint(w.id,w.text)})
  }}
  if(playerControls&&active!=null){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={vm.correct()},modifier=Modifier.weight(0.85f).height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF19A974))){Text(stringResource(R.string.correct),fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1)};OutlinedButton(onClick={vm.skip()},modifier=Modifier.weight(1.15f).height(52.dp)){Text(stringResource(R.string.skip),fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=1)}}}
 }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun BoardWordCard(word:RoundWord,phase:RoundPhase,playerControls:Boolean,timerEnded:Boolean,isProtested:Boolean,onSelect:()->Unit,onProtest:()->Unit,canProtest:Boolean,onHintRequest:()->Unit={}){
 var lit by remember(word.id,word.isActive,word.shouldBlink,timerEnded){mutableStateOf(word.shouldBlink&&!timerEnded)}
 LaunchedEffect(word.id,word.isActive,word.shouldBlink,timerEnded){
  if(timerEnded){lit=false;return@LaunchedEffect}
  if(word.isActive&&word.blinkCount==2){repeat(4){delay(170);lit=!lit};lit=false}
  else if(phase==RoundPhase.UNGUESSED_REPLAY&&word.shouldBlink){while(true){delay(450);lit=!lit}}
  else lit=false
 }
 val isAdmittedWrong=word.status==WordStatus.FINAL_WRONG
 val background by animateColorAsState(
  if(isProtested)Color(0xFFFFE0B2)
  else if(lit)Color(0xFFFFD166)
  else if(word.isBlurred)Color(0xFFD9D5E8)
  else if(word.status==WordStatus.CORRECT||word.status==WordStatus.FINAL_CORRECT)Color(0xFFD9F5E8)
  else if(isAdmittedWrong)Color(0xFFFFEBEE)
  else Color.White,
  label="board-card"
 )
 Card(
  Modifier.fillMaxWidth().combinedClickable(enabled=playerControls&&phase==RoundPhase.UNGUESSED_REPLAY&&word.status==WordStatus.SKIPPED,onClick=onSelect,onDoubleClick={if(word.isActive)onHintRequest()}),
  colors=CardDefaults.cardColors(containerColor=background),
  elevation=CardDefaults.cardElevation(if(word.isActive)5.dp else 0.dp)
 ){Row(Modifier.padding(horizontal=9.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
  Text("${word.boardIndex+1}",fontSize=12.sp,color=Purple,fontWeight=FontWeight.Bold)
  Spacer(Modifier.width(7.dp))
  Text(
   if(word.isBlurred)"••••••" else word.text,
   Modifier.weight(1f),
   fontSize=if(word.isActive)17.sp else 14.sp,
   fontWeight=if(word.isActive)FontWeight.Black else FontWeight.Normal,
   maxLines=1,
   color=if(word.isBlurred)Color.Gray else Color(0xFF29233B),
   textDecoration=if(isAdmittedWrong)TextDecoration.LineThrough else TextDecoration.None
  )
  if(!word.isBlurred&&word.status!=WordStatus.SHOWN)Text(statusLabel(word.status),fontSize=10.sp,color=if(isAdmittedWrong)Color.Red else Purple)
  if(isProtested)Text("⚑",fontSize=12.sp,color=Coral)
  if(canProtest&&!word.isBlurred)TextButton(onClick=onProtest,contentPadding=PaddingValues(horizontal=4.dp)){Text(stringResource(R.string.protest),fontSize=10.sp,maxLines=1,color=Coral)}
 }}
}

@Composable private fun statusLabel(status:WordStatus)=when(status){
 WordStatus.CORRECT,WordStatus.FINAL_CORRECT->stringResource(R.string.status_correct)
 WordStatus.SKIPPED->stringResource(R.string.status_skipped)
 WordStatus.DISPUTED->stringResource(R.string.status_disputed)
 WordStatus.FINAL_WRONG->stringResource(R.string.not_counted)
 WordStatus.NOT_COUNTED->stringResource(R.string.status_wrong)
 WordStatus.SHOWN->""
}

@Composable private fun WordRow(w:RoundWord,onProtest:()->Unit,canProtest:Boolean){
 val isAdmittedWrong=w.status==WordStatus.FINAL_WRONG
 val color by animateColorAsState(
  if(isAdmittedWrong)Color(0xFFFFEBEE) else if(w.wasSkipped)Color(0xFFFFF0D6) else Color.White,
  label="review-row"
 )
 Row(Modifier.fillMaxWidth().padding(vertical=3.dp).background(color,RoundedCornerShape(14.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
  Text(
   w.text,Modifier.weight(1f),
   fontWeight=FontWeight.Bold,
   textDecoration=if(isAdmittedWrong)TextDecoration.LineThrough else TextDecoration.None,
   color=if(isAdmittedWrong)Color.Gray else Color.Unspecified
  )
  Text(statusLabel(w.status),fontSize=12.sp,color=if(isAdmittedWrong)Color.Red else Purple)
  if(canProtest&&w.status==WordStatus.CORRECT)TextButton(onClick=onProtest){Text(stringResource(R.string.protest),color=Coral)}
 }
}

@Composable private fun Review(vm:AppViewModel){
 val s=vm.session?:return;val r=s.currentRound?:return;val team=s.teams.first{it.id==r.teamId}
 ProtestDialog(vm)
 Page(stringResource(R.string.round_review)){
  Text(team.name,fontSize=24.sp,fontWeight=FontWeight.Bold)
  LazyColumn(Modifier.weight(1f)){items(r.words){w->WordRow(w,{vm.protest(w.id)},vm.canProtest(w.id))}}
  Text(stringResource(R.string.score,team.score))
  if(vm.isHost)BigButton(stringResource(R.string.next_team)){vm.next()}else Text(stringResource(R.string.waiting_admin))
 }
}

@Composable private fun Scoreboard(vm:AppViewModel){val s=vm.session?:return;Page(stringResource(R.string.scoreboard),if(vm.isHost){{vm.screen=Screen.REVIEW}}else null){if(s.finishing)Text(stringResource(R.string.finishing_mode),color=Coral,fontWeight=FontWeight.Bold);s.teams.sortedByDescending{it.score}.forEachIndexed{i,t->Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=teamColor(i))){Row(Modifier.padding(16.dp)){Text("${i+1}. ${t.name}",Modifier.weight(1f),fontWeight=FontWeight.Bold);Text("${t.score} · ${stringResource(R.string.rounds_played,t.roundsPlayed)}")}}};Spacer(Modifier.weight(1f));if(vm.isHost)BigButton(stringResource(R.string.start_game)){vm.beginNext()}else Text(stringResource(R.string.waiting_admin))}}
@Composable private fun GameOver(vm:AppViewModel){val s=vm.session?:return;val w=s.teams.first{it.id==s.winnerTeamId};Page(stringResource(R.string.game_over)){Text("🏆",fontSize=72.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center);Text(stringResource(R.string.winner,w.name),fontSize=30.sp,fontWeight=FontWeight.Black,color=Purple);s.teams.sortedByDescending{it.score}.forEach{Text("${it.name}: ${it.score} (${stringResource(R.string.rounds_played,it.roundsPlayed)})")};Spacer(Modifier.weight(1f));BigButton(stringResource(R.string.new_same_teams)){vm.resetSame()};OutlinedButton(onClick={vm.home()},Modifier.fillMaxWidth()){Text(stringResource(R.string.home))}}}
@Composable private fun Settings(vm:AppViewModel){
 val context=LocalContext.current
 Page(stringResource(R.string.settings),{vm.screen=Screen.HOME}){
  Text(stringResource(R.string.language),fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("hy" to "Հայերեն","ru" to "Русский","en" to "English").forEach{(tag,label)->FilterChip(false,{context.getSharedPreferences("alias-ka",0).edit().putString("language",tag).apply();applyLanguage(context,tag);(context as? ComponentActivity)?.recreate()},label={Text(label)})}}
  Row(verticalAlignment=Alignment.CenterVertically){Text("${stringResource(R.string.round_seconds)}: ${vm.roundSeconds}",Modifier.weight(1f));TextButton(onClick={vm.updateRoundSeconds(vm.roundSeconds-5)}){Text("−")};TextButton(onClick={vm.updateRoundSeconds(vm.roundSeconds+5)}){Text("+")}}
  Row(verticalAlignment=Alignment.CenterVertically){Text("${stringResource(R.string.target_score)}: ${vm.targetScore}",Modifier.weight(1f));TextButton(onClick={vm.updateTargetScore(vm.targetScore-5)}){Text("−")};TextButton(onClick={vm.updateTargetScore(vm.targetScore+5)}){Text("+")}}
  Spacer(Modifier.weight(1f))
  TextButton(onClick={vm.screen=Screen.DIAGNOSTICS}){Text(stringResource(R.string.diagnostics))};Text("${stringResource(R.string.version,BuildConfig.VERSION_NAME)} (${BuildConfig.VERSION_CODE})",modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=Color.Gray,fontSize=12.sp)
 }
}
@Composable private fun About(vm:AppViewModel){Page(stringResource(R.string.about),{vm.screen=Screen.HOME}){Text("Alias-ka",fontSize=38.sp,fontWeight=FontWeight.Black,color=Purple);Text(stringResource(R.string.version,BuildConfig.VERSION_NAME));Text(stringResource(R.string.tagline))}}
@Composable private fun Diagnostics(vm:AppViewModel){
 val context=LocalContext.current;val selection=selectDiagnosticLogs(vm.logs);val hasErrors=selection.section==DiagnosticSection.ERRORS
 var showClearConfirm by remember{mutableStateOf(false)}
 val state=when(vm.session?.state){GameState.LOBBY->stringResource(R.string.teams);GameState.ROUND_RUNNING->stringResource(R.string.round);GameState.ROUND_REVIEW->stringResource(R.string.round_review);GameState.GAME_OVER->stringResource(R.string.game_over);else->stringResource(R.string.not_active)}
 val screenName=when(vm.screen){Screen.HOME->stringResource(R.string.home);Screen.CREATE->stringResource(R.string.create_game);Screen.LOBBY->stringResource(R.string.teams);Screen.JOIN->stringResource(R.string.join_game);Screen.ROUND->stringResource(R.string.round);Screen.REVIEW->stringResource(R.string.round_review);Screen.SCOREBOARD->stringResource(R.string.scoreboard);Screen.SETTINGS->stringResource(R.string.settings);Screen.ABOUT->stringResource(R.string.about);Screen.GAME_OVER->stringResource(R.string.game_over);Screen.DIAGNOSTICS->stringResource(R.string.diagnostics)}
 val modeStr=when(vm.clientMode){ClientMode.HOST->stringResource(R.string.host_mode);ClientMode.CONNECTED->stringResource(R.string.client_mode);ClientMode.JOINING->stringResource(R.string.joining_mode);ClientMode.DISCONNECTED->stringResource(R.string.disconnected_mode);ClientMode.IDLE->stringResource(R.string.idle_mode)}
 val report="Alias-ka Diagnostics\nVersion: ${BuildConfig.VERSION_NAME}\nBuild: ${BuildConfig.VERSION_CODE}\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nMode: $modeStr\nGame state: $state\nActive screen: $screenName\nWord queue: ${vm.syncQueueSize} (supabase)\nConnection: ${vm.clientConnected}\nSession: ${vm.activeSessionId?:"none"}\nHost endpoint: ${vm.hostEndpointId?:"none"}\nPermissions: ${NearbyPermissionPolicy.requiredRuntime(Build.VERSION.SDK_INT).joinToString{it.substringAfterLast('.')+"="+(context.checkSelfPermission(it)==PackageManager.PERMISSION_GRANTED)}}\nLogs:\n${vm.logs.joinToString("\n")}"
 Page(stringResource(R.string.diagnostics),{vm.screen=Screen.SETTINGS}){Text("${stringResource(R.string.version,BuildConfig.VERSION_NAME)} · ${stringResource(R.string.build_number,BuildConfig.VERSION_CODE)}");Text(stringResource(R.string.language)+": "+Locale.getDefault().displayLanguage);Text(stringResource(R.string.active_word_count,vm.syncQueueSize)+" · supabase");Text(stringResource(R.string.mode_label,modeStr));Text(stringResource(R.string.connection_label,stringResource(if(vm.clientConnected)R.string.connected else R.string.disconnected)));vm.activeSessionId?.let{Text(stringResource(R.string.session_id_label,it),fontSize=12.sp)};vm.hostEndpointId?.let{Text(stringResource(R.string.host_endpoint_label,it),fontSize=12.sp)};Text(stringResource(R.string.teams_count,vm.session?.teams?.size?:0));Text(stringResource(R.string.state_label,state));Text(stringResource(R.string.screen_label,screenName),fontSize=12.sp);Row{if(hasErrors)TextButton(onClick={copyText(context,"Alias-ka errors",allErrorsCopy(report,selection.logs))}){Text(stringResource(R.string.copy_all_errors))};TextButton(onClick={copyText(context,"Alias-ka diagnostics",report)}){Text(stringResource(R.string.copy_diagnostics))};TextButton(onClick={showClearConfirm=true}){Text(stringResource(R.string.clear_logs))}};Text(stringResource(if(hasErrors)R.string.errors_heading else R.string.info_logs_heading),fontWeight=FontWeight.Black,color=if(hasErrors)Color.Red else Purple);LazyColumn{items(selection.logs){entry->Row(verticalAlignment=Alignment.CenterVertically){Text(entry,Modifier.weight(1f),color=if(entry.startsWith("ERROR"))Color.Red else Color.DarkGray,fontSize=11.sp);if(entry.startsWith("ERROR"))TextButton(onClick={copyText(context,"Alias-ka error",technicalErrorCopy(report,entry))}){Text(stringResource(R.string.copy))}}}}}
 if(showClearConfirm)AlertDialog(onDismissRequest={showClearConfirm=false},confirmButton={TextButton(onClick={vm.clearLogs();showClearConfirm=false}){Text(stringResource(R.string.clear))}},dismissButton={TextButton(onClick={showClearConfirm=false}){Text(stringResource(R.string.cancel))}},text={Text(stringResource(R.string.clear_logs_confirm))})
}
