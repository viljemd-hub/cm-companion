plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Kotlin 2.0+ moved the Compose compiler out of the Kotlin Gradle
    // plugin itself into its own plugin - app/build.gradle.kts's old
    // composeOptions { kotlinCompilerExtensionVersion = ... } block only
    // worked pre-2.0 (2026-11-06 AGP/Kotlin bump, prompted by a newly
    // installed Android Studio release - Rabbit 2, build 262 - far enough
    // ahead of this project's old AGP 8.5.2/Kotlin 1.9.24 that its IDE
    // inspections intermittently lost track of Compose being enabled at
    // all, "Composable function call without Compose plugin" on ~140
    // lines, even though Gradle CLI builds were always correct).
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
