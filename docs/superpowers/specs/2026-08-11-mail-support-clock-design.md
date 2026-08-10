# Mail support clock design

## Context

`resolveDailyScheduledAtUsesTomorrowWhenTimeAlreadyPassed()` derives its input and expected
date from separate wall-clock reads. Between 00:00 and 00:59, subtracting one hour from a
`LocalTime` wraps to 23:xx, which is still in the future on the current date. The test then
incorrectly expects the following date. Separate clock reads also permit boundary races.

## Decision

Give `LegacyResearchMailSupportService` a single `Clock`. The existing public constructor
continues to use `Clock.systemDefaultZone()` so runtime behavior and Spring callers remain
compatible. A package-private constructor accepts a `Clock` for deterministic tests. All
four wall-clock reads in the service use this clock so one service instance has one time
source.

The scheduling tests use a fixed Asia/Seoul clock at 2026-08-11 00:05. They verify that
00:00 has already passed and schedules for the next day, while 23:05 remains later on the
same day. Validation tests derive their inputs from the same fixed instant.

## Alternatives considered

- Use `LocalTime.MIDNIGHT` with the real clock only in the test: smaller, but a date change
  between the service call and assertion can still make the test flaky.
- Rerun after 01:00: produces a temporary green build without removing the defect.
- Inject `Clock` as a new Spring bean: deterministic but unnecessarily changes application
  configuration and every deployment context.

## Delivery

This fix is a stacked branch based on `codex/template-line-ending-test`. Its pull request
initially targets that branch so the diff contains only this clock change. The combined
branch must pass all 174 tests. After the line-ending pull request merges, the clock pull
request is retargeted to `master` and merged separately.

## Success criteria

- The midnight test fails before the clock change and passes with a fixed 00:05 clock.
- The service has one time source without changing runtime time-zone behavior.
- All 174 Gradle tests pass on the stacked branch.
- The clock and line-ending changes remain separate commits and pull requests.
