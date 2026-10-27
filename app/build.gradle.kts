import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.unscroll.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.unscroll.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "2.1.0"
    }

    // Release signing reads ../keystore.properties (private, not in git). Without it,
    // release builds are simply left unsigned.
    val keystoreFile = rootProject.file("keystore.properties")
    signingConfigs {
        if (keystoreFile.exists()) {
            val props = Properties().apply { keystoreFile.inputStream().use { load(it) } }
            create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    // Two versions of the app, from the same code:
    //  - github: everything, including the optional Reels/Shorts & website tracker (an Accessibility service).
    //  - play:   for the Play Store, without the tracker (Google reviews Accessibility use very strictly).
    // Same applicationId and signing key, so people can switch between them.
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            isDefault = true
            buildConfigField("boolean", "TRACKER_AVAILABLE", "true")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "TRACKER_AVAILABLE", "false")
        }
    }

    buildTypes {
        release {
            // Shrinks and optimises the app (removes unused code and resources).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true // gives us BuildConfig.DEBUG
    }

    // Keeping the app small:
    // - The app is in English, so only keep English text from the libraries we use
    //   (they ship their own words in ~90 languages).
    androidResources {
        localeFilters += "en"
    }
    packaging {
        resources {
            // Only needed for Kotlin reflection and coroutine debugging, which the app doesn't use.
            excludes += listOf("kotlin/**.kotlin_builtins", "DebugProbesKt.bin", "META-INF/*.version")
        }
        jniLibs {
            // DataStore's helper for sharing settings between processes; ScrollNah uses one process.
            excludes += "**/libdatastore_shared_counter.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    // Room (local database) + DataStore (small settings)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Splash screen (the opening screen) that looks the same on every Android version
    implementation(libs.androidx.core.splashscreen)


    // Unit tests for the pure logic (moods, break sessions, Bloomy's growth)
    testImplementation(libs.junit)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
