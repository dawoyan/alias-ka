package com.davoyans.alias_ka

enum class DiagnosticSection { ERRORS, INFO }
data class DiagnosticLogSelection(val section:DiagnosticSection,val logs:List<String>)
fun selectDiagnosticLogs(logs:List<String>):DiagnosticLogSelection {val errors=logs.filter{it.startsWith("ERROR")};return if(errors.isNotEmpty())DiagnosticLogSelection(DiagnosticSection.ERRORS,errors.takeLast(100))else DiagnosticLogSelection(DiagnosticSection.INFO,logs.takeLast(100))}
