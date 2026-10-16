# DRS OS Threat Model

Status: **design-phase model (Phase 1)**. No product binaries exist, so no
threat in this model is mitigated by shipped code yet; mitigations map to
controls in security-controls.md with explicit Designed/Implemented status.

## 1. Assets (what we protect)

| ID | Asset | Notes |
|---|---|---|
| A1 | User data on device | messages, photos, credentials, location history |
| A2 | Authentication material | keystore2 keys, Gatekeeper credentials, biometric templates |
| A3 | OS integrity | boot chain, system partitions, OTA payloads |
| A4 | Signing & build keys | release keys, AVB keys, OTA payload keys (offline) |
| A5 | Privacy expectations | absence of telemetry, rule content, permission history |
| A6 | Update channel | trust that an update is genuine and current |

## 2. Threat actors

| ID | Actor | Capability | Primary interest |
|---|---|---|---|
| T1 | Remote network attacker | unauthenticated network access | exploit exposed services |
| T2 | Malicious app | runs in sandbox, may request permissions | privilege/data escalation |
| T3 | Local/opportunistic attacker | brief physical access, possibly with knowledge of victim | data theft, lock-screen bypass |
| T4 | Malicious insider / supply chain | can influence build inputs or dependencies | implant backdoors |
| T5 | Network MITM during update | controls the network | serve malicious OTA |
| T6 | Compromised upstream | upstream AOSP/component compromise | inherit vulnerabilities |

## 3. Attack surfaces (planned product)

- Network-facing: none by design beyond standard Android surface (no DRS
  network daemons).
- IPC: exported DRS components (Security Center views, automation
  triggers) — minimized by boundary rules.
- Local: USB (adb in dev builds), lock screen, share intents.
- Update path: TLS download + signature verify (targets T5).
- Build/supply chain: dependency set, manifest pinning, reproducibility
  (targets T4, T6).
- Development builds: userdebug Cuttlefish images are inherently weaker
  (adb root, relaxed verification) — explicitly non-production.

## 4. Trust boundaries

```
Internet ─│─ TLS/signed payloads ─│─ update_engine (system domain)
Apps ─────│─ sandbox + seccomp + SELinux ─│─ framework services
User ─────│─ lock screen / Gatekeeper / biometrics ─│─ keystore2
Storage ──│─ file-based encryption ─│─ /data
Boot ─────│─ ROM → bootloader verify → AVB → system
```
DRS OS adds no new trust boundary; it extends the standard ones only via
gated overlays and whitelisted privapps.

## 5. Threat → mitigation mapping (Designed)

| Threat | Vector example | Mitigation (control IDs) |
|---|---|---|
| T1 | exploit in unused service | minimize surface: no DRS network services (C-3, C-4); ASB patching (C-8) |
| T2 | permission abuse | sandbox preserved (C-2); explicit consent UI + indicators (C-4); privapp whitelist (C-3) |
| T3 | shoulder/steal | lock screen (A), FBE (A), keystore hardware-backed where available (C-5) |
| T4 | build implant | no secrets in VCS (C-11); reproducible builds (C-10); dependency inventory (C-9); minimal fork policy |
| T5 | malicious OTA | payload signature verify + rollback protection (C-6) |
| T6 | upstream vuln | ASB tracking (C-8); patch cadence; CTS/STS subset (planned) |

## 6. Residual risks (accepted/open, honestly stated)

1. **Emulator fidelity:** Cuttlefish lacks hardware-backed keymaster
   fidelity of real devices; keystore claims will be re-evaluated at device
   phase. Accepted for dev-phase only.
2. **userdebug usage:** all early testing runs userdebug images; no
   security claim may be made from them.
3. **No-GMS ecosystem:** sideloading habits may push users to untrusted
   APK sources; mitigation is documentation and future scanner tooling
   (open decision).
4. **Automation engine (S9):** even deterministic automation can be abused
   if an attacker can write rules; design keeps rules user-visible and
   consent-gated; formal rule-language review is an open task.
5. **Single-maintainer phase:** review depth is limited while the project
   is small; process compensates with checklists and slow, explicit gates.

## 7. Verification evidence

Current: **none for product claims** (no product exists). Evidence that
does exist today, scope = this repository:

- Phase 1: baseline static validation (60 passed / 0 failed / 2 skipped,
  exit 0).
- Phase 2: baseline reproduction (all three Phase 1 scripts re-executed,
  exit 0); hardened manifest validation with production placeholder
  rejection (negative control: placeholder manifest in production scope →
  exit 1, as designed); endpoint allowlist and unsafe-command scans added
  and executed; automation reference-model unit tests executed with real
  pass counts (docs/build/integration-readiness.md records commands and
  outputs).

Every future security claim must cite an executed test and its result;
claims without evidence are a documentation bug.
