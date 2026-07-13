package com.researchi.admin.legacy.matching.domain;

import com.researchi.admin.legacy.research.domain.ResearchApplication;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegacyMatchingSearchCondition {

    private static final Pattern BIRTH_YEAR_RANGE_PATTERN = Pattern.compile("^(\\d{4})\\s*[-~]\\s*(\\d{4})$");

    private final String appSex;
    private final String appBirth;
    private final String appJob;
    private final String appCompany;
    private final String appAddr;
    private final String addComment;
    private final List<Long> additionalAnswerResearchAppSeqs;

    private final List<String> appSexCodes;
    private final List<String> appBirthYears;
    private final List<String> appBirthYearSuffixes;
    private final List<String> appBirthTerms;
    private final List<String> appJobTerms;
    private final List<String> appCompanyTerms;
    private final List<String> appAddrTerms;
    private final List<String> addCommentTerms;

    public LegacyMatchingSearchCondition() {
        this(null, null, null, null, null, null);
    }

    public LegacyMatchingSearchCondition(
            String appSex,
            String appBirth,
            String appJob,
            String appCompany,
            String appAddr,
            String addComment
    ) {
        this(appSex, appBirth, appJob, appCompany, appAddr, addComment, List.of());
    }

    private LegacyMatchingSearchCondition(
            String appSex,
            String appBirth,
            String appJob,
            String appCompany,
            String appAddr,
            String addComment,
            List<Long> additionalAnswerResearchAppSeqs
    ) {
        this.appSex = trimToEmpty(appSex);
        this.appBirth = trimToEmpty(appBirth);
        this.appJob = trimToEmpty(appJob);
        this.appCompany = trimToEmpty(appCompany);
        this.appAddr = trimToEmpty(appAddr);
        this.addComment = trimToEmpty(addComment);
        this.additionalAnswerResearchAppSeqs = distinctPositiveLongs(additionalAnswerResearchAppSeqs);
        this.appSexCodes = parseSexCodes(this.appSex);
        ParsedBirthTerms birthTerms = parseBirthTerms(this.appBirth);
        this.appBirthYears = birthTerms.years();
        this.appBirthYearSuffixes = birthYearSuffixes(this.appBirthYears);
        this.appBirthTerms = birthTerms.terms();
        this.appJobTerms = parseTerms(this.appJob);
        this.appCompanyTerms = parseTerms(this.appCompany);
        this.appAddrTerms = parseTerms(this.appAddr);
        this.addCommentTerms = parseTerms(this.addComment);
    }

    public static LegacyMatchingSearchCondition empty() {
        return new LegacyMatchingSearchCondition();
    }

    public static LegacyMatchingSearchCondition fromStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return empty();
        }
        String appSex = null;
        String appBirth = null;
        String appJob = null;
        String appCompany = null;
        String appAddr = null;
        String addComment = null;
        for (String pair : storageKey.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String name = decode(pair.substring(0, separator));
            String value = decode(pair.substring(separator + 1));
            switch (name) {
                case "appSex" -> appSex = value;
                case "appBirth" -> appBirth = value;
                case "appJob" -> appJob = value;
                case "appCompany" -> appCompany = value;
                case "appAddr" -> appAddr = value;
                case "addComment" -> addComment = value;
                default -> {
                }
            }
        }
        return new LegacyMatchingSearchCondition(appSex, appBirth, appJob, appCompany, appAddr, addComment);
    }

    public boolean hasInput() {
        return !appSex.isBlank()
                || !appBirth.isBlank()
                || !appJob.isBlank()
                || !appCompany.isBlank()
                || !appAddr.isBlank()
                || !addComment.isBlank();
    }

    public int requiredFilterCount() {
        int count = 0;
        if (!appSexCodes.isEmpty()) {
            count++;
        }
        if (!appBirthYears.isEmpty() || !appBirthTerms.isEmpty()) {
            count++;
        }
        if (!appJobTerms.isEmpty()) {
            count++;
        }
        if (!appCompanyTerms.isEmpty()) {
            count++;
        }
        if (!appAddrTerms.isEmpty()) {
            count++;
        }
        if (!addCommentTerms.isEmpty()) {
            count++;
        }
        return count;
    }

    public String storageKey() {
        List<String> pairs = new ArrayList<>();
        addPair(pairs, "appSex", appSex);
        addPair(pairs, "appBirth", appBirth);
        addPair(pairs, "appJob", appJob);
        addPair(pairs, "appCompany", appCompany);
        addPair(pairs, "appAddr", appAddr);
        addPair(pairs, "addComment", addComment);
        return String.join("&", pairs);
    }

    public LegacyMatchingSearchCondition withAdditionalAnswerResearchAppSeqs(List<Long> researchAppSeqs) {
        return new LegacyMatchingSearchCondition(appSex, appBirth, appJob, appCompany, appAddr, addComment, researchAppSeqs);
    }

    public List<String> displayFilters() {
        List<String> filters = new ArrayList<>();
        addDisplay(filters, "성별", appSex);
        addDisplay(filters, "생년월일", appBirth);
        addDisplay(filters, "직업", appJob);
        addDisplay(filters, "회사/학교", appCompany);
        addDisplay(filters, "주소", appAddr);
        addDisplay(filters, "추가기재사항", addComment);
        return filters;
    }

    public List<String> matchedFilters(ResearchApplication application) {
        if (application == null) {
            return List.of();
        }
        List<String> matched = new ArrayList<>();
        if (!appSexCodes.isEmpty() && appSexCodes.contains(trimToEmpty(application.getAppSex()))) {
            matched.add("성별: " + appSex);
        }
        if ((!appBirthYears.isEmpty() || !appBirthTerms.isEmpty()) && birthMatches(application.getAppBirth())) {
            matched.add("생년월일: " + appBirth);
        }
        addMatchedText(matched, "직업", appJob, appJobTerms, application.getAppJob());
        addMatchedText(matched, "회사/학교", appCompany, appCompanyTerms, application.getAppCompany());
        addMatchedText(matched, "주소", appAddr, appAddrTerms, application.getAppAddr());
        addMatchedText(matched, "추가기재사항", addComment, addCommentTerms, application.getAddComment());
        return matched;
    }

    public boolean hasAddCommentTerms() {
        return !addCommentTerms.isEmpty();
    }

    public boolean addCommentMatches(String value) {
        return textMatchesAny(addCommentTerms, value);
    }

    public String addCommentDisplayFilter() {
        return "추가기재사항: " + addComment;
    }

    public String getAppSex() {
        return appSex;
    }

    public String getAppBirth() {
        return appBirth;
    }

    public String getAppJob() {
        return appJob;
    }

    public String getAppCompany() {
        return appCompany;
    }

    public String getAppAddr() {
        return appAddr;
    }

    public String getAddComment() {
        return addComment;
    }

    public List<String> getAppSexCodes() {
        return appSexCodes;
    }

    public List<String> getAppBirthYears() {
        return appBirthYears;
    }

    public List<String> getAppBirthYearSuffixes() {
        return appBirthYearSuffixes;
    }

    public List<String> getAppBirthTerms() {
        return appBirthTerms;
    }

    public List<String> getAppJobTerms() {
        return appJobTerms;
    }

    public List<String> getAppCompanyTerms() {
        return appCompanyTerms;
    }

    public List<String> getAppAddrTerms() {
        return appAddrTerms;
    }

    public List<String> getAddCommentTerms() {
        return addCommentTerms;
    }

    public List<Long> getAdditionalAnswerResearchAppSeqs() {
        return additionalAnswerResearchAppSeqs;
    }

    private boolean birthMatches(String value) {
        String normalized = normalizeDigits(value);
        for (String year : appBirthYears) {
            if (normalized.startsWith(year)) {
                return true;
            }
        }
        if (normalized.length() == 6) {
            for (String suffix : appBirthYearSuffixes) {
                if (normalized.startsWith(suffix)) {
                    return true;
                }
            }
        }
        String text = normalizeText(value);
        for (String term : appBirthTerms) {
            if (text.contains(normalizeText(term)) || normalized.contains(normalizeDigits(term))) {
                return true;
            }
        }
        return false;
    }

    private static void addMatchedText(List<String> matched, String label, String rawValue, List<String> terms, String target) {
        if (terms.isEmpty()) {
            return;
        }
        if (textMatchesAny(terms, target)) {
            matched.add(label + ": " + rawValue);
        }
    }

    private static boolean textMatchesAny(List<String> terms, String target) {
        if (terms == null || terms.isEmpty()) {
            return false;
        }
        String normalizedTarget = normalizeText(target);
        for (String term : terms) {
            if (normalizedTarget.contains(normalizeText(term))) {
                return true;
            }
        }
        return false;
    }

    private static void addPair(List<String> pairs, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        pairs.add(encode(name) + "=" + encode(value));
    }

    private static void addDisplay(List<String> filters, String label, String value) {
        if (value != null && !value.isBlank()) {
            filters.add(label + ": " + value);
        }
    }

    private static List<String> parseSexCodes(String value) {
        List<String> terms = parseTerms(value);
        Set<String> codes = new LinkedHashSet<>();
        for (String term : terms) {
            String normalized = normalizeText(term);
            if ("1".equals(normalized) || "m".equals(normalized) || "male".equals(normalized)
                    || "남".equals(normalized) || "남자".equals(normalized) || "남성".equals(normalized)) {
                codes.add("1");
            } else if ("2".equals(normalized) || "f".equals(normalized) || "female".equals(normalized)
                    || "여".equals(normalized) || "여자".equals(normalized) || "여성".equals(normalized)) {
                codes.add("2");
            } else {
                codes.add(term);
            }
        }
        return List.copyOf(codes);
    }

    private static ParsedBirthTerms parseBirthTerms(String value) {
        List<String> years = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        for (String term : parseTerms(value)) {
            Matcher rangeMatcher = BIRTH_YEAR_RANGE_PATTERN.matcher(term);
            if (rangeMatcher.matches()) {
                addBirthYearRange(years, rangeMatcher.group(1), rangeMatcher.group(2));
                continue;
            }
            String digits = normalizeDigits(term);
            if (digits.matches("\\d{4}")) {
                years.add(digits);
            } else {
                terms.add(term);
            }
        }
        return new ParsedBirthTerms(List.copyOf(years), List.copyOf(terms));
    }

    private static void addBirthYearRange(List<String> years, String startYear, String endYear) {
        int start = Integer.parseInt(startYear);
        int end = Integer.parseInt(endYear);
        if (start > end) {
            int swap = start;
            start = end;
            end = swap;
        }
        for (int year = start; year <= end; year++) {
            years.add(String.valueOf(year));
        }
    }

    private static List<String> birthYearSuffixes(List<String> years) {
        if (years == null || years.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> suffixes = new LinkedHashSet<>();
        for (String year : years) {
            if (year != null && year.length() == 4) {
                suffixes.add(year.substring(2));
            }
        }
        return List.copyOf(suffixes);
    }

    private static List<String> parseTerms(String value) {
        String normalized = trimToEmpty(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String term : normalized.split(",")) {
            String trimmed = term.trim();
            if (!trimmed.isBlank()) {
                terms.add(trimmed);
            }
        }
        return List.copyOf(terms);
    }

    private static List<Long> distinctPositiveLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueValues = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null && value > 0) {
                uniqueValues.add(value);
            }
        }
        return List.copyOf(uniqueValues);
    }

    private static String normalizeText(String value) {
        return Normalizer.normalize(trimToEmpty(value), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private static String normalizeDigits(String value) {
        return trimToEmpty(value).replaceAll("[^0-9]", "");
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedBirthTerms(List<String> years, List<String> terms) {
    }
}
