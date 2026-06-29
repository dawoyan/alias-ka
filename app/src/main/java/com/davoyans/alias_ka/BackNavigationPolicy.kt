package com.davoyans.alias_ka

enum class BackAction { SHOW_EXIT_HINT, EXIT_APP, GO_HOME, GO_SETTINGS, GO_LOBBY, GO_SCOREBOARD, BLOCK_ACTIVE_ROUND }
class BackNavigationPolicy(private val exitWindowMillis:Long=2_000) {
 private var lastHomeBack=Long.MIN_VALUE
 fun decide(screen:Screen,nowMillis:Long):BackAction {if(screen!=Screen.HOME)lastHomeBack=Long.MIN_VALUE;return when(screen){
  Screen.HOME->if(lastHomeBack!=Long.MIN_VALUE&&nowMillis-lastHomeBack<=exitWindowMillis)BackAction.EXIT_APP else {lastHomeBack=nowMillis;BackAction.SHOW_EXIT_HINT}
  Screen.SETTINGS,Screen.CREATE,Screen.JOIN,Screen.ABOUT->BackAction.GO_HOME
  Screen.DIAGNOSTICS->BackAction.GO_SETTINGS
  Screen.ROUND->BackAction.BLOCK_ACTIVE_ROUND
  Screen.REVIEW->BackAction.GO_SCOREBOARD
  Screen.SCOREBOARD,Screen.GAME_OVER->BackAction.GO_LOBBY
  Screen.LOBBY->BackAction.GO_HOME
 }}
}
