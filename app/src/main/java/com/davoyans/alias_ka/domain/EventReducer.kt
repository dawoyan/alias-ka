package com.davoyans.alias_ka.domain

data class SequencedEvent(val eventId:String,val sequenceNumber:Long,val type:String)
sealed interface EventResult { data class Applied(val nextExpected:Long):EventResult; data object Duplicate:EventResult; data class SequenceGap(val expected:Long,val received:Long):EventResult }
class HostEventReducer(startSequence:Long=1) {
 private val seen=mutableSetOf<String>(); private var expected=startSequence
 fun accept(event:SequencedEvent):EventResult {
  if(event.eventId in seen||event.sequenceNumber<expected)return EventResult.Duplicate
  if(event.sequenceNumber>expected)return EventResult.SequenceGap(expected,event.sequenceNumber)
  seen+=event.eventId;expected++;return EventResult.Applied(expected)
 }
}
