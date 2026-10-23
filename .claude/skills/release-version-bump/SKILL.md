---
name: release-version-bump
description: Bump the released version and release date in the fixed set of files of the phoss-smp project (docker/build-all.sh, CLAUDE.md, publiccode.yml, README.md, and in the sibling wiki Home.md and News-and-noteworthy.md). Trigger whenever the user says "prepare release X.Y.Z", "prepared for release", "bump the version to X.Y.Z", "update version files for the release", or similar in the phoss-smp repo — even when they don't say the word "skill".
---

# Release version bump (phoss-smp)

Replaces the previous release version with the new one in all files that mention it, and sets the release date where one is shown.
Do **not** commit or push unless explicitly asked; use specific `git add` if asked to commit.

## Inputs

- **New version** `X.Y.Z`: take it from the user; if not given, derive it from the `-SNAPSHOT` version in `pom.xml` (strip `-SNAPSHOT`) and confirm.
- **Old version**: read it from `docker/build-all.sh` (`version=...`) — never assume it.
- **Release date** `YYYY-MM-DD`: default to today's date; ask if the user mentions a different one.

## Files (repo = `/Users/philip/dev/git/phoss-smp`, wiki = `../phoss-smp.wiki`)

| File | Change |
|------|--------|
| `docker/build-all.sh` | `version=OLD` → `version=NEW` |
| `CLAUDE.md` | ``Current version: `OLD`.`` → ``Current version: `NEW`.`` |
| `publiccode.yml` | `softwareVersion: "OLD"` → NEW **and** `releaseDate: "..."` → release date |
| `README.md` | `Latest version: **[OLD](.../releases/tag/phoss-smp-parent-pom-OLD)** (DATE).` → version in link text, tag URL and date |
| wiki `Home.md` | `**Latest release: OLD (DATE)**` → NEW and date |
| wiki `News-and-noteworthy.md` | top entry heading `vNEW - work in progress` → `vNEW - DATE` |

## Procedure

1. Read old version from `docker/build-all.sh`; grep all six files for it to confirm each occurrence before editing.
2. Edit each file with the Edit tool (exact strings, not blind `sed` over the repo). Only touch the occurrences listed above — other mentions of the old version (e.g. historical news entries, migration notes) must stay.
3. If the wiki heading is not `vNEW - work in progress` (e.g. entry missing or already dated), stop and tell the user instead of guessing.
4. Verify with `grep -n "OLD" <the six files>` — remaining hits must be intentional — and `git diff --stat` in both repos.
5. Report only deviations or leftovers; the diff speaks for itself.

## Not covered

- Maven `pom.xml` versions (handled by the Maven release process).
- Starting the next development version's news entry (`vNEXT - work in progress`) — do that only if asked.
