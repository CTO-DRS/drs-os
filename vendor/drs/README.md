# vendor/drs — DRS Vendor Configuration (Scaffold)

**Status: empty scaffold. No vendor content exists.**

Intended contents (future, per AOSP conventions and ADR-0002):

- DRS-owned product configuration that is vendor-like (not device-hardware
  specific): e.g., default permission state configs, feature XMLs that are
  product-level rather than app-level.
- Rules once populated: every file auditable, no proprietary binary blobs
  without a recorded license and source, no third-party brand assets.

## Deliberate exclusions

- No prebuilt binaries, blobs, or "extracted" vendor content of any kind.
- No signing configuration or keys (see docs/security/key-management.md).
- No hardware-specific overlays (those belong to the device layer and
  require the real tree to define).
