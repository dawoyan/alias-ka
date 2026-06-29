package com.davoyans.alias_ka

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsPolicyTest {
 @Test fun `errors are shown first when present`(){val r=selectDiagnosticLogs(listOf("INFO ready","ERROR failed","WARN retry"));assertEquals(DiagnosticSection.ERRORS,r.section);assertEquals(listOf("ERROR failed"),r.logs)}
 @Test fun `info section is used without errors`(){val r=selectDiagnosticLogs(listOf("INFO ready","WARN nearby"));assertEquals(DiagnosticSection.INFO,r.section);assertEquals(2,r.logs.size)}
}
