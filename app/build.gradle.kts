plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.ocr_poc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ocr_poc"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Jetpack Compose dependencies
    implementation("androidx.compose.ui:ui:1.5.0") // Jetpack Compose UI
    implementation("androidx.compose.material3:material3:1.1.0") // Material 3
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.7") // Compose Preview
    implementation("androidx.compose.runtime:runtime-livedata:1.4.7") // For LiveData and Compose integration
    implementation ("androidx.activity:activity-compose:1.6.1")
    implementation ("androidx.compose.ui:ui:1.3.0")
    implementation ("androidx.compose.material3:material3:1.0.0")
    implementation ("com.google.android.gms:play-services-vision:20.1.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")

    implementation ("androidx.compose.material:material:1.6.0")
    implementation ("androidx.compose.ui:ui-tooling:1.6.0")
    implementation ("androidx.compose.foundation:foundation:1.6.0")

    implementation ("io.coil-kt:coil-compose:2.3.0")


    implementation ("androidx.compose.material3:material3:1.1.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation ("androidx.activity:activity-compose:1.7.2")
    // Lottie dependencies for animations
    implementation(libs.lottie) // Lottie animations
    implementation("com.airbnb.android:lottie-compose:6.0.0") // Lottie for Compose

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.2")
    implementation(libs.datastore.preferences)

    implementation(libs.gson)
    implementation(libs.datastore.preferences)

    // Necessary for BiLSTM
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation (libs.entity.extraction.v1600beta2)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.play.services.mlkit.document.scanner)
    implementation(libs.coil.compose)
    implementation(libs.text.recognition)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(platform(libs.firebase.bom))
    implementation (libs.common)
    implementation(libs.firebase.analytics)
}