# PRD.md

## Product Direction

Researchi Admin is an old-admin-DB-first manager program.

The old admin DB is the operational source of truth:

- `TB_RESEARCH_MST`: focus group/survey postings
- `TB_RESEARCH_APP`: applicants
- `TB_BLACKLIST_MST`: blacklist

## Core Features

- Manage focus group/survey rows from `TB_RESEARCH_MST`
- Generate copy-ready homepage posting content
- Manage public application forms at `/research/{researchNo}/apply`
- Store applications in `TB_RESEARCH_APP`
- Search applicants per focus group/survey
- Prevent duplicate applications by name, phone, and birth date
- Manage `PROVIDE_YN` as whether applicant information was provided to the client
- Export all/provided applicant information as XLSX or TXT
- Send manual, scheduled, and threshold emails
- Mark sent applicants as provided
- Manage old-admin blacklist rows in `TB_BLACKLIST_MST`
- Run keyword matching manually and send SMS notifications
- Track monthly mail/SMS usage and estimated cost on the dashboard
- Manage clients and client contacts independently
- Keep action/search/mail/SMS/export logs

## Data Policy

Old tables may be MyISAM, so update flows must write revision logs before changing old DB rows. Supplemental tables should not replace old admin source data.
