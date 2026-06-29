package com.davoyans.alias_ka

import java.security.MessageDigest

fun wordChecksum(words:List<String>)=MessageDigest.getInstance("SHA-256").digest(words.joinToString("\n").toByteArray()).joinToString(""){"%02x".format(it)}
data class WordTransferVerification(val complete:Boolean,val words:List<String> = emptyList(),val error:String?=null)
class WordTransferAssembler {
 private var total=0;private var chunks=0;private var checksum="";private val received=mutableMapOf<Int,List<String>>()
 fun begin(totalWords:Int,totalChunks:Int,expectedChecksum:String){total=totalWords;chunks=totalChunks;checksum=expectedChecksum;received.clear()}
 fun add(index:Int,words:List<String>){if(index in 0 until chunks)received[index]=words}
 fun verify():WordTransferVerification {if(received.size!=chunks)return WordTransferVerification(false);val words=(0 until chunks).flatMap{received[it].orEmpty()};return if(words.size==total&&wordChecksum(words)==checksum)WordTransferVerification(true,words)else WordTransferVerification(false,error="checksum/count mismatch")}
}
