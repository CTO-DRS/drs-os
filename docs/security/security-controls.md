# DRS OS Security Controls — Inventory and Status

Status ledger, Phase 2. Every control carries exactly one status from the
mandated taxonomy:

| Status value | Meaning |
|---|---|
| **upstream capability** | Provided by stock AOSP and used as-is; DRS neither implements nor modifies it |
| **DRS design requirement** | A rule DRS imposes on itself/its integrations; true by policy and review, not by code |
| **implementation pending** | DRS-owned engineering work that has not been done |
| **test pending** | The control exists (upstream or DRS) but its verification has not been executed |
| **verified implementation** | Executed and observed in this repository with recorded evidence (scope noted) |

Nothing here is described as audited: **a written security design is not a
security audit**, and no independent audit exists.

## Control inventory

| ID | Control | Status | Basis & scope | Verification |
|---|---|---|---|---|
| C-1 | SELinux enforcing; no permissive domains | upstream capability (enforcing policy ships with AOSP) + DRS design requirement (never permissive) + verified implementation (repository guard only) | Guard: check_sepolicy.sh, runs today, exit 0 recorded | build-time neverallow + boot-time `getenforce`: **test pending** (needs checkout) |
| C-2 | Android application sandbox unmodified | upstream capability | AOSP zygote/seccomp/SELinux stacking | CTS subset: **test pending** |
| C-3 | Least privilege; privapp whitelists, per-permission justification | DRS design requirement (+ upstream enforcement mechanism) | packages/apps/README.md rules | PM reject-on-mismatch: **implementation pending** (no privapp exists) |
| C-4 | Explicit permission controls; privacy indicators | upstream capability (PermissionController, privacy chips) | DRS surfacing in Security Center: **implementation pending** | Manual matrix: **test pending** |
| C-5 | Release signing; offline key custody | DRS design requirement | key-management.md; no keys exist | Custody checklist: **implementation pending** (first key generation) |
| C-6 | OTA signature verification + rollback protection | upstream capability (update_engine, AVB rollback indices) | update-integrity.md design | Signed-OTA e2e: **test pending** |
| C-7 | Verified Boot (AVB) on supported targets | upstream capability | Cuttlefish supports AVB | Boot verification status: **test pending** |
| C-8 | ASB tracking + upstream patch ingestion | DRS design requirement (process) | vulnerability-management.md | Monthly log: **implementation pending** (cadence starts at checkout) |
| C-9 | Dependency & license inventory | DRS design requirement + verified implementation (policy active; inventory enforced empty) | tools/README.md | Inventory diff per dependency add (ongoing) |
| C-10 | Reproducible build posture | DRS design requirement | reproducible-builds.md | Double-build hashes: **test pending** (no build exists) |
| C-11 | No secrets in source control | DRS design requirement + verified implementation (repository scope) | Pattern scans in validate_project.sh / run_static_checks.sh — executed Phase 1 & 2, exit 0 recorded. **Limitation honestly stated: pattern scanning cannot guarantee absence of all secret forms; it is a tripwire, not proof.** | Re-run per change (ongoing) |
| C-12 | Log hygiene (no PII in DRS logs) | DRS design requirement | data-flow.md logging policy | Release review: **implementation pending** (no product logs exist) |
| C-13 | Backup, recovery, incident response | DRS design requirement (process) | vulnerability-management.md §5 | Tabletop: **implementation pending** |
| C-14 | Vulnerability reporting channel | DRS design requirement | SECURITY.md; public channel deferred until authorized public presence | Publish at first release: **implementation pending** |
| C-15 | Source & dependency integrity (pinning, allowlisted URLs) | DRS design requirement + verified implementation (repository scope) | Phase 2: endpoint allowlist + placeholder rejection active in validators | Re-run per change (ongoing) |
| C-16 | Auditability of privileged operations | DRS design requirement | Rule engine: stateless decisions + mandatory audit records (docs/automation/rule-engine-spec.md); Security Center read-only views | **implementation pending** (product integration) |
| C-17 | Local data protection (FBE, sandbox-only storage) | upstream capability | data-flow.md storage rules | **test pending** (needs build) |

## Verified-today evidence (scope: this repository only)

Controls C-1 (guard), C-9, C-11, C-15, C-16 (reference-model tests) have
executed verification with recorded exit codes (Phase 1 and Phase 2 runs;
see docs/build/integration-readiness.md). Everything else is upstream
capability, policy, or pending — never claim more than the status column
says.

## Non-negotiables (review gates)

1. Never weaken SELinux policy to make a feature work; document open
   decisions instead (subsystem-boundaries.md §5).
2. Never add a permission without mapping it to a user-visible feature and
   a data-flow entry.
3. Never claim a control is active/verified without executable evidence.
4. Never store private keys in source control (key-management.md).
5. Never introduce permissive SELinux rules or broad privileged
   permissions as development conveniences.
