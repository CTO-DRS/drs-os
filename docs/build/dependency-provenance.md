# DRS OS Dependency & Source Provenance Policy

Status: policy (Phase 2). How DRS OS guarantees that every byte it adds —
and every byte it inherits — is traceable, licensed, and reviewable.

## 1. Upstream provenance

- The AOSP baseline is traceable to a **recorded manifest commit SHA**
  (ADR-0001, procedure: upstream-sync.md).
- Upstream license notices are never stripped; NOTICE records the
  foundation and trademark posture. Distributions re-ship upstream
  licenses verbatim.
- The authoritative source for any upstream component is the checked-out
  tree at the pinned SHA — not documentation, not memory. Where this
  repository describes upstream components, it marks symbol-level
  claims as design-level until re-verified against the tree.

## 2. DRS-added dependencies (future: code, fonts, icons, libraries)

Every addition is registered in `tools/README.md` **before** commit with:

| Field | Requirement |
|---|---|
| Name + version | exact; "latest" is not a version |
| Source | canonical upstream URL, archived copy reference |
| Integrity | hash of the exact artifact used (SHA-256) |
| License | SPDX id + license text shipped alongside |
| Reviewer + date | human attribution (no AI-generated assets — ADR-0004) |
| Reason | the consumer feature; dead dependencies are removed with their feature |

Verification is manual and explicit (fetch, hash, license read) — the
zero-budget phase adds no automated fetching, and no validator downloads
anything.

## 3. Tool provenance (this repository)

| Tool | Origin | Integrity posture |
|---|---|---|
| bash / git / python3 (host) | system-provided | versions measured and recorded per environment (check_environment.sh); no redistribution |
| Automation reference model | written in this repo | stdlib-only, deterministic, unit-tested (tests/automation/) |

No third-party code is vendored in this repository today; the empty
inventory in tools/README.md is the enforced state.

## 4. Integrity rules

1. Every tracked file is attributable to a reviewed commit (git history
   is the provenance record; history rewriting is prohibited).
2. Generated files carry generation instructions in-repo (e.g., contrast
   table in colors.md is recomputed by scripts/drs_contrast_check.py
   from the project's tool area) — no orphan artifacts.
3. Binary assets are prohibited until the inventory mechanism above is
   exercised with a real, hashed, licensed addition.
4. External URLs in documentation are constrained to the verified
   allowlist (validated per change by validate_project.sh); a new
   domain requires adding it to the allowlist **with a recorded reason**
   in the same change.

## 5. Phase 3 additions (2026-10-16)

Allowlist additions, with reasons:

| Host | Reason | Verified |
|---|---|---|
| `schemas.android.com` | XML namespace identifier in Android manifests/resources (`xmlns:android`, `xmlns:tools`) — an identifier, never fetched | n/a (identifier) |
| `github.com` + `objects.githubusercontent.com` | Kotlin compiler 2.0.21 release download (JetBrains) | HTTPS 200 + SHA-256 verified |
| `repo1.maven.org` | JUnit Platform console standalone 1.10.2 (Maven Central) | HTTPS 200 + SHA-256 verified |
| `services.gradle.org`, `download.gradle.org`, `dl.google.com` | documented future Android build hosts (SDK/Gradle distributions); probed reachable (200) in Phase 3 | probed only → **Phase 4: actually used** (see below) |
| `api.adoptium.net` | Eclipse Temurin JDK 17.0.20.1+1 tarball (redirects to Adoptium GitHub releases) — the full JDK (javac) required by AGP 8.7.3; the host JRE 21 has no compiler | Phase 4: downloaded 193,252,603 B, version string verified via `java -version`; outside the repo in tools/ |

Phase 4 toolchain actually resolved and used (all outside the repo, none
vendored): Temurin JDK 17.0.20.1+1 (api.adoptium.net), Gradle 8.9
(services.gradle.org, sha256 verified against the official .sha256),
Android cmdline-tools 12.0 + platform-35 + build-tools 35.0.0 +
platform-tools (dl.google.com), AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM
2024.12.01 / androidx artifacts (google() = dl.google.com, mavenCentral()
= repo1.maven.org). Hashes and sizes: docs/phase4/TOOLCHAIN_REPORT.md §2
and docs/phase4/BUILD_RESULTS.md §5.

Toolchain artifacts downloaded in Phase 3 (OUTSIDE the repository, hashed
in tools/README.md): kotlin-compiler-2.0.21.zip (85,822,011 B),
junit-platform-console-standalone-1.10.2.jar (2,680,679 B). Neither is
vendored, redistributed, or a product dependency.

## Phase 5 additions (2026-10-17) — Robolectric no-device CI layer

Test-only dependencies (never packaged into any APK; testImplementation /
testRuntimeOnly only), pinned in both apps' `gradle/libs.versions.toml`
with the same versions:

| Artifact | Version | Source host | Why |
|---|---|---|---|
| `org.robolectric:robolectric` | 4.14.1 | repo1.maven.org (mavenCentral) | JVM-simulated Android for the no-device CI layer (PHASE4_FINAL_REPORT §13 item 2) |
| `androidx.test.ext:junit` | 1.2.1 | dl.google.com (google) | AndroidJUnit4 runner bridging Robolectric into JUnit4 |
| `androidx.compose.ui:ui-test-junit4` | 1.7.6 via BOM 2024.12.01 | dl.google.com (google) | Compose test APIs (createAndroidComposeRule, matchers) |
| `org.junit.vintage:junit-vintage-engine` | 5.10.2 | repo1.maven.org (mavenCentral) | Runs the JUnit4/Robolectric suites on the same JUnit Platform as the Jupiter suites |

Runtime-resolved during the first Robolectric test run (Robolectric's own
dependency fetch, cached outside the repo): `org.robolectric:android-all-
instrumented` artifacts for the emulated SDK levels, fetched from
repo1.maven.org by Robolectric itself — free, standard, recorded here for
completeness.

Pin discipline: no existing pin was changed; the new pins keep the
catalogs of DRSSettings and DRSAutomation in lockstep (checked by
check_phase3_code.sh catalog consistency). DRSSecurityCenter received no
new dependencies (its logic is covered by security-core JVM tests).
