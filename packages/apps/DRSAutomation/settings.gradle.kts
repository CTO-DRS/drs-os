// SPDX-License-Identifier: Apache-2.0
// DRS OS Automation — standalone app (Phase 3)
//
// Build status: SOURCE IMPLEMENTED — BUILD NOT VERIFIED in the Phase 3
// sandbox (no Android SDK). Prerequisites: JDK 17, Android SDK platform
// 35 + build-tools, AGP 8.7.3 via Gradle 8.9+ (docs/phase3/BUILD_AND_TEST.md).
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
rootProject.name = "DRSAutomation"

include(":app")

include(":foundation")
project(":foundation").projectDir = file("../../../components/foundation")
include(":engine-core")
project(":engine-core").projectDir = file("../../../components/engine-core")
