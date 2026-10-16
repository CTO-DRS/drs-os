# DRS OS Key Management

Status: **design and custody policy (Phase 1)**. No signing key of any kind
exists. No key material may be generated before release engineering is
authorized; this document fixes the rules that will apply when they are.

## 1. Key classes (planned)

| Key class | Purpose | Rotation | Custody |
|---|---|---|---|
| Platform release key | signs DRS system/privapps at release | per major platform generation, planned | offline, encrypted-at-rest, dual control |
| OTA payload key | signs update payloads verified by update_engine | per rotation event with rollback-index bump | offline, dual control |
| AVB key | root of verified boot on supported targets | rare; bump = coordinated release | offline, dual control, highest sensitivity |
| Per-app signing keys | signs each DRS app (upgrade identity) | avoided; loss = new app identity | offline |
| Dev/test keys | Cuttlefish userdebug builds only | disposable | must never sign production artifacts |

## 2. Hard rules (normative)

1. **Never in source control.** No private key, no keystore file, no
   certificate with private component, no passphrase. Enforced by secret
   scanning (Active control C-11) and .gitignore deny patterns.
2. **Offline generation.** Keys are generated only on an air-gapped or
   dedicated offline machine, using AOSP's documented signing tools
   (`make_key`-style generation and AVB tooling as documented in the AOSP
   signing documentation); exact command sequences will be validated
   against the checked-out tree at generation time and recorded then.
3. **Separation of environments.** A key that has touched a build machine
   used for userdebug/test images is never used for production.
4. **Dual control.** Production signing requires two project members; a
   single-person signing event requires a logged exception decision.
5. **Passphrases** live only with custodians, never in scripts, CI
   variables, or this repository.

## 3. Lifecycle

- **Generation:** offline, entropy from OS CSPRNG; key params recorded in
  the decision log (algorithm, length, generation date, custodians) —
  never the key itself.
- **Storage:** encrypted offline media, two geographic copies minimum,
  integrity-checked; no cloud storage of private keys at zero-budget
  phase (acceptable because signing cadence is expected to be low).
- **Use:** signing happens on a dedicated clean machine from known-good
  inputs; the machine is restored from a clean image after each signing.
- **Rotation:** triggered by schedule (annual review), suspected compromise,
  or platform-generation change; AVB/rollback coordination per
  update-integrity.md.
- **Revocation/compromise response:** if a production key is suspected
  compromised: stop signing, assess blast radius, rotate, bump rollback
  index so old-signed artifacts are rejected, publish incident note
  (vulnerability-management.md IR process).

## 4. Test keys discipline

AOSP userdebug builds sign with public test keys by default — acceptable for
Cuttlefish development only. A release checklist item (security-controls.md)
blocks any production claim while artifacts are test-signed. Test keys are
regenerated per project rather than reused from any template if release
signing ever becomes imminent; regardless, they never touch this
repository.
