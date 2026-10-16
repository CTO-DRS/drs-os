# DRS OS Reproducible Builds

Status: policy (Phase 1). No build exists to reproduce; this page fixes the
reproducibility contract that all future builds must satisfy.

## 1. Inputs that must be pinned

| Input | Pinning mechanism | Recorded where |
|---|---|---|
| Upstream manifest revision | `repo init -b android-latest-release` then pin the resolved manifest **commit SHA** (not the branch name) | decision log (ADR-0001) |
| DRS component revisions | explicit `revision` (SHA or tag) per project in the local manifest — never a moving branch | manifests/local_manifests/ (ADR-0002) |
| Build number / timestamps | fixed, injected environment values for repro builds (`BUILD_NUMBER`, `BUILD_DATETIME` per AOSP reproducibility documentation) | release record |
| Toolchain | AOSP in-tree prebuilts (clang/JDK/art tools) — never host ad-hoc toolchains for release artifacts | build record |
| Host environment snapshot | `build/scripts/check_environment.sh` output archived per release | release record |

## 2. Procedure (contract)

1. Two independent builds from the same pinned inputs on separate paths.
2. Artifact sets hashed (SHA-256 manifest of images/apks/zips).
3. Hash sets must match; any difference is a release blocker with a filed
   investigation, not a shrug.
4. The hash manifest is published with the release notes.

## 3. Deltas DRS OS must keep reproducible

- Overlay/resource-only changes (ADR-0003 preference) are naturally
  reproducible; framework patches must avoid embedding timestamps/paths.
- SELinux policy compilations are deterministic given identical inputs —
  keep policy inputs in-repo and versioned.
- Automation rule compiler (future S9) must be deterministic: same rule
  input → byte-identical output.

## 4. Known non-goals / honest limits

- Userdebug development builds are not reproducibility targets (test keys,
  ad-hoc timestamps are tolerated there — explicitly not for releases).
- Emulator/host tooling (Cuttlefish host packages) versions are recorded
  but are not part of the artifact hash contract.

## 5. Status

Designed; first exercised at the first release build. The Phase 1
repository itself is committed with git and tagged, which is the current
integrity anchor for the project's documents and tooling.
