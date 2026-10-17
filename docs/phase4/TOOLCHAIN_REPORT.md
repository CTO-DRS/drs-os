# Phase 4 — Toolchain Report

Measured 2026-10-17 (sandbox UTC). Every number below comes from an
executed command or a recorded file size; nothing is assumed. The goal
of this phase is build verification, so the toolchain was installed
first and is documented here in the state actually used for the builds
reported in `BUILD_RESULTS.md`.

## 1. Host environment (unchanged from Phase 0/3 measurements)

| Item | Value | Evidence |
|---|---|---|
| OS | Debian 13 (trixie), Linux 5.10.134, x86-64 container under KVM host | `uname`, `/etc/os-release` (Phase 0) |
| CPU | 2 vCPU, Intel Xeon, **no `vmx`/`svm` exposed**, `/dev/kvm` absent | `nproc`, `ls /dev/kvm` (failed) |
| RAM | 3.9 GiB total, ~3.4 GiB available, no swap | `free -h` |
| Disk | 9.9 GiB overlay rootfs; 8.9 GiB free before install, 7.6 GiB after toolchain install | `df -h` |
| Privileges | uid 1001, no sudo — everything installed user-level under `/home/z/my-project/` | `id` |
| Network | HTTPS OK to dl.google.com, services.gradle.org, api.adoptium.net, repo.maven.apache.org | download log successes |

## 2. Toolchain installed for Phase 4 (all free, no accounts, no root)

| Component | Version | Source (exact) | Size | Integrity |
|---|---|---|---|---|
| Temurin JDK | 17.0.20.1+1 (`javac 17.0.20.1` verified) | `https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse` → Adoptium GitHub release | 193,252,603 B tarball | version string recorded from `java -version` output |
| Gradle | 8.9 | `https://services.gradle.org/distributions/gradle-8.9-bin.zip` | 136,114,148 B | **sha256 verified** against official `gradle-8.9-bin.zip.sha256` (`sha256sum -c` → OK) |
| Android cmdline-tools | 12.0 (`sdkmanager --version`) | `https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip` | 153,607,504 B | Google-published artifact, filename recorded |
| Android platform | `platforms;android-35` | sdkmanager | — | installed dir verified |
| Android build-tools | 35.0.0 (aapt2 2.19-11948202) | sdkmanager | — | `aapt2 version` executed |
| platform-tools | current (`adb` present, no device connected) | sdkmanager | — | `adb` binary present |

Install locations (all outside the repo, git-ignored by the outer
workspace): JDK + Gradle under `/home/z/my-project/tools/`, SDK under
`/home/z/my-project/android-sdk/`, Gradle user home at
`/home/z/my-project/.gradle-home/`. Environment is exported by
`/home/z/my-project/scripts/drs_build_env.sh` (JAVA_HOME, ANDROID_HOME,
GRADLE_USER_HOME, PATH). SDK licenses accepted via
`yes | sdkmanager --licenses` (exit 0; license files present under
`android-sdk/licenses/`).

## 3. Versions pinned by the repository (NOT upgraded — per task §3)

- Android Gradle Plugin **8.7.3** (minimum Gradle 8.9 → installed exactly 8.9)
- Kotlin **2.0.21** + Kotlin Compose compiler plugin 2.0.21
- Compose BOM **2024.12.01**; core-ktx 1.15.0; activity-compose 1.9.3;
  lifecycle 2.8.7; appcompat 1.7.0; JUnit Jupiter 5.10.2
- compileSdk/targetSdk 35, minSdk 29, JVM target 17

No Gradle/Kotlin/AGP/SDK upgrade was made. Defect D-4 adds a missing
pin (navigation-compose 2.8.5) rather than changing existing ones.

## 4. Memory strategy for this sandbox (documented, reproducible)

The repo's `gradle.properties` requests `-Xmx2048m`. On a 3.9 GiB
sandbox with a Kotlin daemon this risks OOM. A **user-level** override
was placed in `GRADLE_USER_HOME/gradle.properties` (which takes
precedence over project gradle.properties):

```properties
org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx768m
org.gradle.parallel=false
```

Repository files are intentionally unchanged: on a normal developer
machine the repo defaults remain valid. This is recorded as baseline
item D-3 (environment-only, not a repo defect).

## 5. What can and cannot run here (task §3 four-way classification)

| Class | Tests | Verdict |
|---|---|---|
| 1. No-Android JVM tests | core-module suites (188) via kotlinc+JUnit; app pure-logic unit tests via Gradle `test`/`testDebugUnitTest` | **RUNNABLE** |
| 2. Android-SDK-required builds | Gradle `assembleDebug` per app (AGP + SDK 35 + build-tools 35.0.0) | **RUNNABLE after toolchain install** |
| 3. Emulator/device tests | instrumented tests, screenshots, launch-and-navigate evidence | **BLOCKED**: `/dev/kvm` absent (x86_64 emulator images require KVM; software emulation is unsupported and would not complete on 2 vCPU). adb exists but no device/emulator can be started. |
| 4. Full AOSP build | repo/sync, `soong_ui`, Cuttlefish boot | **BLOCKED**: ≥64 GiB RAM / ~400 GB disk / KVM required (Phase 0 audit); also requires explicit authorization per task §1 |

No time was spent retrying infeasible class-3/4 operations (task §3).

## 6. Disk ledger

| Item | Size |
|---|---|
| android-sdk (installed) | 458 MB |
| tools dir (JDK + Gradle + zips + Phase 3 kotlinc/JUnit) | 1.1 GB |
| Estimated Gradle dependency cache after 3 app builds | ~0.6–0.9 GB |
| Free after toolchain install (before builds) | 7.6 GB |

Budget check before each large operation: the full plan fits with >5 GB
headroom; no pruning or cleanup is required (and no destructive cleanup
is performed, per task §10).

## 7. Reproduce commands

```bash
# JDK (Temurin 17), Gradle 8.9 (verify sha256 against official .sha256), cmdline-tools:
curl -fsSL "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" -o jdk17.tar.gz
curl -fsSL "https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256" -o gradle-8.9-bin.zip.sha256
curl -fsSL "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -o gradle-8.9-bin.zip
echo "$(cat gradle-8.9-bin.zip.sha256)  gradle-8.9-bin.zip" | sha256sum -c -
unzip -q gradle-8.9-bin.zip && tar -xzf jdk17.tar.gz
mkdir -p $SDK/cmdline-tools && unzip -q cmdline-tools.zip -d $SDK/cmdline-tools \
  && mv $SDK/cmdline-tools/cmdline-tools $SDK/cmdline-tools/latest
yes | sdkmanager --licenses
sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```
