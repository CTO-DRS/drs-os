# DRS OS Subsystem Boundaries

Status: design baseline (Phase 1). Defines what each subsystem owns, what
it may reach, what it must never reach, and across which process/IPC/
security boundaries it communicates. Companion to system-overview.md.

## 1. Boundary model

DRS OS inherits the AOSP boundary stack and adds no new privileged trust
domain. Boundaries, from strongest outward:

1. **Kernel/user boundary** — enforced by the Linux kernel; all user-space
   is untrusted relative to it.
2. **App sandbox** — per-app UID, per-app data dirs, seccomp, SELinux
   `untrusted_app*` domains. Never relaxed for DRS apps.
3. **SELinux domain boundary** — every system process in a narrowly scoped
   domain; DRS additions gated by docs/security/sepolicy justification
   process (build/scripts/check_sepolicy.sh).
4. **Binder IPC permission checks** — each service validates caller
   identity/permissions; DRS services must re-verify on every call.
5. **Privileged-app whitelist** — privapps get only explicitly whitelisted
   privileged permissions, each individually justified in review.

## 2. Ownership and access matrix

| Subsystem | Owns (data/services) | May call | Must never | IPC surface |
|---|---|---|---|---|
| AOSP core (S1) | system services, framework config | kernel, HALs, all Binder services | — (upstream) | standard AOSP services |
| ART (S2) | app processes, JIT/AOT caches | framework APIs | direct HAL/kernel access | SDK APIs only |
| Kernel/HAL (S3) | drivers, HAL processes | kernel | cross-domain Binder escalation | HIDL/AIDL HAL contracts |
| DRS SystemUI (S4) | its own process + resources | public statusbar/notification APIs, `TileService` host APIs | hold INTERNET, read arbitrary app data | Binder to framework services |
| Settings overlays (S5) | overlay resources only | — (resources are declarative) | add code paths | none (RRO) |
| Security & Privacy Center (S6) | its app-private data; read-only views of security state | permission/usage APIs it holds consent for | write system settings silently, network | Binder (framework APIs) |
| Shade & QS tiles (S7) | tile state | TileService API, public notification APIs | persistent background network | Binder (framework APIs) |
| Theme engine (S8) | overlay APKs | OverlayManager via system UI | override `overlayable`-protected AOSP security resources | RRO registration |
| Automation engine (S9) | rules store (app-private), its scheduler | only public APIs whose permissions the user granted | run rules with elevated UID; fire actions without user-visible rule | local Binder (own service), intents |
| OTA updater (S10) | update metadata cache | `update_engine` API | bypass payload verification | Binder to update_engine |

## 3. Data ownership rules

- Every DRS app stores data exclusively in its own sandboxed
  `/data/data/<pkg>` (or credential-encrypted equivalent); no shared
  world-readable storage.
- Cross-app exports happen only through documented Android mechanisms
  (ContentProvider with grantable URI permissions, SAF).
- No DRS component reads other apps' private data; privileged views in the
  Security & Privacy Center use only permissioned APIs and never raw
  `/data` access.
- No component holds an Internet permission unless its function is
  impossible without it; today only the future updater surface is expected
  to need network, and even there download staging is designed to run in a
  dedicated, narrowly-scoped context.

## 4. Interface stability rules

- DRS services annotate intended-public surfaces and treat everything else
  as internal; no reliance on hidden APIs (`@hide`) without an explicit,
  logged decision (hidden-API reliance is an upgrade liability).
- SELinux policy, privapp whitelists, and overlayable policies are
  security-relevant interfaces: changes require ADR reference and review
  sign-off.

## 5. Known open boundary decisions (not silently resolved)

- Whether the Security & Privacy Center (S6) needs a `@SystemApi` addition
  for *recent sensitive access* history beyond what public APIs expose;
  if so, the API addition will be proposed upstream-first rather than
  forked.
- How automation rules (S9) survive backup/restore without leaking their
  content; candidate: encrypted, user-consented backup only.
These are tracked here deliberately; implementing either without a decision
record is a review failure.
