# Contributing to DRS OS

Thank you for considering a contribution. DRS OS is a platform-engineering
project with strict, non-negotiable constraints. Contributions that violate
them will be declined regardless of technical quality.

## Ground rules (normative)

1. **No AI/ML dependencies.** Do not add AI models, generative systems,
   cloud AI APIs, AI SDKs, or AI-assisted build features. All logic must be
   deterministic and auditable. See ADR-0004. Exceptions require a new,
   accepted ADR — none exist.
2. **No secrets.** Never commit private keys, tokens, credentials, or
   signing material. The repository runs automated secret scanning; commits
   containing secret-like material will be rejected and the material must be
   rotated.
3. **Native components only.** System functionality must use native Android
   platform technologies (platform services, privileged apps, runtime
   resource overlays). WebView/HTML/JS is not an implementation vehicle for
   DRS OS system features.
4. **Security is not negotiable.** Do not weaken SELinux policy, request
   unverifiable privileges, or relax signature/verified-boot behavior to make
   a feature work. If a design requires a privilege escalation, document it
   as an open decision in `docs/security/` instead of forcing it through.
5. **Honesty in artifacts.** Never claim a feature, test result, build, or
   image exists unless it demonstrably does. Documentation must distinguish
   *designed*, *implemented*, and *verified* states.
6. **Preserve provenance.** Keep upstream AOSP code clearly separated from
   DRS-owned code; never strip license headers or attribution.

## Every change must

- Carry an `SPDX-License-Identifier: Apache-2.0` header (shell scripts, XML
  templates; enforced by `build/scripts/check_license_headers.sh`).
- Pass the static gate locally before being proposed:

  ```bash
  bash build/scripts/validate_project.sh
  bash tests/static/run_static_checks.sh
  ```

- Use LF line endings, UTF-8, and final newlines (enforced by static checks).
- Reference the relevant ADR in the commit message when the change touches
  architecture, security posture, manifest strategy, or the no-AI policy.

## Commit message convention

```
<area>: <imperative summary>

<optional body explaining why>

Refs: ADR-000N (if applicable)
```

Examples: `docs: add OTA rollback window rationale`,
`scripts: extend secret scan to XML templates`.

## Review priorities

Reviewers check, in order: constraint compliance (AI/budget/native-only),
security impact, factual accuracy of documentation, test honesty (skipped
checks must be labeled skipped), and finally style. Formatting-only churn in
large docs is discouraged.

## Repository status note

The project repository is currently local-only; no public remotes exist and
none may be created without explicit project-owner authorization. Until a
public presence is authorized, "proposing a contribution" means preparing a
patch series or change list locally for review by the project owner.
