# ADR-0004: No-AI Policy

- Status: Accepted (Phase 1; standing project constraint)
- Date: 2026-10-16
- Deciders: DRS OS project owner
- Supersedes: none

## Context

DRS OS is specified as a zero-AI product: no AI models, no generative
systems, no cloud AI APIs, no AI assistant features. The policy must be
made enforceable rather than aspirational, because AI SDKs frequently
arrive as transitive conveniences, and "smart assistant" features are the
industry default.

## Decision

**DRS OS contains no artificial-intelligence components — in product code,
system services, build tooling, and project infrastructure.**

1. All product behavior is deterministic: explicit rules, user-authored
   automation (S9), configuration, and classical algorithms.
2. The automation engine is the *replacement* for any AI-assistant
   concept: transparent, user-controlled, locally executed, inspectable
   rules — no learned models, no probabilistic inference, no network
   inference calls.
3. Prohibited in any dependency, build file, or shipped component:
   AI/ML SDKs and runtimes (model inference libraries, LLM client SDKs,
   cloud AI endpoints). Prohibited endpoints include the public APIs of
   generative-AI providers; the validator scans dependency manifests and
   all tracked text for such references.
4. Allowed without debate: compilers, static analyzers, linters,
   formatters, spell-checking dictionaries, and deterministic heuristics —
   tools whose behavior is inspectable and reproducible.
5. This policy also binds contributor tooling embedded in the repo: no
   AI-generated assets are committed; every asset has a human-recorded
   provenance entry.

## Enforcement (active today)

- `build/scripts/validate_project.sh` scans for generative-AI endpoint
  patterns across all tracked files and for AI SDK identifiers in
  dependency manifests; any hit fails validation.
- When dependency manifests appear (post-checkout), the scan extends to
  them automatically (reported as skipped-with-reason while none exist —
  never as a fake pass).
- Exceptions require a new accepted ADR superseding this one; none exist.

## Consequences

- Certain industry-standard "smart" features (assistant, generative
  wallpaper, AI photo tools) are permanently out of scope; the product
  identity is deterministic, private, and auditable.
- Supply-chain audit surface stays small and legible.
- Zero-cost posture reinforced: no inference infrastructure, ever.

## Alternatives considered

- **Local-only AI (on-device models):** still violates the stated product
  constraint (deterministic, auditable behavior) and adds heavyweight
  dependencies at zero budget. Rejected.
- **Opt-in AI with network consent:** contradicts the no-cloud-AI
  identity and expands the threat model for marginal benefit. Rejected.

## References

- NOTICE §4 (product attribution of the policy)
- docs/architecture/system-overview.md S9 (deterministic automation)
- docs/architecture/data-flow.md F6 (no cloud flows, normative)
