/*
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.drs.os.automation"
    compileSdk = 35   // standalone-app choice; AOSP integration level confirmed at checkout

    defaultConfig {
        applicationId = "com.drs.os.automation"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.3.0-phase3"
        resourceConfigurations += listOf("ar", "en")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            // Robolectric needs the merged resources for the no-device CI layer.
            isIncludeAndroidResources = true
            all { test ->
                test.maxHeapSize = "1024m"
            }
        }
    }
}

dependencies {
    // Composite included-build coordinate (auto-substituted; Phase 4 D-1 fix).
    implementation("com.drs.os:foundation:0.3.0")
    // Composite included-build coordinate (auto-substituted; Phase 4 D-1 fix).
    implementation("com.drs.os:engine-core:0.3.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Robolectric no-device CI layer (Phase 5): real-SharedPreferences
    // persistence + corrupted-recovery tests for the rule store.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    // Runs the JUnit4/Robolectric suites on the same JUnit Platform as the
    // Jupiter suites (useJUnitPlatform below).
    testRuntimeOnly(libs.junit.vintage.engine)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
