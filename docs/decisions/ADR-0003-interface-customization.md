# ADR-0003: Interface Customization Strategy — Overlays First

- Status: Accepted (Phase 1)
- Date: 2026-10-16
- Deciders: DRS OS platform engineering
- Supersedes: none

## Context

DRS OS changes the platform's visual identity and some behavior (SystemUI
look, settings strings, theme colors, Quick Settings tiles) while building
on `android-latest-release` (ADR-0001). Every upstream line DRS modifies
increases future merge cost and audit surface, so the customization
mechanism is a first-order architecture decision.

## Decision

Customization mechanisms are chosen by the following strict preference
order. A mechanism may be skipped only with a written justification in the
change's commit/ADR:

1. **Runtime resource overlays (RRO)** — colors, dimensions, drawables,
   string tables, and the RTL variants. Zero code forks.
2. **Official extension points** — `TileService` for Quick Settings tiles,
   public framework APIs, declared `overlayable` policies, settings
   providers. Zero or additive code.
3. **New DRS packages** (privileged apps with individually justified
   `privapp-permissions` whitelists) — for functionality that has no
   overlay/extension answer (Security & Privacy Center, automation).
4. **Minimal framework patches** — last resort. Each patch lists: exact
   files, reason, upstreaming plan, and merge-cost estimate; recorded in
   the decision log. Silent drift into framework forks is a review
   failure.

Arabic-first RTL is handled natively (start/end-relative layouts, mirrored
directional resources) inside these mechanisms — RTL is a resource and
layout concern, not a fork.

## Consequences

- Upstream merges stay small; security review focuses on DRS-owned code
  only.
- Some visuals are constrained to what resources can express; deeper
  SystemUI redesign (docs/architecture S4 stage 2) may eventually justify
  a bounded SystemUI patch set — that step requires its own ADR with the
  full file list.
- `overlayable` policy protects security-critical AOSP resources from
  being re-themed by anything not DRS-signed.

## Alternatives considered

- **Full SystemUI replacement from day one:** maximum visual freedom,
  maximum fork cost, weakens security review (SystemUI is privileged).
  Deferred to stage 2 with explicit justification gate.
- **Framework fork with cherry-picks:** fast to hack, slow to maintain;
  rejected as default.

## References

- docs/architecture/system-overview.md §4 (extension-point policy)
- docs/architecture/subsystem-boundaries.md (ownership rules)
- branding/README.md (design system consuming this strategy)
