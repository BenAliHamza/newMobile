plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services) // Google Services plugin for Firebase
}

android {
    namespace = "tn.esprit.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "tn.esprit.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 11 for Firebase & modern AndroidX
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // UI + AndroidX
    implementation(libs.appcompat)
    implementation("com.google.android.material:material:1.11.0") // Material3 explicitly
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // MVVM / Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Firebase (managed by BoM: versions aligned automatically)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Google Sign-In (for FirebaseAuth with Google)
    implementation(libs.play.services.auth)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
