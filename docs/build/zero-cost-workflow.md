# DRS OS Zero-Cost Build & Validation Workflow

Status: workflow definition (Phase 1). Encodes the Phase 0 verdict into an
executable plan. Nothing in Stage 2+ has been executed. All commands shown
are reference material from official documentation — they are **not run in
this sandbox** and will only be run after authorization on a validated host.

## Stage 0 — Static-only development (CURRENT SANDBOX — active now)

Goal: maximize progress with zero downloads, zero risk, zero cost.

- Author everything that does not need the AOSP tree: documentation, ADRs,
  manifest templates, design specs, validation tooling (this repository).
- Validate continuously:

  ```bash
  bash build/scripts/check_environment.sh
  bash build/scripts/validate_project.sh
  bash tests/static/run_static_checks.sh
  ```

- Export deliverables through the workspace download area; treat the
  container filesystem as session-scoped (Phase 0 persistence finding).

## Stage 1 — Host acquisition (zero-budget reality)

Phase 0 reviewed the free-infrastructure landscape: no genuinely free tier
offers RAM/disk within an order of magnitude of AOSP requirements; trial
credit programs were excluded because they require payment instruments
(never described as free — see SECURITY.md principles). Conclusion: the
zero-cost path is **project-owned or user-provided hardware** meeting
docs/build/prerequisites.md §3. Until that exists, the project stays in
Stage 0 and does not stall: Stage 0 output (pinned manifests, specs,
tooling) is exactly what Stage 2 consumes.

## Stage 2 — Checkout on a validated host (NOT EXECUTED — reference only)

Executed only after: host checklist §3 passes; `repo init` authorization
given; branch re-verified (Phase 0 verified `android-latest-release` →
`android17-release`; re-check at that time); **record the resolved manifest
commit SHA** in the decision log (ADR-0001) before any sync:

```bash
# reference only — not run here
repo init -u https://android.googlesource.com/platform/manifest \
           -b android-latest-release
# record: git -C .repo/manifests rev-parse HEAD   -> decision log
repo sync -c -j8
```

DRS custom components arrive via the local manifest
(manifests/local_manifests/drs-os-local.xml) after real remotes exist and
are pinned to explicit revisions (ADR-0002).

## Stage 3 — Build & Cuttlefish (NOT EXECUTED — reference only)

```bash
# reference only — target string must be confirmed from the tree first
source build/envsetup.sh
lunch <aosp_cf_x86_64_phone-...>   # exact string read from actual tree
m                                   # AOSP build entry point
# launch the virtual device per source.android.com/docs/devices/cuttlefish
```

The lunch target is deliberately not invented here (Phase 1 constraint);
it will be confirmed from the checked-out tree and recorded.

## Stage 4 — Verification loop

Run docs/compatibility/test-matrix.md Layer 1 gates: boot smoke, SELinux
enforcing at boot, verification status, then Layer 2 functional gates.
Every gate result (pass/fail/skip+reason) is recorded; a release claim
requires executed evidence.

## Standing prohibitions (all stages)

No full checkout in the constrained sandbox; no flashing of any physical
device; no bootloader/partition changes; no account creation, billing
activation, or paid services; no AI dependencies at any stage; no secrets
in the repository at any stage.
