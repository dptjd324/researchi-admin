# TASKS.md

## Completed

- [x] Old-admin DB first architecture
- [x] Focus group/survey management based on `TB_RESEARCH_MST`
- [x] Applicant management based on `TB_RESEARCH_APP`
- [x] Blacklist management based on `TB_BLACKLIST_MST`
- [x] Public application form based on `/research/{researchNo}/apply`
- [x] Mail, scheduled mail, threshold mail, export, SMS, and matching based on `RESEARCH_NO`
- [x] Client/contact management separated from old public-board data
- [x] Dashboard monthly mail/SMS usage and estimated cost
- [x] Retired public-board/new-admin routes and tables removed from active code
- [x] `document_srl` compatibility columns removed from local log tables
- [x] Old mail template variable `documentSrl` removed
- [x] Obsolete cleanup/migration SQL removed

## Remaining

- [ ] Decide whether any production-only retired tables still need manual backup before deletion.
- [ ] Remove empty directories if the IDE still shows deleted modules.
- [ ] Decide separately whether manual publish `public_document_srl` is still useful as an external post-number memo.
- [ ] Decide later whether ADD_COMMENT question/answer parsing should become structured data.
