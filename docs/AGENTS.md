# AGENTS.md

## Working Direction

Researchi Admin is old-admin-DB-first.

Use these operational keys:

- `RESEARCH_NO`
- `RESEARCH_NO + RESEARCH_APP_SEQ`
- `BLACKLIST_NO`

Do not add another public-board integration unless explicitly requested in a new project phase.

## Current Active Areas

- `legacy.research`
- `legacy.application`
- `legacy.blacklist`
- `legacy.matching`
- `client`
- `mailing`
- `notification`
- `dashboard`
- `log`
- `auth`

## Refactoring Rules

- Keep old DB data intact.
- Keep supplemental logs and snapshots unless the user explicitly asks to delete data.
- Remove retired code only when no active route/service depends on it.
- Prefer small, verifiable changes.
- Run compile/tests after structural cleanup.
