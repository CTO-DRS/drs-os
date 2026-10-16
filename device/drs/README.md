# device/drs — Device Layer (Scaffold)

**Status: empty scaffold. No device configuration exists.**

This directory will hold the DRS OS device layer for the x86-64 Cuttlefish
target — a *derived* configuration, not a new virtual device: DRS OS first
runs stock Cuttlefish with DRS overlays and packages on top (ADR-0003).

## What will eventually live here (not created now)

- `AndroidProducts.mk` and a product makefile deriving from the Cuttlefish
  phone product in the checked-out tree (inherit, do not copy).
- Product-level package lists for DRS apps and overlays.
- The exact lunch target string confirmed from the real tree and recorded
  in the decision log.

## Why nothing is created yet (deliberate)

Writing device makefiles, kernel configs, or device trees without the real
AOSP tree would produce plausible-looking guesses that fail at build time
and mislead reviewers. The Phase 1 constraint holds: no device tree, no
kernel, no HAL implementation without a verified technical basis.
`manifests/local_manifests/drs-os-local.xml` reserves the mount point
(`device/drs`) for when this content becomes real.
