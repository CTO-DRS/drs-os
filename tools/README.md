# tools/ — Tool Inventory and Third-Party Dependency Policy

## Inventory (maintained; update on every change)

| Tool/dependency | Version | License | Source | Used by |
|---|---|---|---|---|
| bash | 5.2.37 (measured in sandbox) | GPL-3.0-or-later | system | validation scripts |
| git | 2.47.3 (measured) | GPL-2.0-or-later | system | version control |
| python3 | 3.12.15 (measured) | PSF-2.0 | system | XML/link validation + automation reference model (stdlib only) |

No third-party code, fonts, icons, or libraries are vendored in this
repository. The tooling uses only the tools above (host-provided, not
redistributed by this repo). The automation reference model
(automation-reference/rule_engine.py) is project-authored, stdlib-only,
and pinned by unit tests in tests/automation/.

## Policy for future additions

1. Every added dependency (code, font, icon, asset) is registered here
   **before** commit: name, version, license, upstream source, reason,
   provenance verification.
2. Licenses must be compatible with Apache-2.0 distribution and the
   project's security posture; copyleft components are acceptable only in
   components whose license obligations are understood and recorded
   (e.g., GPL kernel comes via upstream AOSP, obligations noted).
3. **No AI/ML SDKs** (ADR-0004) — scanned automatically.
4. No dependency is added at build scope without a reproducible source
   (pinned URL + hash recorded); "latest" is not a version.
5. Fonts: candidates and their licenses are documented in
   branding/typography.md; none are redistributed yet — entries appear
   here only when binary inclusion actually happens.

## Removal

Dependencies are removed when their consumer feature is removed; the
inventory row is deleted in the same change (no dead rows).
