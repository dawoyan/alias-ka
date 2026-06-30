import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.davoyans.alias_ka"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.davoyans.alias_ka"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }
    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("RELEASE_STORE_FILE", ""))
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.register("copyAliasKaApk") {
    dependsOn("assembleDebug")
    doLast {
        val destination = file(System.getProperty("user.home") + "/android")
        destination.mkdirs()
        copy {
            from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
            into(destination)
            rename { "alias-ka-07.apk" }
        }
        logger.lifecycle("Alias-ka APK copied to ~/android/alias-ka-07.apk")
    }
}

tasks.register("copyAliasKaAab") {
    dependsOn("bundleRelease")
    doLast {
        val destination = file(System.getProperty("user.home") + "/android")
        destination.mkdirs()
        copy {
            from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
            into(destination)
            rename { "alias-ka-07.aab" }
        }
        logger.lifecycle("Alias-ka AAB copied to ~/android/alias-ka-07.aab")
    }
}
