/*
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.drs.os.drssettings"
    compileSdk = 35   // standalone-app choice; AOSP integration level confirmed at checkout

    defaultConfig {
        applicationId = "com.drs.os.drssettings"
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
}

dependencies {
    // Composite included-build coordinate (auto-substituted; Phase 4 D-1 fix).
    implementation("com.drs.os:foundation:0.3.0")
    // Composite included-build coordinate (auto-substituted; Phase 4 D-1 fix).
    implementation("com.drs.os:settings-core:0.3.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // appcompat powers per-app locales (AppCompatDelegate) — the
    // documented, supported mechanism for language selection.
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
