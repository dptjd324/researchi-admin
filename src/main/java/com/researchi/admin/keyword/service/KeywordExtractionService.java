package com.researchi.admin.keyword.service;

import com.researchi.admin.application.domain.ApplicationAnswerItem;
import com.researchi.admin.application.domain.ApplicationDetail;
import com.researchi.admin.application.domain.ApplicationExtraAnswerItem;
import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.service.ApplicationService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.keyword.config.KeywordProperties;
import com.researchi.admin.keyword.domain.AdminApplicationKeyword;
import com.researchi.admin.keyword.domain.AdminJobKeyword;
import com.researchi.admin.keyword.domain.KeywordCandidate;
import com.researchi.admin.keyword.mapper.AdminApplicationKeywordMapper;
import com.researchi.admin.keyword.mapper.AdminJobKeywordMapper;
import com.researchi.admin.xe.domain.XeJobDocument;
import com.researchi.admin.xe.service.XeJobService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KeywordExtractionService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "with", "that", "this", "from", "have", "your",
            "you", "are", "for", "job", "jobs", "apply", "application"
    );

    private final KeywordProperties keywordProperties;
    private final AdminApplicationKeywordMapper adminApplicationKeywordMapper;
    private final AdminJobKeywordMapper adminJobKeywordMapper;
    private final ApplicationService applicationService;
    private final XeJobService xeJobService;
    private final AdminJobMetaMapper adminJobMetaMapper;

    public KeywordExtractionService(
            KeywordProperties keywordProperties,
            AdminApplicationKeywordMapper adminApplicationKeywordMapper,
            AdminJobKeywordMapper adminJobKeywordMapper,
            @Lazy ApplicationService applicationService,
            XeJobService xeJobService,
            AdminJobMetaMapper adminJobMetaMapper
    ) {
        this.keywordProperties = keywordProperties;
        this.adminApplicationKeywordMapper = adminApplicationKeywordMapper;
        this.adminJobKeywordMapper = adminJobKeywordMapper;
        this.applicationService = applicationService;
        this.xeJobService = xeJobService;
        this.adminJobMetaMapper = adminJobMetaMapper;
    }

    @Transactional("adminTransactionManager")
    public List<KeywordCandidate> syncApplicationKeywords(Long applicationId) {
        ApplicationDetail detail = applicationService.getApplicationDetail(applicationId);
        return syncApplicationKeywords(applicationId, detail.application(), detail.answers(), detail.extraAnswers());
    }

    @Transactional("adminTransactionManager")
    public List<KeywordCandidate> syncApplicationKeywords(
            Long applicationId,
            ApplicationRecord application,
            List<ApplicationAnswerItem> answers
    ) {
        return syncApplicationKeywords(applicationId, application, answers, List.of());
    }

    @Transactional("adminTransactionManager")
    public List<KeywordCandidate> syncApplicationKeywords(
            Long applicationId,
            ApplicationRecord application,
            List<ApplicationAnswerItem> answers,
            List<ApplicationExtraAnswerItem> extraAnswers
    ) {
        List<KeywordCandidate> keywords = extractApplicationKeywords(application, answers, extraAnswers);
        adminApplicationKeywordMapper.deleteByApplicationId(applicationId);
        for (KeywordCandidate keyword : keywords) {
            AdminApplicationKeyword row = new AdminApplicationKeyword();
            row.setApplicationId(applicationId);
            row.setKeyword(keyword.keyword());
            row.setKeywordNormalized(keyword.normalized());
            row.setSourceType(keyword.sourceType());
            adminApplicationKeywordMapper.insert(row);
        }
        return keywords;
    }

    @Transactional("adminTransactionManager")
    public List<KeywordCandidate> syncJobKeywords(Long documentSrl) {
        XeJobDocument jobDocument = xeJobService.getJobDocument(documentSrl);
        if (jobDocument == null) {
            throw new IllegalArgumentException("공고를 찾을 수 없습니다.");
        }
        AdminJobMeta jobMeta = adminJobMetaMapper.findByDocumentSrl(documentSrl);
        List<KeywordCandidate> keywords = extractJobKeywords(jobDocument, jobMeta);
        adminJobKeywordMapper.deleteByDocumentSrl(documentSrl);
        for (KeywordCandidate keyword : keywords) {
            AdminJobKeyword row = new AdminJobKeyword();
            row.setDocumentSrl(documentSrl);
            row.setKeyword(keyword.keyword());
            row.setKeywordNormalized(keyword.normalized());
            row.setSourceType(keyword.sourceType());
            adminJobKeywordMapper.insert(row);
        }
        return keywords;
    }

    public List<KeywordCandidate> getJobKeywords(Long documentSrl) {
        return adminJobKeywordMapper.findByDocumentSrl(documentSrl).stream()
                .map(row -> new KeywordCandidate(row.getKeyword(), row.getKeywordNormalized(), row.getSourceType()))
                .toList();
    }

    public Map<Long, List<KeywordCandidate>> getApplicationKeywords(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<KeywordCandidate>> grouped = new LinkedHashMap<>();
        for (AdminApplicationKeyword row : adminApplicationKeywordMapper.findByApplicationIds(applicationIds)) {
            grouped.computeIfAbsent(row.getApplicationId(), ignored -> new ArrayList<>())
                    .add(new KeywordCandidate(row.getKeyword(), row.getKeywordNormalized(), row.getSourceType()));
        }
        return grouped;
    }

    List<KeywordCandidate> extractApplicationKeywords(
            ApplicationRecord application,
            List<ApplicationAnswerItem> answers
    ) {
        return extractApplicationKeywords(application, answers, List.of());
    }

    List<KeywordCandidate> extractApplicationKeywords(
            ApplicationRecord application,
            List<ApplicationAnswerItem> answers,
            List<ApplicationExtraAnswerItem> extraAnswers
    ) {
        LinkedHashMap<String, KeywordCandidate> deduplicated = new LinkedHashMap<>();
        addKeywords(deduplicated, application.getApplicantName(), "APPLICATION_NAME");
        addKeywords(deduplicated, application.getJobText(), "APPLICATION_JOB");
        addKeywords(deduplicated, application.getOrganizationText(), "APPLICATION_ORG");
        addKeywords(deduplicated, application.getRegionText(), "APPLICATION_REGION");
        addKeywords(deduplicated, application.getExtraComment(), "APPLICATION_COMMENT");
        addKeywords(deduplicated, application.getPriorResearchText(), "APPLICATION_PRIOR");
        for (ApplicationAnswerItem answer : answers) {
            addKeywords(deduplicated, answer.getDisplayAnswer(), "DYNAMIC_ANSWER");
        }
        for (ApplicationExtraAnswerItem extraAnswer : extraAnswers) {
            addKeywords(deduplicated, extraAnswer.getAnswerText(), "APPLICATION_EXTRA_ANSWER");
        }
        return List.copyOf(deduplicated.values());
    }

    List<KeywordCandidate> extractJobKeywords(XeJobDocument jobDocument, AdminJobMeta meta) {
        LinkedHashMap<String, KeywordCandidate> deduplicated = new LinkedHashMap<>();
        addKeywords(deduplicated, jobDocument.getTitle(), "JOB_TITLE");
        addKeywords(deduplicated, jobDocument.getContent(), "JOB_CONTENT");
        if (meta != null) {
            addKeywords(deduplicated, meta.getRewardText(), "JOB_META");
            addKeywords(deduplicated, meta.getPlaceText(), "JOB_META");
            addKeywords(deduplicated, meta.getRegionText(), "JOB_META");
            addKeywords(deduplicated, meta.getBrandText(), "JOB_META");
            addKeywords(deduplicated, meta.getApplicationFormNotice(), "JOB_META");
            addKeywords(deduplicated, meta.getInternalMemo(), "JOB_META");
        }
        return List.copyOf(deduplicated.values());
    }

    private void addKeywords(Map<String, KeywordCandidate> target, String text, String sourceType) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(Normalizer.normalize(text, Normalizer.Form.NFKC));
        int added = 0;
        while (matcher.find()) {
            String token = matcher.group().trim();
            String normalized = normalizeToken(token);
            if (normalized == null || target.containsKey(normalized)) {
                continue;
            }
            target.put(normalized, new KeywordCandidate(token, normalized, sourceType));
            added++;
            if (added >= keywordProperties.getMaxKeywordsPerSource()) {
                break;
            }
        }
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < keywordProperties.getMinLength() || normalized.length() > keywordProperties.getMaxLength()) {
            return null;
        }
        if (STOP_WORDS.contains(normalized)) {
            return null;
        }
        if (normalized.chars().allMatch(Character::isDigit) && normalized.length() > 4) {
            return null;
        }
        Set<Character> distinct = new LinkedHashSet<>();
        for (char current : normalized.toCharArray()) {
            distinct.add(current);
        }
        return distinct.size() == 1 ? null : normalized;
    }
}
