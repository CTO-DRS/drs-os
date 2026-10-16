# system/sepolicy — DRS SELinux Policy (Guarded Scaffold)

**Status: deliberately rule-free.** This directory contains **no** `.te`
files, no `file_contexts`, no allow rules, and no domains. This is a
security decision, not an omission.

## Policy philosophy (normative)

1. **Start from upstream enforcing policy.** DRS OS builds on the AOSP
   policy that ships with the baseline (ADR-0001). It is enforcing,
   complete, and audited upstream; DRS adds only what its own components
   genuinely require.
2. **No rule without a verified basis and written justification.** When a
   DRS component needs policy, each `.te` addition must (a) correspond to
   a real, demonstrable access need, (b) be as narrow as possible, and
   (c) be justified in `docs/security/sepolicy-justifications.md`.
   `build/scripts/check_sepolicy.sh` enforces this: any allow-type rule
   without a justification entry fails validation.
3. **Permissive domains are forbidden, always.** No DRS configuration may
   set a domain permissive or disable enforcing mode; the check script
   hard-fails on any `permissive` statement.
4. **No weakened policy to ship a feature.** If a feature cannot be built
   within policy, the design decision is documented (see
   docs/architecture/subsystem-boundaries.md §5) — the policy is not
   relaxed.

## Why scaffold-only is the honest state

Real policy can only be written against a real checkout (domain names,
types, and attributes vary by branch) and verified only at build time
(neverallow checks) and boot time (`getenforce`). Authoring rules now
would be guesswork masquerading as security engineering.

Full policy analysis (`sepolicy-analyze`, build-time neverallow
verification, boot-mode verification on Cuttlefish) is a Layer-1/2 test
activity — see docs/compatibility/test-matrix.md — currently impossible
in this environment and reported as skipped, never faked.
