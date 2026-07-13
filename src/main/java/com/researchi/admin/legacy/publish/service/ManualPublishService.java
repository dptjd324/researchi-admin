package com.researchi.admin.legacy.publish.service;

import com.researchi.admin.legacy.publish.domain.ManualPublishContent;
import com.researchi.admin.legacy.publish.domain.ManualPublishLog;
import com.researchi.admin.legacy.publish.mapper.ManualPublishLogMapper;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.web.support.TextLinkRenderer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ManualPublishService {

    private static final String STATUS_PUBLISHED = "MANUAL_PUBLISHED";

    private final ManualPublishLogMapper manualPublishLogMapper;
    private final TextLinkRenderer textLinkRenderer;

    public ManualPublishService(ManualPublishLogMapper manualPublishLogMapper, TextLinkRenderer textLinkRenderer) {
        this.manualPublishLogMapper = manualPublishLogMapper;
        this.textLinkRenderer = textLinkRenderer;
    }

    public ManualPublishContent generateContent(ResearchMaster researchMaster) {
        if (researchMaster == null) {
            throw new IllegalArgumentException("researchMaster is required.");
        }
        String title = trimToEmpty(researchMaster.getResearchTitle());
        List<String> bodyParts = new ArrayList<>();
        addIfPresent(bodyParts, researchMaster.getResearchContents());
        addIfPresent(bodyParts, researchMaster.getAddComment());
        String body = String.join("\n\n", bodyParts);
        return new ManualPublishContent(title, body, textLinkRenderer.render(body));
    }

    public ManualPublishLog getLatestLog(Long researchNo) {
        if (researchNo == null) {
            return null;
        }
        return manualPublishLogMapper.findLatestByResearchNo(researchNo);
    }

    public void recordPublished(ResearchMaster researchMaster, Long publicDocumentSrl, Long publishedBy) {
        ManualPublishContent content = generateContent(researchMaster);
        ManualPublishLog log = new ManualPublishLog();
        log.setResearchNo(researchMaster.getResearchNo());
        log.setGeneratedTitle(content.getTitle());
        log.setGeneratedBody(content.getBody());
        log.setPublishStatus(STATUS_PUBLISHED);
        log.setPublicDocumentSrl(publicDocumentSrl);
        log.setPublishedBy(publishedBy);
        manualPublishLogMapper.insert(log);
    }

    private void addIfPresent(List<String> values, String value) {
        String normalized = trimToEmpty(value);
        if (!normalized.isEmpty()) {
            values.add(normalized);
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
