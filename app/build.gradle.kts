plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * versionCode must strictly increase for BOTH local upgrade-in-place
 * installs (adb/tap-to-update without uninstalling first, as long as the
 * signing key also matches - true by default for debug builds from the
 * same machine) and Play Store updates (which reject a non-increasing
 * versionCode outright). A hardcoded versionCode = 1 (this file's
 * original state) silently blocks both once you rebuild twice - derive
 * it from the git commit count instead, so every commit that changes app
 * code gets a fresh, monotonic number with no manual bookkeeping.
 * Falls back to 1 if git isn't available (e.g. a zip-exported source
 * tree with no .git directory).
 */
val gitCommitCount: Int = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    process.waitFor()
    process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1
}

android {
    namespace = "si.apartmamatevz.cmcompanion"
    compileSdk = 34

    defaultConfig {
        applicationId = "si.apartmamatevz.cmcompanion"
        minSdk = 26
        targetSdk = 34
        versionCode = gitCommitCount
        versionName = "0.3.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Chrome Custom Tabs (2026-09-10) - opening the admin panel via a
    // plain Intent.ACTION_VIEW launched a full separate browser task,
    // and a real user report found "no way back" to Companion from
    // there (system Back/Recents work, but nothing in the UI itself
    // suggests it). Custom Tabs stays visually branded as a tab but
    // keeps a clear back arrow that returns straight to the calling app.
    implementation("androidx.browser:browser:1.8.0")
    // QR pairing scanner (2026-09-10) - the deep link/QR *protocol* has
    // been locked in since v0.1 (bridge/PairingDeepLink.kt), but nothing
    // could actually scan a QR code yet, only tap a rendered link. Uses
    // zxing-android-embedded's ready-made scan Activity + ActivityResult
    // contract rather than hand-rolling a CameraX preview - a QR scanner
    // is a solved, boring problem, not something worth a custom UI here.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
