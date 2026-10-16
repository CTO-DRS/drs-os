// SPDX-License-Identifier: Apache-2.0
// DRS OS Settings — standalone app (Phase 3)
//
// Build status: SOURCE IMPLEMENTED — BUILD NOT VERIFIED (no Android SDK
// in the Phase 3 sandbox). See docs/phase3/BUILD_AND_TEST.md.
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
rootProject.name = "DRSSettings"

include(":app")

include(":foundation")
project(":foundation").projectDir = file("../../../components/foundation")
include(":settings-core")
project(":settings-core").projectDir = file("../../../components/settings-core")
