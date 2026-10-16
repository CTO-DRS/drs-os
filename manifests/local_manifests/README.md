# Production Local Manifests — EMPTY BY POLICY

This directory is the **production** home for DRS OS repo local manifests.
It currently contains **no manifests** — and that is the correct state.

## Why it is empty

1. No DRS-owned Git repositories exist (creating them requires explicit
   authorization — ADR-0002).
2. Every candidate remote, revision, and project name would be a guess,
   and guessed integration values are a hazard, not a shortcut.
3. Phase 2 policy: **placeholder remotes are rejected** in this scope by
   `build/scripts/check_manifests.sh` (exit 1). A file with
   `git.invalid`, `example.com`, `localhost`, or marker strings such as
   `PLACEHOLDER`/`CHANGEME` cannot pass here.

## How a production manifest may come into existence (future)

1. Authorization is granted and DRS repositories are created.
2. Start from `../templates/drs-os-local.xml.template` (note the
   `.template` extension — the repo tool cannot consume it directly).
3. Replace every placeholder with a **verified** value: real remote URL
   (fetched and confirmed reachable), explicit pinned revision (commit
   SHA or tag — never a moving branch reference), real project names.
4. Record the upstream manifest commit SHA per ADR-0001.
5. Run `bash build/scripts/check_manifests.sh` — production mode must
   pass with zero placeholder findings.
6. Only then may the file be used in a checkout, on a validated host,
   with `repo sync` executed by an authorized engineer.

## Rules

- Do not place illustrative files here; illustrations belong in
  `../templates/`.
- Do not edit upstream AOSP `default.xml` — ever (ADR-0002).
- Every production manifest must reference only verified remotes and
  pinned revisions; semantic verification against the live AOSP manifest
  happens on the integration host before first sync.
