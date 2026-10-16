# AOSP Integration Readiness Report

Report ID: DRS-DR-0002 · Phase 2 · Date: 2026-10-16 (UTC)
Scope: placeholder/integration hazards, manifest authenticity, remote
verification procedure, and honest readiness of every integration input.

## 1. Baseline audit (Phase 1 state, reproduced — not trusted)

Commands executed before any change (exit codes recorded):

| Command | Exit | Result |
|---|---|---|
| `git status --short` / `git log --oneline -3` | 0 | HEAD 94f2725 `phase-1-scaffold`; all files mode-drifted 644→755 (content byte-identical: `git diff` 0 insertions/deletions) — sandbox filesystem does not preserve permission bits between sessions; restored committed modes, tree verified clean |
| `bash build/scripts/check_environment.sh` | 0 | PASS=3 WARN=2 FAIL=0 (RAM/disk honest WARNs) |
| `bash build/scripts/validate_project.sh` | 0 | 61 passed / 0 failed / 1 skipped |
| `bash tests/static/run_static_checks.sh` | 0 | all static checks pass |
| `grep -rn git.invalid`, example/localhost/TODO sweeps | 0 | inventory below |

## 2. Placeholder / fictional-endpoint inventory (task: identify all)

| Location | Finding | Disposition |
|---|---|---|
| `manifests/local_manifests/drs-os-local.xml` (Phase 1 path) | **HAZARD**: executable-shaped manifest with placeholder remote `git.invalid` sitting in the production local-manifest path | **REMOVED** from production scope — moved to `manifests/templates/drs-os-local.xml.template` (git history preserved via `git mv`). The `.template` extension makes it non-executable: the repo tool consumes only `*.xml` |
| `manifests/templates/drs-os-local.xml.template` | placeholder remote `fetch/review=https://git.invalid/...`, placeholder revisions `refs/heads/main` | Retained **by design** as the illustrative template; clearly marked INACTIVE AND NON-BUILDABLE; template scope only |
| `build/scripts/check_manifests.sh` (Phase 1) | placeholder detection existed but produced only warnings; production/template scopes were not distinguished | **Rewritten (Phase 2)**: production scope REJECTS placeholder markers (fail, exit 1); template scope validates structure with TEMPLATE-MODE notices |
| Docs mentions of `git.invalid` (ADR-0002, manifests READMEs, this file) | documentation of the hazard, not endpoints | Retained; docs are allowlisted in the endpoint scan as the documented placeholder host |
| `example.com/org`, `localhost`, `127.0.0.1`, TODO/FIXME/CHANGEME sweeps | none found | — |

## 3. Production placeholder rejection (new control, verified)

- New rule: any manifest under `manifests/` (excluding `templates/`) or
  `manifests/local_manifests/` containing placeholder markers
  (`git.invalid`, `example.com|org`, `.example`, `localhost`, `127.0.0.1`,
  `TODO`, `PLACEHOLDER`, `CHANGEME`, `REPLACE_ME`) in any remote
  fetch/review, project name/path/revision, or raw content → **FAIL**.
- **Negative control (executed)**: a fixture with `git.invalid` placed at
  `manifests/local_manifests/zz-negative-test.xml` → validator FAILed
  with two findings, exit 1. Fixture removed; production area is empty
  again (correct state).
- Template negative-space: the template passes structural checks with
  TEMPLATE-MODE notices, exit 0.

## 4. Manifest authenticity — what is verified vs not

Verified today (structural): XML well-formedness, `<manifest>` root,
project path safety (no absolute paths, no `..`), remote/revision
resolution rules, no `<default>` re-pointing hazard, placeholder
rejection in production scope.

NOT verified (impossible in this environment; reported as not-run,
never faked):
- **Semantic validation against the live upstream `default.xml`** —
  requires the real manifest, i.e., a network fetch by an engineer, not
  an automated download.
- **Remote authenticity** — no DRS remotes exist; the procedure below is
  mandatory before any production manifest.
- Lunch-target strings, exact symbol availability, build behavior.

## 5. Remote verification procedure (gated, for the future)

A production manifest may be created only after, for **every** remote
and project:

1. Remote URL confirmed reachable by an engineer (TLS, expected host);
   recorded with date and result. No automated fetching by validators.
2. Repository confirmed DRS-owned and authorized to exist.
3. Revision pinned to a **specific commit SHA or tag** (never a moving
   branch); SHA recorded in the decision log.
4. Project `path` cross-checked against the integration map
   (docs/architecture/integration-map.md) and AOSP conventions.
5. Upstream manifest commit SHA recorded first (ADR-0001 procedure,
   docs/build/upstream-sync.md).
6. Production-mode `check_manifests.sh` passes with zero placeholder
   findings.
7. `repo sync` is then permitted **only** on a validated host by an
   authorized engineer. It has never been run for DRS OS.

## 6. Readiness verdict per integration input

| Input | Status |
|---|---|
| Production local manifest | **NOT READY — correctly absent.** No authorized remotes exist. Creating repositories requires explicit authorization; inventing them is prohibited. |
| Manifest template | READY as illustration; NON-EXECUTABLE by extension |
| Upstream branch selection | verified Phase 0 (`android-latest-release` → `android17-release`); re-verify at checkout (procedure: upstream-sync.md) |
| Lunch target | **NOT PINNED — deliberately.** Must be read from the real tree; no invented names anywhere in this repo |
| Device/vendor/app code | NOT READY (proposed only) — by design; no kernel/HAL/device-tree filler exists |
| Validation tooling | READY and active (Phase 2 hardened) |

## 7. Remaining blockers (unchanged in kind since Phase 0)

1. No compliant build host (64 GB RAM / 400 GB disk / KVM) — Phase 0
   verdict stands.
2. No DRS-owned Git repositories (authorization required).
3. No public security contact (deferred until authorized public
   presence).
4. Sandbox permission-bit drift (found this phase; mitigated by mode
   restoration + this report; re-check `git status --short` before every
   future session).

## 8. Conclusion

The Phase 1 scaffold contained exactly one integration hazard (the
executable-shaped placeholder manifest in a production path); it is now
isolated as a non-executable template, production scope actively rejects
placeholders (verified by negative control), and the repository's
endpoint inventory is constrained to an allowlist of verified official
hosts plus the documented placeholder. Integration readiness is honestly
**blocked on authorization and a compliant host** — not on documentation.
