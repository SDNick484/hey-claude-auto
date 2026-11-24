plugins {
    // Kotlin support is built into the Android Gradle plugin since 9.0, so there is
    // no separate Kotlin plugin to apply.
    alias(libs.plugins.android.application) apply false
}
