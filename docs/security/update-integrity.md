# DRS OS Update Integrity & Verified Boot

Status: **design specification (Phase 1)** — no OTA has been produced, no
build exists, no bootloader has been touched (and will not be without
explicit authorization).

## 1. Update model

DRS OS adopts AOSP A/B (seamless) updates via `update_engine`:

- Two slot images; updates install to the inactive slot while the device
  remains usable.
- Bootloader marks successful boots; a failed boot on the new slot triggers
  automatic fallback to the previous slot (bootloader retry/rollback
  semantics as provided by the platform).
- Virtual A/B (snapshot-based) is the expected configuration on Cuttlefish
  generation targets; exact dynamic-partition layout will be read from the
  checked-out tree rather than assumed.

## 2. Integrity chain

1. **Payload authenticity:** OTA payloads are signed with the project's
   offline OTA payload key; `update_engine` verifies the payload signature
   before any bytes are committed to the inactive slot. Keys per
   key-management.md; production payloads never signed with test keys.
2. **Package-to-build binding:** payloads are generated from release-candidate
   target-files with recorded build fingerprints; the update metadata must
   match the security patch level trajectory (never regress the ASB patch
   level).
3. **Boot verification (AVB):** on targets that support Verified Boot,
   vbmeta and partition hashes chain from an immutable root of trust;
   DRS OS keeps AVB enabled on production images. `unlock` capability is
   a development-only affordance and production guidance requires a locked
   bootloader.
4. **Rollback protection:** AVB rollback indices bind the image to a minimum
   security version, defeating downgrade attacks. The OS version in the
   rollback index is bumped with security-critical releases; the bump
   schedule is a release-engineering decision recorded per release.
5. **dm-verity:** system partitions are verified at runtime; corruption or
   tampering causes enforced errors per platform policy.

## 3. Update distribution posture

- Check/download is user-initiated and TLS-protected; no silent background
  fetch of payloads without user visibility.
- There is no DRS update server yet by design; at zero budget the first
  stage distributes signed full-OTA files manually (user sideload of
  **signed** packages only — sideloading of unsigned packages remains
  blocked by verified boot in locked production configuration).
- Any future update server is a new ADR (it introduces T5 surface).

## 4. Failure and recovery

- Signature failure: abort, keep current slot, surface a clear message;
  no partial trust.
- New-slot boot failure: bootloader fallback to previous slot; data intact.
- User-facing recovery: factory reset via recovery is the last resort;
  documented, never silently automated.

## 5. What is explicitly NOT claimed

- No OTA payload, signing key, or update server exists today.
- No bootloader has been modified or unlocked anywhere.
- AVB specifics (exact vbmeta layout, rollback index values) are defined
  only after the first real build; this document fixes policy, not
  byte-level configuration.
