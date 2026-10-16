# DRS OS Upstream Synchronization & Patch Maintenance Policy

Status: policy (Phase 2). Operational procedure for tracking the AOSP
baseline, pinning revisions, and keeping DRS modifications mergeable.

## 1. Branch and revision pinning procedure (normative steps)

1. **Re-verify the branch** at integration time against
   https://android.googlesource.com/platform/manifest and
   https://source.android.com/docs/setup/start (Phase 0 verified
   `android-latest-release` → revision `android17-release` on
   2026-10-16; this is re-checked, never assumed).
2. `repo init -u https://android.googlesource.com/platform/manifest -b
   android-latest-release` on a validated host.
3. **Immediately record the resolved manifest commit SHA**:
   `git -C .repo/manifests rev-parse HEAD` → decision log entry
   (date, operator, SHA, branch). This SHA — not the branch name — is
   the reproducibility anchor.
4. DRS local manifest: every project pinned to an explicit SHA or tag
   (never a moving branch ref) — ADR-0002.
5. Record tool versions (repo launcher, git, python) next to the SHA.

## 2. Sync cadence

| Event | Trigger | Actions |
|---|---|---|
| Monthly ASB intake | Android Security Bulletin publication | review affected components → plan merge window (vulnerability-management.md §2) |
| Quarterly source drop | AOSP trunk-stable publishing cadence | full sync window; re-pin manifest SHA; re-run full static + build gates |
| Ad-hoc security fix | critical upstream fix | expedited merge per vulnerability-management.md severity bands |

Each sync event records: date, old SHA → new SHA, delta summary,
gates re-run with results.

## 3. Patch maintenance policy

- **Default is zero local patches.** DRS customizations enter via
  overlays, extension points, and new packages (ADR-0003).
- Any framework/SystemUI patch is registered in the **minimal-fork
  ledger** (decision log): file path, exact reason, upstreaming status,
  re-merge cost estimate. A patch absent from the ledger is a defect.
- Merge conflict policy: DRS side yields structurally (adapt DRS patch
  to upstream structure) unless the upstream change is itself the
  defect; behavior disputes go to an ADR, not to patch accretion.
- Upstream-first: fixes with general value are proposed to AOSP
  (gerrit) rather than held as DRS-local deltas. Holding a general
  fix locally requires a logged justification and a review date.
- After every upstream merge: re-run full validation suite + build +
  boot smoke before any artifact is labeled.

## 4. Prohibited

- Pinning by branch name in production manifests.
- Silent cherry-pick accumulation outside the ledger.
- Syncing with an unverified local manifest (placeholder rejection must
  have passed in production mode — docs/build/integration-readiness.md).
- Recording the manifest SHA after the fact; it is captured at init,
  before sync.
