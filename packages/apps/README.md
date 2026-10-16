# packages/apps — DRS System Applications (Scaffold)

**Status: empty scaffold. No application code exists.**

Planned DRS system apps (design in docs/architecture/system-overview.md):

| App | Purpose | Class | Privileged? |
|---|---|---|---|
| DRSSecurityCenter | permission & security-state transparency (S6) | C | yes, whitelisted |
| DRSAutomation | deterministic rule engine + editor (S9) | C | no |
| DRSFiles | file management | C | no |

## Implementation rules (normative)

- Native platform components only (Kotlin/Java platform apps) — WebView is
  not a UI implementation vehicle for system functionality.
- Every privileged permission is individually justified in a
  `privapp-permissions` whitelist reviewed per change.
- Each app lives in its own repository (ADR-0002); this directory is the
  AOSP-tree mount parent, not a monorepo.
- No app may hold an Internet permission unless its documented data flow
  requires it (docs/architecture/data-flow.md) — none do today.

Nothing here runs anywhere yet; there is no build, no APK, no signing.
