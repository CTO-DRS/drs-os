# DRS OS Security Controls — Inventory and Status

Status ledger. **Designed** = specified in this repository, not built.
**Active** = running and verified today. Nothing here may be described as
audited; no independent audit exists.

## Control inventory

| ID | Control | Source | Status | Verification plan |
|---|---|---|---|---|
| C-1 | SELinux enforcing in all production configurations; no permissive domains | AOSP + project guard | Designed (guard script **Active** on repo) | build-time neverallow; boot-time `getenforce`; check_sepolicy.sh (runs now) |
| C-2 | Android app sandbox preserved unmodified | AOSP | Designed (inherited) | CTS subset on Cuttlefish |
| C-3 | Least privilege; privapp-permissions whitelists, each permission justified | project policy | Designed | whitelist diff review; PM reject-on-mismatch |
| C-4 | Explicit permission controls; understandable privacy indicators | AOSP PermissionController | Designed (reuse; UI surfacing in S6) | manual matrix + permission audit |
| C-5 | Secure release signing; keys never in source, offline custody | docs/security/key-management.md | Designed | key-custody checklist; repo scans |
| C-6 | OTA signature verification + rollback protection | docs/security/update-integrity.md | Designed | signed-OTA e2e test on Cuttlefish |
| C-7 | Verified Boot (AVB) where the target supports it | AOSP AVB | Designed (Cuttlefish supports) | boot verification status check |
| C-8 | Android Security Bulletin tracking + upstream patch ingestion | docs/security/vulnerability-management.md | Designed (process) | monthly review log |
| C-9 | Dependency & license inventory | tools/README.md | **Active** (policy + empty inventory enforced) | inventory diff per dependency add |
| C-10 | Reproducible build posture | docs/build/reproducible-builds.md | Designed | double-build hash comparison |
| C-11 | No secrets in source control | project policy | **Active** (scan runs in validate_project.sh / run_static_checks.sh) | recorded exit statuses |
| C-12 | Log hygiene: no PII in DRS logs | data-flow.md | Designed | release checklist review |
| C-13 | Backup, recovery, incident response defined | vulnerability-management.md | Designed (process docs) | tabletop exercise planned |
| C-14 | Vulnerability reporting channel | SECURITY.md | Designed (channel TBD pre-public) | publish at first public release |

## Active-today evidence

Only C-11, C-9 (inventory policy), and the repo-side half of C-1 (policy
guard) run in this phase. Their execution results are recorded in the Phase
1 validation report with real exit codes. Everything else is design intent.

## Non-negotiables (review gates)

1. Never weaken SELinux policy to make a feature work; document open
   decisions instead (see subsystem-boundaries.md §5).
2. Never add a permission without mapping it to a user-visible feature and
   a data-flow entry.
3. Never claim a control is active without executable evidence.
4. Never store private keys in source control; generation happens offline
   per key-management.md.
