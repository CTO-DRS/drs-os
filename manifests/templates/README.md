# Manifest Templates — Illustrative Only, Non-Executable

Files here are **illustrative templates**, deliberately non-executable:

- The `.template` extension means the `repo` tool cannot consume them
  (it only reads `*.xml`).
- They contain placeholder hosts (`git.invalid`) and placeholder
  revisions (`refs/heads/main`) — none of which may reach a production
  manifest without verification.
- `build/scripts/check_manifests.sh` validates them structurally and
  labels them TEMPLATE-MODE; it would **reject** the same content in a
  production scope (Phase 2 hardening).

## Files

- `drs-os-local.xml.template` — the future DRS OS local manifest,
  showing the intended repository split (device/drs, vendor/drs, three
  system apps, overlays) and the no-`<default>` pattern that prevents a
  local manifest from re-pointing upstream projects.

## Promoting a template to production

See `../local_manifests/README.md` for the gated procedure. Short
version: authorized remotes + verified URLs + pinned revisions +
production-mode validator pass, or it does not ship.
