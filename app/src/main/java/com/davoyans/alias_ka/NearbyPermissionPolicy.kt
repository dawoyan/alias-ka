package com.davoyans.alias_ka

object NearbyPermissionPolicy {
 const val ACCESS_WIFI_STATE="android.permission.ACCESS_WIFI_STATE"
 val requiredManifest=setOf(ACCESS_WIFI_STATE,"android.permission.CHANGE_WIFI_STATE","android.permission.ACCESS_NETWORK_STATE")
 fun missingManifest(declared:Set<String>)=requiredManifest-declared
 fun requiredRuntime(api:Int,advertising:Boolean=false)=when {api>=33->setOf(if(advertising)"android.permission.BLUETOOTH_ADVERTISE" else "android.permission.BLUETOOTH_SCAN","android.permission.BLUETOOTH_CONNECT","android.permission.NEARBY_WIFI_DEVICES");api>=31->setOf(if(advertising)"android.permission.BLUETOOTH_ADVERTISE" else "android.permission.BLUETOOTH_SCAN","android.permission.BLUETOOTH_CONNECT");else->setOf("android.permission.ACCESS_FINE_LOCATION")}
 fun canStart(api:Int,granted:Set<String>,advertising:Boolean=false)=requiredRuntime(api,advertising).all{it in granted}
}
