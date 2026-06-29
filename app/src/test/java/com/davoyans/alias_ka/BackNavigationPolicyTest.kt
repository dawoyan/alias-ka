package com.davoyans.alias_ka

import org.junit.Assert.assertEquals
import org.junit.Test

class BackNavigationPolicyTest {
 @Test fun `settings back returns home without exit`(){assertEquals(BackAction.GO_HOME,BackNavigationPolicy().decide(Screen.SETTINGS,0))}
 @Test fun `game lobby back returns home and round is blocked`(){val p=BackNavigationPolicy();assertEquals(BackAction.GO_HOME,p.decide(Screen.LOBBY,0));assertEquals(BackAction.BLOCK_ACTIVE_ROUND,p.decide(Screen.ROUND,1))}
 @Test fun `first home back hints and second exits in window`(){val p=BackNavigationPolicy();assertEquals(BackAction.SHOW_EXIT_HINT,p.decide(Screen.HOME,1_000));assertEquals(BackAction.EXIT_APP,p.decide(Screen.HOME,2_500))}
 @Test fun `late second home back shows hint again`(){val p=BackNavigationPolicy();p.decide(Screen.HOME,1_000);assertEquals(BackAction.SHOW_EXIT_HINT,p.decide(Screen.HOME,4_000))}
 @Test fun `diagnostics back returns settings`(){assertEquals(BackAction.GO_SETTINGS,BackNavigationPolicy().decide(Screen.DIAGNOSTICS,0))}
 @Test fun `join back goes home and active round never exits`(){val p=BackNavigationPolicy();assertEquals(BackAction.GO_HOME,p.decide(Screen.JOIN,0));assertEquals(BackAction.BLOCK_ACTIVE_ROUND,p.decide(Screen.ROUND,1));assertEquals(BackAction.BLOCK_ACTIVE_ROUND,p.decide(Screen.ROUND,2))}
}
