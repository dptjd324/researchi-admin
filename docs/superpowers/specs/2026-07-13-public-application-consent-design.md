# Public Application Consent Design

## Goal

Public research applications must collect auditable consent for the current research and optional future recruitment. Matching and notification behavior must enforce that consent instead of relying on the existing `PROVIDE_YN` delivery-state column.

## Consent UI

The public application page displays one section titled `10. 개인정보 수집·이용 및 리서치 안내 수신 동의` with a single explanatory panel. The current-research consent is independent and required. Future recruitment is an optional parent consent with two child channel choices:

1. Current research personal-information collection and use: required.
2. Future research matching and recruitment: optional.
3. Future research SMS messages: optional child channel.
4. Future research email messages: optional child channel.

The notice identifies the operator as `리서치아이` and the withdrawal contact as `spirit2@naver.com`. It lists the application fields and survey answers as collected data. It explains current-research administration, future matching, and channel-specific notifications as separate purposes.

The required consent data is retained until two years after the applicable research ends. Future recruitment consent remains valid until two years after consent or until withdrawal, whichever occurs first. Statutory retention exceptions remain stated in the notice.

Optional checkboxes default to unchecked and refusing them does not prevent the current application. Selecting future recruitment enables the two channel choices and requires at least one of SMS or email. Clearing future recruitment also clears both child channels. Selecting a child channel automatically selects future recruitment.

## Persistence

Create `admin_legacy_application_consent` in the admin database through `AdminSchemaBootstrap`. Each submitted application has one immutable consent record keyed by `(research_no, research_app_seq)` with:

- required privacy consent status
- future recruitment consent status
- SMS consent status
- email consent status
- consent text version
- consent timestamp
- future-consent expiration timestamp
- optional withdrawal timestamp

The consent version is a code constant so the exact deployed wording can be traced. No existing application is backfilled as consented. Existing records without a consent row remain visible in ordinary applicant administration but are ineligible for future matching and recruitment notifications.

The legacy `TB_RESEARCH_APP.PROVIDE_YN` column remains exclusively the delivery/provision state and is not reused for privacy consent.

## Submission Flow

1. Validate the required application fields and required current-research consent.
2. Insert the legacy application and its structured answers.
3. Insert the consent record using the submitted optional choices.
4. Index the application for search.
5. Return the success response only when the application and consent operations succeed.

The consent write uses the admin database and must not be swallowed as a best-effort operation. A consent persistence failure produces an application error and is logged with the research and application identifiers.

## Matching Enforcement

Matching candidate generation accepts only applications with an active future-recruitment consent record. Active means:

- `future_recruitment_yn = 'Y'`
- no withdrawal timestamp
- the expiration timestamp is later than the current server time
- at least one of `sms_yn` or `email_yn` is `Y`

This check is performed before results are stored, so an ineligible applicant does not appear in the matching result window or exports.

The existing two-year application-age limit, blacklist exclusion, successful-notification exclusion, duplicate handling, and keyword filters remain unchanged.

## Notification Enforcement

Consent is checked again immediately before dispatch because consent can expire or be withdrawn after a matching result was created.

- SMS requires active future-recruitment consent and `sms_yn = 'Y'`.
- Email requires active future-recruitment consent and `email_yn = 'Y'`.

An ineligible selected row is skipped without calling the provider. The notification log records a consent-related skipped result so the administrator can distinguish it from missing contact details or duplicate sends. Bulk and per-row dispatch use the same server-side checks.

## Matching Result UI

Every matching row carries `smsAllowed` and `emailAllowed` independently from the sent-state flags. A row displays only the channel buttons that the applicant currently allows:

- SMS only: display the SMS button only.
- Email only: display the email button only.
- Both channels: display both buttons.
- Neither channel: exclude the applicant from matching results.

The shared selection checkbox remains channel-neutral. For a mixed selection, the bulk SMS command targets only selected rows with active SMS consent and the bulk email command targets only selected rows with active email consent. Each bulk button displays its current eligible count, such as `SMS 발송 3명` and `이메일 발송 5명`, and is disabled when that channel has zero eligible selected rows. The confirmation dialog uses the same eligible count. Server-side dispatch rechecks consent and does not trust the browser count.

## Withdrawal Readiness

This change stores `withdrawn_at` and defines active-consent queries, but it does not add a public withdrawal page or an administrator mutation control. A later withdrawal workflow can mark the record withdrawn without changing matching or notification logic. Until that workflow exists, withdrawal requests received at `spirit2@naver.com` require a controlled database or maintenance operation.

## Tests

- Form validation rejects missing required consent and rejects future recruitment with neither channel selected. It accepts future recruitment with SMS only, email only, or both channels.
- Submission stores all consent flags, version, and expiration data.
- Schema bootstrap includes the consent table and unique/index constraints.
- Matching excludes missing, expired, withdrawn, and future-recruitment-denied consent records.
- SMS and email dispatch independently enforce their channel consent immediately before provider dispatch.
- Matching rows expose channel eligibility, hide disallowed per-row buttons, and calculate mixed-selection bulk counts per channel.
- Existing matching restrictions and duplicate prevention tests continue to pass.
- The rendered form contains the final Korean notice, the required current-research checkbox, and the optional parent-and-channel checkbox group with visible required/optional labels.

## Deployment

The admin schema bootstrap creates the consent table at application startup. After deployment, only newly submitted applications with active optional consent enter future matching. There is no migration that marks prior applicants as consented.
