# tests/security — Security Test Scaffolding

**Status: planned. No security tests run here yet** beyond the
repository-level checks that live in `build/scripts/` and
`tests/static/run_static_checks.sh` (secret scan, sepolicy guard, license
headers — active today with recorded results).

## Planned checks (require a real checkout / build; blocked honestly)

| Check | What it verifies | Gate |
|---|---|---|
| SELinux boot-mode test | `getenforce` returns Enforcing on production-config Cuttlefish boot | every build |
| privapp whitelist audit | every privileged permission in DRS apps maps to a reviewed justification | every app change |
| permission surface audit | no DRS package requests undocumented permissions | every app change |
| network-free boot test | core UI functional with no network route (data-local principle) | per release |
| OTA negative tests | tampered/replayed/downgraded payload rejected by update_engine | per release |
| log hygiene audit | no PII in DRS-tagged log output | per release |
| reproducibility spot check | double-build hash comparison | per release candidate |

## Review gate discipline

Security-relevant changes (sepolicy, privapp whitelists, permissions,
overlayable policies) additionally require: ADR reference, threat-model
delta note, and explicit reviewer sign-off — see CONTRIBUTING.md and
security-controls.md non-negotiables.

Nothing in this directory may fake a security result. A test that cannot
run is reported as skipped with its reason, in the release record.
