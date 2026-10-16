# Security Policy

## Project status

DRS OS is in **Phase 1 (scaffolding)**. This repository contains
documentation, decision records, manifest templates, and static validation
scripts. It contains **no** operating-system binaries, no kernel, no
system images, and no applications. There is no runtime attack surface
associated with this repository itself beyond its use as a development
artifact.

## Reporting a vulnerability

- **Production code:** none exists yet, so there is nothing to exploit in
  the product sense. Do not attempt to test or attack any system and claim
  it is DRS OS.
- **This repository (tooling/docs):** if you find a defect in the
  validation scripts (e.g., a check that can be bypassed, a scan that
  misses an obvious secret pattern) or a factual security error in the
  documentation, report it privately to the project maintainer. A public
  security contact address and coordinated-disclosure process will be
  published when the project establishes an authorized public presence;
  see docs/security/vulnerability-management.md for the designed process.

## Safe harbor

Good-faith research limited to this repository's own content (running the
shipped validation scripts, reviewing documentation) is welcome and will
not be met with legal action, provided you do not access third-party
systems, exfiltrate data, or degrade any service.

## Hard guarantees maintained in this repository

- No private keys, tokens, or credentials are stored here, and automated
  scans (build/scripts/validate_project.sh,
  tests/static/run_static_checks.sh) enforce this.
- SELinux policy scaffold contains no permissive domains and no allow rules
  without documented justification (build/scripts/check_sepolicy.sh).
- No AI/ML/cloud-AI dependencies or endpoints are referenced
  (ADR-0004; scanned by validate_project.sh).
- Security-relevant decisions are recorded as ADRs and security documents
  under docs/security/, with explicit Designed-vs-Implemented status.

## Scope of security controls today

The **only** controls that are active at this stage are repository-level
static checks. All platform security controls (SELinux enforcement,
verified boot, OTA signature verification, sandbox hardening, key custody)
are **designed but not implemented**; each is labeled accordingly in
docs/security/security-controls.md. Never assume a documented control is
active.
