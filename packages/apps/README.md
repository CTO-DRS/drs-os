# packages/apps — DRS System Applications

**Status update (Phase 3, 2026-10-16):** DRSSecurityCenter, DRSAutomation,
and DRSSettings now contain real source-complete Kotlin/Compose
implementations of their v1 scope, with Arabic-default resources and
pinned Gradle configuration. **Build status: SOURCE IMPLEMENTED — BUILD
NOT VERIFIED** (no Android SDK in the Phase 3 sandbox); the automation
app's pure logic is JVM-verified (9/9 tests). Details:
docs/phase3/IMPLEMENTED_COMPONENTS.md. DRSFiles remains untouched
scaffold (not in Phase 3 scope).

Per-app layout (each is a self-contained Gradle project that includes
the pure-Kotlin cores from components/ via relative paths):

| App | Directory | v1 contents |
|---|---|---|
| DRSSecurityCenter | DRSSecurityCenter/ | dashboard, deterministic checklist (6 checks), privacy screen (zero-permission introspection), status screen (permissionless APIs only) |
| DRSAutomation | DRSAutomation/ | rule editor/list/detail/audit over the engine-core; dry-run; honest trigger scope |
| DRSSettings | DRSSettings/ | language (per-app locales), theme, privacy/security entry points, automation policy, About |
| DRSFiles | — | scaffold only (future phase) |

Planned DRS system apps (design in docs/architecture/system-overview.md):

| App | Purpose | Class | Privileged? |
|---|---|---|---|
| DRSSecurityCenter | permission & security-state transparency (S6) | C | privileged stage comes after platform integration; whitelist policy pinned in platform/integration/privapp-permissions/ |
| DRSAutomation | deterministic rule engine + editor (S9) | C | no |
| DRSFiles | file management | C | no |

## Implementation rules (normative)

- Native platform components only (Kotlin/Java platform apps) — WebView is
  not a UI implementation vehicle for system functionality.
- Every privileged permission is individually justified in a
  `privapp-permissions` whitelist reviewed per change.
- Each app lives in its own repository (ADR-0002); this directory is the
  AOSP-tree mount parent, not a monorepo. The standalone Gradle projects
  here are the pre-integration development form; Soong templates live in
  platform/integration/ (inactive by design).
- No app may hold an Internet permission unless its documented data flow
  requires it (docs/architecture/data-flow.md) — none do today, and all
  three v1 manifests request zero permissions.

Nothing here has been installed or run on a device; there is no APK, no
signing, no boot verification yet.
