package com.davoyans.alias_ka.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

data class HostRoom(val name:String)
data class DiscoveredRoom(val endpointId:String,val name:String)
data class GameEvent(val type:String,val payload:String="")
data class GameSnapshot(val payload:String)
interface LocalGameTransport {
 fun startAdvertising(hostRoom:HostRoom); fun stopAdvertising(); fun startDiscovery(); fun stopDiscovery(); fun connect(endpointId:String); fun disconnect(); fun stopAllEndpoints(); fun sendEvent(event:GameEvent); fun sendEventTo(endpointId:String,event:GameEvent); fun sendSnapshot(snapshot:GameSnapshot)
}

class NearbyConnectionsGameTransport(context:Context, private val listener:Listener):LocalGameTransport {
 interface Listener { fun onAdvertisingStarted(){};fun onDiscoveryStarted(){};fun onRoom(room:DiscoveredRoom){}; fun onConnected(endpointId:String){}; fun onDisconnected(endpointId:String){}; fun onMessage(endpointId:String,message:String){}; fun onError(message:String){} }
 private val client=Nearby.getConnectionsClient(context); private val endpoints=linkedSetOf<String>(); private var pendingEndpoint:String?=null
 private val payloadCallback=object:PayloadCallback(){ override fun onPayloadReceived(id:String,p:Payload){ p.asBytes()?.let{listener.onMessage(id,String(it,StandardCharsets.UTF_8))} }; override fun onPayloadTransferUpdate(id:String,u:PayloadTransferUpdate)=Unit }
 private val lifecycle=object:ConnectionLifecycleCallback(){
  override fun onConnectionInitiated(id:String,info:ConnectionInfo){ client.acceptConnection(id,payloadCallback) }
  override fun onConnectionResult(id:String,result:ConnectionResolution){ if(result.status.isSuccess){endpoints+=id;listener.onConnected(id)}else listener.onError(result.status.statusMessage?:"Connection failed") }
  override fun onDisconnected(id:String){endpoints-=id;listener.onDisconnected(id)}
 }
 override fun startAdvertising(hostRoom:HostRoom){ client.startAdvertising(hostRoom.name,SERVICE_ID,lifecycle,AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()).addOnSuccessListener{listener.onAdvertisingStarted()}.addOnFailureListener{listener.onError("Advertising failed: ${it.javaClass.simpleName}: ${it.message}")} }
 override fun stopAdvertising(){client.stopAdvertising()}
 override fun startDiscovery(){client.startDiscovery(SERVICE_ID,object:EndpointDiscoveryCallback(){override fun onEndpointFound(id:String,info:DiscoveredEndpointInfo){listener.onRoom(DiscoveredRoom(id,info.endpointName))};override fun onEndpointLost(id:String)=Unit},DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()).addOnSuccessListener{listener.onDiscoveryStarted()}.addOnFailureListener{listener.onError("Discovery failed: ${it.javaClass.simpleName}: ${it.message}")} }
 override fun stopDiscovery(){client.stopDiscovery()}
 override fun connect(endpointId:String){pendingEndpoint=endpointId;client.requestConnection("Alias-ka",endpointId,lifecycle).addOnFailureListener{listener.onError(it.message?:"Connect failed")} }
 override fun disconnect(){stopAllEndpoints()}
 override fun stopAllEndpoints(){endpoints.forEach{client.disconnectFromEndpoint(it)};endpoints.clear();pendingEndpoint=null}
 override fun sendEvent(event:GameEvent)=send("EVENT|${event.type}|${event.payload}")
 override fun sendEventTo(endpointId:String,event:GameEvent){client.sendPayload(endpointId,Payload.fromBytes("EVENT|${event.type}|${event.payload}".toByteArray(StandardCharsets.UTF_8))).addOnFailureListener{listener.onError("Payload failed: ${it.message}")}}
 override fun sendSnapshot(snapshot:GameSnapshot)=send("SNAPSHOT|${snapshot.payload}")
 private fun send(value:String){if(endpoints.isNotEmpty())client.sendPayload(endpoints.toList(),Payload.fromBytes(value.toByteArray(StandardCharsets.UTF_8)))}
 companion object { const val SERVICE_ID="com.davoyans.alias_ka.NEARBY" }
}
