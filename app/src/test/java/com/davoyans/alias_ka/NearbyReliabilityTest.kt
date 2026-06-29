package com.davoyans.alias_ka

import org.junit.Assert.*
import org.junit.Test

class NearbyReliabilityTest {
 @Test fun `manifest checker detects missing wifi state`(){val declared=NearbyPermissionPolicy.requiredManifest-NearbyPermissionPolicy.ACCESS_WIFI_STATE;assertTrue(NearbyPermissionPolicy.ACCESS_WIFI_STATE in NearbyPermissionPolicy.missingManifest(declared))}
 @Test fun `runtime permission blocks then grant allows nearby`(){val required=NearbyPermissionPolicy.requiredRuntime(33);assertFalse(NearbyPermissionPolicy.canStart(33,required-NearbyPermissionPolicy.requiredRuntime(33).first()));assertTrue(NearbyPermissionPolicy.canStart(33,required))}
 @Test fun `word transfer remains incomplete until all chunks arrive`(){val words=listOf("Ա","Բ","Գ");val a=WordTransferAssembler();a.begin(3,2,wordChecksum(words));a.add(0,words.take(2));assertFalse(a.verify().complete);a.add(1,words.drop(2));assertEquals(words,a.verify().words)}
 @Test fun `client verification rejects stale or corrupt host words`(){val expected=listOf("host","words");val a=WordTransferAssembler();a.begin(2,1,wordChecksum(expected));a.add(0,listOf("stale","local"));val result=a.verify();assertFalse(result.complete);assertNotNull(result.error)}
 @Test fun `technical error copy contains diagnostics and full error`(){val copied=technicalErrorCopy("Version: 1.4.0\nDevice: test","ERROR Advertising failed: ACCESS_WIFI_STATE missing");assertTrue(copied.contains("Device: test"));assertTrue(copied.contains("ACCESS_WIFI_STATE missing"))}
 @Test fun `copy all includes every error but not info`(){val copied=allErrorsCopy("Alias-ka Diagnostics",listOf("ERROR one","INFO ok","ERROR two"));assertTrue(copied.contains("ERROR one"));assertTrue(copied.contains("ERROR two"));assertFalse(copied.contains("INFO ok"))}
}
