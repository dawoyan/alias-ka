package com.davoyans.alias_ka

fun technicalErrorCopy(diagnostics:String,error:String)="$diagnostics\nSelected error:\n$error"
fun allErrorsCopy(diagnostics:String,logs:List<String>)="$diagnostics\nAll errors:\n${logs.filter{it.startsWith("ERROR")}.joinToString("\n")}"
