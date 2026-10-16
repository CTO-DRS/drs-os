// SPDX-License-Identifier: Apache-2.0
// DRS OS Security Center — standalone app (Phase 3)
//
// Build status: SOURCE IMPLEMENTED — BUILD NOT VERIFIED in the Phase 3
// sandbox (no Android SDK). Prerequisites to build: JDK 17, Android SDK
// platform 35 + build-tools, AGP 8.7.3 via Gradle 8.9+. See
// docs/phase3/BUILD_AND_TEST.md. This configuration must not be changed
// silently (docs/phase-plan.md policy).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "DRSSecurityCenter"

include(":app")

// Shared pure-Kotlin modules, tested in Phase 3 on the JVM.
include(":foundation")
project(":foundation").projectDir = file("../../../components/foundation")
include(":security-core")
project(":security-core").projectDir = file("../../../components/security-core")
