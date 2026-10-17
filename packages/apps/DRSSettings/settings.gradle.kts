// SPDX-License-Identifier: Apache-2.0
// DRS OS Settings — standalone app (Phase 3)
//
// Build status: builds are verified in docs/phase4/BUILD_RESULTS.md
// (Phase 4 installed the real toolchain; Phase 3 had no Android SDK).
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

// Shared pure-Kotlin modules, tested on the JVM. Consumed as COMPOSITE
// included builds with explicit dependency substitution (Phase 4, D-1
// fix): projectDir subprojects outside the consumer's root dir expose
// no variants on Gradle 8.9 ("No variants exist", verified empirically).
// See components/*/build.gradle.kts for the full rationale.
includeBuild("../../../components/foundation") {
    dependencySubstitution {
        substitute(module("com.drs.os:foundation")).using(project(":"))
    }
}
includeBuild("../../../components/settings-core") {
    dependencySubstitution {
        substitute(module("com.drs.os:settings-core")).using(project(":"))
    }
}
