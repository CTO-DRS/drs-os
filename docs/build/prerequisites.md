# DRS OS Build Prerequisites

Status: prerequisites ledger. Values combine the Phase 0 verified AOSP
requirements with a fresh re-measurement of this sandbox taken at Phase 1
start (2026-10-16). **The current sandbox cannot build AOSP**; this page
defines what a capable host must provide.

## 1. Official AOSP requirements (verified Phase 0, 2026-10-16)

Source: source.android.com/docs/setup/start (fetched during Phase 0 audit;
re-verify at checkout time — the page is authoritative, not this summary).

| Requirement | Official value |
|---|---|
| Host OS | 64-bit Linux desktop distribution |
| RAM | ≥ 64 GB recommended by official docs |
| Disk | ≥ 400 GB free (checkout + build, ZFS/ext4 guidance) |
| Toolchain | Repo launcher (≥ 2.45 noted in Phase 0), Git, Python 3, OpenJDK per branch (prebuilts ship in-tree) |
| Packages | standard build package list per official docs (clang/bison/flex etc. ship as AOSP prebuilts, host basics still required) |
| Virtualization | KVM required to run Cuttlefish (build itself does not need it) |

Branch (verified): `android-latest-release`, resolving to revision
`android17-release`; newest tag observed `android-17.0.0_r1`. Decision and
pinning rules: ADR-0001.

## 2. This sandbox, re-measured at Phase 1 start (evidence)

| Dimension | Measured | vs. requirement |
|---|---|---|
| OS | Debian GNU/Linux 13 (trixie), kernel 5.10.134 (x86-64) | arch OK, containerized |
| CPU | 2 vCPU, Intel Xeon, no nested-virt flags | far below build-practical range |
| RAM | 4.06 GiB total, 4 GiB cgroup hard limit, no swap | ~6% of 64 GB |
| Disk | 9.2 GiB free of 9.9 GiB overlay rootfs | ~2% of 400 GB |
| KVM | /dev/kvm absent | Cuttlefish cannot run |
| git / python3 | git 2.47.3, Python 3.12.15 | sufficient for repo/static tooling |
| Java | OpenJDK 21 runtime only (no javac) | irrelevant now; AOSP ships prebuilt JDK |
| repo launcher | absent | installable sudo-free on a capable host |
| sudo | none (uid 1001) | no host package installs |
| Workspace | overlay fs, session-scoped container | export deliverables via download/ |

Verdict (unchanged from Phase 0): **static authoring and validation only**
in this sandbox; sync/build/run on a compliant host.

## 3. Target host checklist (for the future compliant machine)

- [ ] 64-bit Linux with ≥ 64 GB RAM and ≥ 400 GB free SSD
- [ ] KVM available to the build user (/dev/kvm readable)
- [ ] git ≥ 2.40ish, Python 3.6+ (repo requirement), curl
- [ ] Repo launcher installed via the official sudo-free path
- [ ] Git identity configured locally for the project
- [ ] Disk formatted ext4 (or ZFS per official guidance), not overlay-backed
- [ ] Time sync enabled (reproducible build timestamps depend on it)
- [ ] Optional: Docker only if choosing the containerized AOSP flow
      (equally acceptable; native flow is simpler at zero cost)

Each box is checked off in the decision log with evidence when the host
materializes. `build/scripts/check_environment.sh` automates the
measurement portion and may be re-run there.

## 4. What this repository deliberately does NOT contain

No repo manifests claiming real remotes, no device trees, no kernel
configs, no HAL stubs, no signing configuration — these require the
verified technical basis of a real checkout (Phase 1 constraint; see
system-overview.md S3 and ADR-0002).
