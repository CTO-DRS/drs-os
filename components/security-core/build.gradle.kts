/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * security-core — 4-state security checklist model (PASS / WARNING /
 * UNAVAILABLE / NOT CHECKED) with presentation mapping. Pure Kotlin;
 * no Android dependencies.
 *
 * Included as a subproject by the DRS app builds (see
 * packages/apps/<app>/settings.gradle.kts) and also compiled
 * SDK-independently by build/scripts/run_core_tests.sh (kotlinc +
 * JUnit console), which remains the canonical no-SDK test path.
 *
 * NOTE (Phase 4, defect D-1 fix): this component is consumed as a
 * COMPOSITE included build, not as a projectDir subproject — projectDir
 * subprojects outside the consumer's root dir expose no variants on
 * Gradle 8.9 ("No variants exist" at consumption time, verified
 * empirically). The literal plugin version is required here because an
 * included build has its own classpath; it MUST stay in lockstep with
 * the app version catalogs (build/scripts/check_phase3_code.sh checks).
 * group/name below power the automatic dependency substitution.

 */
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
}

group = "com.drs.os"
version = "0.3.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
