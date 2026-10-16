/*
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.drs.os.securitycenter"

    // compileSdk 35 is the STANDALONE-app choice (stable public API).
    // The AOSP integration API level comes from the actual platform tree
    // at integration time and is confirmed separately (ADR-0001) — do
    // not treat this number as the platform target.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drs.os.securitycenter"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.3.0-phase3"
        // Arabic-first: ar is the default (values/), en is the explicit
        // alternative. Restrict shipped locales to the supported set.
        resourceConfigurations += listOf("ar", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // SECURITY: production releases are signed only with the
            // release key ceremony (docs/security/key-management.md).
            // Debug keys are never used for production artifacts.
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
}

dependencies {
    implementation(project(":foundation"))
    implementation(project(":security-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
