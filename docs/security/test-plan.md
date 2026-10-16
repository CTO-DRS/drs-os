# DRS OS Security Test Plan

Status: **plan (Phase 2)**. Maps each security control to its verification
method, required environment, and current status. Nothing here may be
reported as passed without an executed run; "planned" is the honest state
for everything requiring a checkout or build.

## Test inventory

| # | Test | Verifies (control) | Method | Environment needed | Status |
|---|---|---|---|---|---|
| T-01 | Secret scan | C-11 | Pattern tripwire over tracked files (validate_project.sh, run_static_checks.sh) | any host with bash+grep | **VERIFIED** (repo scope; executed Phase 1 & 2, exit 0; negative control fires) |
| T-02 | Placeholder-remote rejection | C-15 | Hardened check_manifests.sh production scope; negative test with `git.invalid` fixture | bash + python3 | **VERIFIED** (repo scope; negative control exit 1 recorded Phase 2) |
| T-03 | Endpoint allowlist | C-15 | validate_project.sh URL allowlist scan | bash + grep | **VERIFIED** (repo scope) |
| T-04 | Unsafe-command scan | C-15 (tooling hygiene) | validate_project.sh script scan | bash + grep | **VERIFIED** (repo scope) |
| T-05 | AI-dependency absence | ADR-0004 | Endpoint + dependency-manifest scans | bash + grep | **VERIFIED** (repo scope; dependency scan formally N/A until manifests exist) |
| T-06 | Rule-engine semantics & safety | C-16 (design) | Python reference-model unit tests (tests/automation/) | python3 ≥ 3.10 | **VERIFIED** (reference model; product integration: planned) |
| T-07 | Sepolicy guard | C-1 (repo side) | check_sepolicy.sh (no permissive; justification enforcement) | bash + grep | **VERIFIED** (scaffold state) |
| T-08 | License/attribution presence | C-9 | check_license_headers.sh | bash | **VERIFIED** (repo scope) |
| T-09 | Boot-time SELinux enforcing | C-1 | `getenforce` on production-config boot + console capture | compliant host + Cuttlefish build | **planned** |
| T-10 | Privapp whitelist enforcement | C-3 | Install privapp with undeclared permission → must be rejected; review diff | build host | **planned** |
| T-11 | Permission transparency accuracy | C-4 | Grant/revoke flows; Security Center views match platform state | Cuttlefish build | **planned** |
| T-12 | OTA positive path | C-6 | Signed payload installs, boots, slot switch verified | build host + Cuttlefish | **planned** |
| T-13 | OTA negative paths | C-6 | Tampered, replayed, downgraded payloads → all rejected, nothing installed | build host + Cuttlefish | **planned** |
| T-14 | Verified Boot status | C-7 | Locked bootloader; verification state green; tampered system refused | build host + Cuttlefish | **planned** |
| T-15 | Network-free core function | C-17/local-data | Boot + exercise core UI with no network route | Cuttlefish | **planned** |
| T-16 | Log hygiene | C-12 | Exercise DRS surfaces; grep logs for PII markers | Cuttlefish | **planned** |
| T-17 | Reproducible build | C-10 | Double-build hash comparison | build host | **planned** |
| T-18 | Sandbox integrity | C-2 | CTS subset incl. sandbox/permission tests | build host + Cuttlefish | **planned** |
| T-19 | Automation product integration | C-16 | Port reference-model semantics to platform engine; replay same test vectors | build host | **planned** |
| T-20 | Rule-engine adversarial inputs | C-16 | Fuzz rule inputs (malformed, oversized, hostile strings) against reference model | any host | **planned** (deterministic corpus first) |

## Rules

1. Status is one of: VERIFIED (executed, evidence recorded, scope stated),
   planned, blocked (+ reason). No other values.
2. VERIFIED at repository scope never implies product-scope security.
3. Planned tests become executable only when their environment exists;
   until then they are skipped with this exact reason in reports.
4. T-20 (adversarial inputs) runs **before** any product integration of
   the automation engine, not after.
