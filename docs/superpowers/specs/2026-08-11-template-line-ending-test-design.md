# Template line-ending test design

## Context

`LegacyMatchingTemplateTest.matchingRunWindowConfirmsSelectedAndSingleNotifications()`
expects JavaScript snippets containing LF (`\n`). On Windows, the template resource can
contain CRLF (`\r\n`), so the assertion fails even though the template behavior is unchanged.
The public secret-policy pull request does not modify either the template or this test.

## Decision

Normalize the template text loaded by `LegacyMatchingTemplateTest` from CRLF and lone CR
to LF before performing textual assertions. Keep the production HTML unchanged.

The normalization will be isolated to a small test helper so every test in this class that
reads `matching-run-window.html` uses the same platform-independent representation. No
application behavior, runtime configuration, or repository-wide line-ending policy changes.

## Alternatives considered

- Enforce LF repository-wide through `.gitattributes`: rejected because it changes a broader
  set of files and may create unrelated diffs.
- Change assertions to accept either CRLF or LF separately: rejected because it duplicates
  platform handling in each assertion and is easier to miss in future assertions.
- Ignore the failure and merge the security-policy pull request: rejected because a known
  failing full suite weakens the merge gate.

## Verification and delivery

1. Reproduce the focused test failure before editing.
2. Add the test-only normalization helper and update the two direct resource reads in this
   class to use it.
3. Run the focused test and then the full Gradle test suite.
4. Confirm only the design and test files changed, run diff checks and secret scanning, and
   publish a separate pull request.
5. Merge this fix first. Update the public secret-policy branch from `master`, rerun its
   policy checks and the full Gradle suite, and merge it only when all checks pass.

## Success criteria

- The focused test passes with CRLF and LF template resources.
- All Gradle tests pass.
- Production files remain unchanged.
- The fix and the secret-policy change remain in separate pull requests.
