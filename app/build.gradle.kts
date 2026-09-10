import java.util.Properties

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

// Local-only debug "Test connection" shortcut (see DockScreen.kt) - reads
// app/dev.secrets.properties (git-ignored, see the .example file next to
// it). Absent by default: a fresh clone builds fine with all four fields
// empty, and the button simply doesn't appear. Never read from anywhere
// that would let these values leak into a committed file.
val devSecrets = Properties().apply {
    val file = rootProject.file("app/dev.secrets.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun devSecret(key: String): String = devSecrets.getProperty(key, "")

android {
    namespace = "si.apartmamatevz.cmcompanion"
    compileSdk = 34

    defaultConfig {
        applicationId = "si.apartmamatevz.cmcompanion"
        minSdk = 26
        targetSdk = 34
        versionCode = gitCommitCount
        versionName = "0.2.2"

        buildConfigField("String", "DEV_BRIDGE_URL", "\"${devSecret("DEV_BRIDGE_URL")}\"")
        buildConfigField("String", "DEV_INSTALLATION_ID", "\"${devSecret("DEV_INSTALLATION_ID")}\"")
        buildConfigField("String", "DEV_DEVICE_TOKEN", "\"${devSecret("DEV_DEVICE_TOKEN")}\"")
        buildConfigField("String", "DEV_DEVICE_LABEL", "\"${devSecret("DEV_DEVICE_LABEL")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
