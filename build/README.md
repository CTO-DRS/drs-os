# build/ — Build Documentation and Validation Scripts

Two distinct things live under `build/`:

## build/scripts/ — static validation tooling (ACTIVE)

Read-only, network-free, root-free scripts that validate **this
repository**. They do not build AOSP, do not download anything, do not
install packages, do not modify the host, and contain no destructive
commands. Exit codes: `0` pass (skips reported explicitly), `1` failure,
`2` environmental skip where documented.

| Script | Purpose |
|---|---|
| check_environment.sh | host capability report (RAM/disk/tools/KVM) — informational, not a build pass |
| validate_project.sh | orchestrator: required files, bash -n, secrets, unsafe commands, endpoint allowlist, AI scan, links, sub-checks |
| check_manifests.sh | XML validation; **production scope rejects placeholder remotes**; template scope structural-only (Phase 2 hardening) |
| check_sepolicy.sh | sepolicy guard: no permissive, no unjustified rules |
| check_license_headers.sh | SPDX headers on scripts and manifest XML/templates |

Active test suites outside build/: `tests/static/run_static_checks.sh`
(static entry point incl. manifest validation) and
`tests/automation/run_automation_tests.sh` (rule-engine reference model,
25 tests). Skips are always reported with their reason — never silently
converted into passes.

## docs/build/ — build knowledge (PLANNED state)

Prerequisites, zero-cost workflow, reproducibility policy — see
docs/build/. Nothing in this repository builds AOSP; a passing static
validation is **not** a build success and must never be presented as one.
