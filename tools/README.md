# tools/ — Tool Inventory and Third-Party Dependency Policy

## Inventory (maintained; update on every change)

| Tool/dependency | Version | License | Source | Used by |
|---|---|---|---|---|
| bash | 5.2.37 (measured in sandbox) | GPL-3.0-or-later | system | validation scripts |
| git | 2.47.3 (measured) | GPL-2.0-or-later | system | version control |
| python3 | 3.12.15 (measured) | PSF-2.0 | system | XML/link validation + automation reference model (stdlib only) |
| OpenJDK JRE | 21.0.12.1 (measured) | GPL-2.0-with-classpath-exception | system | runs kotlinc + JUnit console |
| Kotlin compiler (authoring/test tool, not vendored) | 2.0.21 | Apache-2.0 | https://github.com/JetBrains/kotlin/releases/download/v2.0.21/kotlin-compiler-2.0.21.zip — SHA-256 0352c0a45bd22f80f6b26e485cd04da8047baa5de54865281fb9f89a4a7bcf2a (verified at download 2026-10-16) | compiles components/* and app-pure sources (build/scripts/run_core_tests.sh); installed OUTSIDE the repo at /home/z/my-project/tools/ |
| JUnit Platform console standalone (test tool, not vendored) | 1.10.2 | EPL-2.0 | https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar — SHA-256 a1de557821293ce903c213c694165fff532cf92081bac4238b9e05b35f04f43f (verified at download 2026-10-16) | runs core-module unit tests; test frameworks never ship in product artifacts |

Android build dependencies (AGP 8.7.3, Kotlin 2.0.21 plugin, Compose
BOM 2024.12.01, androidx artifacts — all Apache-2.0) are pinned in the
per-app `gradle/libs.versions.toml` catalogs; they are build-scope
declarations for a host with the Android SDK and were NOT downloaded in
the Phase 3 sandbox (no SDK present). Licenses are recorded in
docs/phase3/IMPLEMENTED_COMPONENTS.md §4.

No third-party code, fonts, icons, or libraries are vendored in this
repository. The tooling uses only the tools above (host-provided, not
redistributed by this repo). The automation reference model
(automation-reference/rule_engine.py) is project-authored, stdlib-only,
and pinned by unit tests in tests/automation/.

## Policy for future additions

1. Every added dependency (code, font, icon, asset) is registered here
   **before** commit: name, version, license, upstream source, reason,
   provenance verification.
2. Licenses must be compatible with Apache-2.0 distribution and the
   project's security posture; copyleft components are acceptable only in
   components whose license obligations are understood and recorded
   (e.g., GPL kernel comes via upstream AOSP, obligations noted).
3. **No AI/ML SDKs** (ADR-0004) — scanned automatically.
4. No dependency is added at build scope without a reproducible source
   (pinned URL + hash recorded); "latest" is not a version.
5. Fonts: candidates and their licenses are documented in
   branding/typography.md; none are redistributed yet — entries appear
   here only when binary inclusion actually happens.

## Phase 4 toolchain (installed 2026-10-17, outside the repository)

| Artifact | Version | Size (B) | Integrity / verification |
|---|---|---|---|
| Temurin JDK 17 (tarball, tools/jdk17.tar.gz) | 17.0.20.1+1 | 193,252,603 | `java -version` output recorded; downloaded via api.adoptium.net (provenance row added) |
| Gradle (tools/gradle-8.9-bin.zip) | 8.9 | 136,114,148 | SHA-256 verified against official `gradle-8.9-bin.zip.sha256` (sha256sum -c → OK) |
| Android cmdline-tools (android-sdk/, outside tools/) | 12.0 | 153,607,504 zip | dl.google.com artifact; `sdkmanager --version` = 12.0 |
| SDK packages (android-sdk/) | platform-35, build-tools 35.0.0, platform-tools | 458 MB installed | aapt2 2.19-11948202; licenses accepted |

Used for the Phase 4 builds recorded in docs/phase4/BUILD_RESULTS.md.
None of these are vendored into the repository or shipped in archives.

## Removal

Dependencies are removed when their consumer feature is removed; the
inventory row is deleted in the same change (no dead rows).
