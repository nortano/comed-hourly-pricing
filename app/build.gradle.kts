

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinSerialization)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.nortano.comedhourlypricing"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nortano.comedhourlypricing"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")
    implementation(libs.protolayout)
    implementation(libs.protolayout.material)
    implementation(libs.protolayout.expression)
    implementation(libs.wear.tiles)
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.horologist.tiles)
    implementation(libs.horologist.compose.layout)
    implementation(libs.work.runtime.ktx)
    debugImplementation(libs.wear.tiles.tooling)
    debugImplementation(libs.wear.tiles.tooling.preview)
    debugImplementation(libs.wear.tooling.preview)


    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
