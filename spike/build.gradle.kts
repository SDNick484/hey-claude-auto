plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.sdnick484.heyclaude.spike"
    // androidx.core 1.19 refuses to build against anything older than API 37.
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.sdnick484.heyclaude.spike"
        // The only target is a Pixel 9, which shipped with Android 14 (API 34).
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "m0"
    }

    // Two ways of reaching Claude from the car, installable side by side so one
    // head-unit session can compare them.
    flavorDimensions += "entry"
    productFlavors {
        // A Car App Library screen listing the conversation, plus notifications.
        create("templated") {
            dimension = "entry"
            applicationIdSuffix = ".t"
        }
        // Messaging notifications only. No Car App Library code at all.
        create("notif") {
            dimension = "entry"
            applicationIdSuffix = ".n"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Robolectric needs the merged manifest and resources.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core)
    "templatedImplementation"(libs.androidx.car.app)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    "testTemplatedImplementation"(libs.androidx.car.app.testing)
}
