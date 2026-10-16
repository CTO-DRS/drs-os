# ADR-0001: AOSP Baseline Branch

- Status: Accepted (Phase 1)
- Date: 2026-10-16
- Deciders: DRS OS platform engineering
- Supersedes: none

## Context

DRS OS builds on official AOSP. A branch must be chosen for the baseline
checkout. The Phase 0 audit (report: DRS-DR-0001) verified branch facts
against live official sources on 2026-10-16 rather than assuming them:

- source.android.com/docs/setup/start instructs builders to use the
  `android-latest-release` manifest branch under the 2026 trunk-stable
  policy (source published to AOSP quarterly).
- The live manifest repository shows `android-latest-release` declares
  revision `android17-release`; newest published release tags observed:
  `android-17.0.0_r1`, `16.0.0_r4`, `15.0.0_r36`.

## Decision

Baseline **`android-latest-release`**, interpreted as: initialize with the
branch, then **pin the resolved manifest commit SHA** in the decision log
at checkout time and treat that SHA as the true baseline for
reproducibility (docs/build/reproducible-builds.md). The branch name is
the selector; the SHA is the anchor.

## Consequences

- Tracking the branch means periodic upstream merges (security bulletins
  and quarterly source drops land quickly). Cost: continuous merge
  hygiene — mitigated by the minimal-fork policy (ADR-0003).
- Every sync event records: date, branch, resolved SHA, delta summary.
- Security posture: monthly ASB content reaches the baseline faster than
  with tagged-release pinning.
- If upstream policy changes (e.g., branch renamed or flow reverted), the
  change is detected at the next integration checkpoint and re-decided
  here; the recorded SHA remains valid for existing builds.

## Alternatives considered

- **Pinned release tag** (e.g., android-17.0.0_r1): maximal stability,
  slower security intake; rejected as primary, retained as fallback if
  merge cadence proves unmanageable.
- **aosp-main**: strongest freshness but unstable for productization
  (Phase 0 observed the main head state varies); rejected.
- **Custom long-lived fork branch**: maximizes control, maximizes merge
  debt; rejected — contradicts minimal-fork policy.

## References

- Phase 0 report §4.2 (branch verification evidence)
- docs/build/reproducible-builds.md (pinning contract)
- https://android.googlesource.com/platform/manifest (official source)
