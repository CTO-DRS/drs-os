// DRS OS Automation — root build (SPDX-License-Identifier: Apache-2.0)
//
// NOTE (Phase 4, D-1 fix): the shared component subprojects apply
// `id("org.jetbrains.kotlin.jvm")` WITHOUT a version. The plugin jar is
// already on this build's classpath via the kotlin-android/kotlin-compose
// declarations below (same Kotlin 2.0.21 release, enforced by
// build/scripts/check_phase3_code.sh). A versioned request in a
// subproject fails with "already on the classpath with an unknown
// version", and `alias(libs.plugins.kotlin.jvm)` must NOT be used in
// build scripts: with the catalog entry absent the generated accessor
// collides with ToolchainManagement.jvm (observed on Gradle 8.9).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
