# DRS OS Data Flow

Status: design baseline (Phase 1). Describes how data moves through the
planned system, where it rests, and which flows intentionally do not exist.

## 1. Principles

1. **Local by default.** User data never leaves the device. There is no
   DRS telemetry, analytics, crash-upload, or cloud sync of any kind. Any
   future change to this rule requires a new ADR and, by policy, will be
   refused — the no-cloud posture is a product identity, not a phase
   limitation.
2. **Explicit exits.** Data crosses a boundary only when the user performs
   an explicit action (share sheet, file export, backup to user-chosen
   location).
3. **Least retention.** Each component keeps data only as long as its
   function needs it; logs must not contain message content or identifiers
   beyond Android log policy.

## 2. Flow inventory

### F1 — User interaction → framework → apps (Class A)
Touch/keys → kernel input → SystemUI/window manager → focused app process.
All inside the standard AOSP pipeline; DRS adds theming resources only.
Rest points: app-private storage per sandbox rules.

### F2 — App data storage (Class A)
App writes → per-app sandboxed dir (credential-encrypted storage by
default; device-encrypted only where the platform semantics require it).
No DRS component reads another app's sandbox. Uninstall removes the data.

### F3 — Automation rules (Class C, designed)
```
user authors rule in editor app
        │  (validated, compiled to deterministic trigger/action pairs)
        ▼
rules store (app-private storage, on-device only)
        │
system events ──► trigger evaluation (local, deterministic, no network)
   AlarmManager / JobScheduler / broadcast / sensor-consented state
        │
        ▼
action dispatch via public APIs the user already consented to
   (e.g., toggle DND, set theme, open app) — never via elevated UID
```
Safety properties: rules are user-visible and user-deletable; actions are a
whitelisted code-reviewed set; no rule can grant itself new permissions;
rule content is never logged.

### F4 — OTA updates (Class B/C, designed)
```
update metadata check (user-initiated, TLS)
        ▼
payload download → staged to update_engine
        ▼
signature verification (payload key) — failure aborts, nothing installs
        ▼
install to inactive slot → reboot → new slot verified by AVB chain
        ▼
confirmation window; failure → automatic rollback to previous slot
```
Anti-rollback: AVB rollback indices prevent booting older, vulnerable
images. Details: docs/security/update-integrity.md.

### F5 — Diagnostics (Class A, constrained)
Bugreports exist (upstream) but are **user-triggered, user-exported only**.
No automatic upload path exists in DRS OS and none will be added.

### F6 — What does NOT flow (negative space, normative)
- No usage statistics, no device identifiers to any server, no crash
  reporting service, no "anonymous analytics", no advertising IDs, no
  cloud configuration push.
- The automation engine never sends rule content or event history anywhere.

## 3. Logging policy

DRS components log with tags `DRS*`, never include user content, tokens,
or precise location; verbose logging compiled out of production builds.
Log review is part of the release checklist (docs/security/security-controls.md,
control C-12).

## 4. Data-localization verification (planned)

- Static: permission review gates — any INTERNET/ network-typed permission
  request in a DRS package must map to a documented flow above.
- Dynamic (post-checkout, Cuttlefish): network-free run test — boot and
  exercise core flows with no network route and verify functional core.
