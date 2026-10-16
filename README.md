# DRS OS — Original, Security-First Mobile Operating System on AOSP

> **Status: Phase 1 project scaffolding (documentation, decision records, and
> static validation tooling only).**
> This repository does **not** contain a bootable operating system, a Linux
> kernel, a SystemUI implementation, privileged applications, or any build
> artifacts. Every component listed here is either upstream AOSP capability
> that DRS OS plans to reuse, or a documented plan that has not been
> implemented. See [docs/phase-plan.md](docs/phase-plan.md).

## What is DRS OS?

DRS OS is an original mobile operating system built on the official
[Android Open Source Project (AOSP)](https://source.android.com). It is a
real platform-engineering project — not a launcher, not a themed Android
application, not a WebView shell, and not a visual prototype.

The product direction is a premium, modern, **Arabic-first (RTL-native),
security-and-privacy-first** mobile OS, initially targeting the x86-64
Cuttlefish virtual device, with physical-device support deferred to a later
phase.

## Non-negotiable project principles

| Principle | Commitment |
|---|---|
| Base platform | Official AOSP, baseline branch `android-latest-release` (verified in Phase 0; see ADR-0001) |
| Initial target | x86-64 Cuttlefish virtual device; no phone flashing in early phases |
| AI policy | **Zero AI/ML/generative/cloud-AI components** — deterministic logic, configurable rules, and local automation only (ADR-0004) |
| Budget | Zero. No paid services, no billing activation, no trial-credit dependence |
| System UI | Native Android platform components only — **no WebView/HTML/CSS/JS** for system functionality |
| Security | SELinux enforcing, application sandboxing, least privilege, verified boot where supported |
| Privacy | Data stays on device; no telemetry; transparent permission controls |
| Branding | Original DRS OS identity; AOSP attribution preserved; third-party brands removed from user-facing surfaces |

## Repository layout

```
drs-os/
├── docs/
│   ├── architecture/    System architecture and subsystem boundaries
│   ├── security/        Threat model, controls, updates, keys, vulnerabilities
│   ├── compatibility/   App compatibility strategy and test matrix
│   ├── build/           Prerequisites, zero-cost workflow, reproducibility
│   ├── decisions/       Architecture Decision Records (ADRs)
│   └── phase-plan.md    Phase status and Phase 2 proposal
├── manifests/           AOSP manifest strategy + local-manifest template
├── device/drs/          Future Cuttlefish-derived device layer (empty scaffold)
├── vendor/drs/          DRS-owned vendor configuration (empty scaffold)
├── packages/apps/       Future DRS system apps (empty scaffold)
├── overlays/            Runtime resource overlays (RRO) for theming (scaffold)
├── system/sepolicy/     SELinux policy scaffold — deliberately rule-free
├── build/               Build prerequisites docs + static validation scripts
├── scripts/             Reserved for developer workflow helpers
├── tests/               static/ (active) · security/ · compatibility/ (planned)
├── branding/            DRS OS design system: colors, typography, icons
└── tools/               Tool inventory and third-party dependency policy
```

## Validating this repository

The scaffold ships with read-only, network-free validation scripts:

```bash
bash build/scripts/check_environment.sh   # host capability report (read-only)
bash build/scripts/validate_project.sh    # full static validation of this repo
bash tests/static/run_static_checks.sh    # static test suite entry point
```

A passing result means the **repository content** is structurally valid. It
does **not** mean any operating-system component has been built, and it does
**not** mean an AOSP build is possible on the current host. The Phase 0 audit
concluded that full AOSP builds require a capable host (≥ 64 GB RAM,
≥ 400 GB free disk, KVM); see [docs/build/prerequisites.md](docs/build/prerequisites.md)
and [docs/build/zero-cost-workflow.md](docs/build/zero-cost-workflow.md).

## Phase status

| Phase | Scope | Status |
|---|---|---|
| Phase 0 | Environment & feasibility audit | Complete (2026-10-16); verdict: build infeasible in sandbox, feasible on compliant host |
| Phase 1 | Project scaffolding, architecture, security design, validation tooling | Complete (this repository) |
| Phase 2 | Host preparation and AOSP integration readiness | Proposed — requires explicit authorization; see [docs/phase-plan.md](docs/phase-plan.md) |

## License and attribution

Original DRS OS content is licensed under [Apache-2.0](LICENSE). DRS OS is
built on AOSP; upstream license notices and provenance will be preserved for
every reused component. See [NOTICE](NOTICE). DRS OS is an independent
project and is not affiliated with, endorsed by, or derived from any phone
manufacturer or Google product branding. "Android" is a trademark of Google
LLC; references here are to the open-source project.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). In short: no secrets in the repo, no
AI dependencies, no unlicensed assets, license headers required, static
checks must pass, and security-relevant changes require an ADR reference.

## Reporting security concerns

See [SECURITY.md](SECURITY.md) and
[docs/security/vulnerability-management.md](docs/security/vulnerability-management.md).
