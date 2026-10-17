// SPDX-License-Identifier: Apache-2.0
// Shared pure-Kotlin DRS OS component, consumed via composite builds
// (includeBuild) by packages/apps/*. Kept as its own Gradle build so a
// single source location serves all app builds; run its tests directly
// with `gradle test` here, or SDK-independently via
// build/scripts/run_core_tests.sh.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
// engine-core uses foundation (MiniJson, KvStore) — composite dependency.
includeBuild("../foundation") {
    dependencySubstitution {
        substitute(module("com.drs.os:foundation")).using(project(":"))
    }
}

rootProject.name = "engine-core"
