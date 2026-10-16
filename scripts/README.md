# scripts/ — Developer Workflow Helpers (Reserved)

**Status: reserved; no scripts here yet.**

This directory is reserved for future developer-workflow helpers that are
not part of the validation gate (for example, release-note generation or
checklist scaffolding), should the project need them.

Validation tooling deliberately lives in `build/scripts/` to mirror the
AOSP convention of build-time tooling under `build/`, and so that
`validate_project.sh` can discover all gate scripts in one place. Do not
move or duplicate validation logic here; the single source of truth for
checks is `build/scripts/` and `tests/static/`.
