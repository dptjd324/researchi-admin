package com.researchi.admin.xe.service;

import com.researchi.admin.job.domain.BoardConfig;
import com.researchi.admin.xe.domain.XeJobDocument;
import com.researchi.admin.xe.domain.XeModule;
import com.researchi.admin.xe.mapper.XeJobMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class XeJobService {

    private static final DateTimeFormatter XE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final XeJobMapper xeJobMapper;

    public XeJobService(XeJobMapper xeJobMapper) {
        this.xeJobMapper = xeJobMapper;
    }

    public List<XeModule> getJobModules() {
        return xeJobMapper.findJobModules(BoardConfig.managedMids());
    }

    public XeModule getModuleByMid(String mid) {
        return xeJobMapper.findModuleByMid(mid);
    }

    public List<XeJobDocument> getJobDocumentsByIds(List<Long> documentSrls) {
        if (documentSrls == null || documentSrls.isEmpty()) {
            return List.of();
        }
        return xeJobMapper.findJobDocumentsByIds(documentSrls, BoardConfig.managedMids());
    }

    public List<Long> getJobDocumentSrlsByTitle(String normalizedKeyword, List<String> keywordTokens) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return List.of();
        }
        return xeJobMapper.findJobDocumentSrlsByTitle(normalizedKeyword, keywordTokens, BoardConfig.managedMids());
    }

    public List<Long> getApplicationJobDocumentSrlsByTitle(String normalizedKeyword, List<String> keywordTokens) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return List.of();
        }
        return xeJobMapper.findJobDocumentSrlsByTitle(normalizedKeyword, keywordTokens, BoardConfig.applicationMids());
    }

    public List<XeJobDocument> getJobDocumentsPage(String mid, String normalizedKeyword, List<String> keywordTokens, int limit, int offset) {
        return xeJobMapper.findJobDocumentsPage(mid, normalizedKeyword, keywordTokens, BoardConfig.managedMids(), limit, offset);
    }

    public List<XeJobDocument> getApplicationJobDocumentsPage(String mid, String normalizedKeyword, List<String> keywordTokens, int limit, int offset) {
        return xeJobMapper.findJobDocumentsPage(mid, normalizedKeyword, keywordTokens, BoardConfig.applicationMids(), limit, offset);
    }

    public List<XeJobDocument> getJobDocumentsAfter(
            String mid,
            String normalizedKeyword,
            List<String> keywordTokens,
            Long afterDocumentSrl,
            int limit
    ) {
        return xeJobMapper.findJobDocumentsAfter(mid, normalizedKeyword, keywordTokens, BoardConfig.managedMids(), afterDocumentSrl, limit);
    }

    public int countJobDocuments(String mid, String normalizedKeyword, List<String> keywordTokens) {
        return xeJobMapper.countJobDocuments(mid, normalizedKeyword, keywordTokens, BoardConfig.managedMids());
    }

    public XeJobDocument getJobDocument(Long documentSrl) {
        return xeJobMapper.findJobDocumentById(documentSrl, BoardConfig.managedMids());
    }

    @Transactional("xeTransactionManager")
    public Long createJobDocument(String mid, String title, String content, String status, String ipAddress) {
        XeModule module = xeJobMapper.findModuleByMid(mid);
        if (module == null) {
            throw new IllegalArgumentException("공고 게시판을 찾을 수 없습니다.");
        }

        Long nextDocumentSrl = xeJobMapper.findNextDocumentSrl();
        if (nextDocumentSrl == null) {
            nextDocumentSrl = 1L;
        }

        Long nextListOrder = xeJobMapper.findNextListOrder();
        if (nextListOrder == null) {
            nextListOrder = -1L;
        }

        String now = now();
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(nextDocumentSrl);
        document.setModuleSrl(module.getModuleSrl());
        document.setTitle(title);
        document.setContent(content);
        document.setStatus(status);
        document.setRegdate(now);
        document.setLastUpdate(now);
        document.setIpAddress(ipAddress == null || ipAddress.isBlank() ? "127.0.0.1" : ipAddress);
        document.setListOrder(nextListOrder);
        xeJobMapper.insertJobDocument(document);
        return nextDocumentSrl;
    }

    @Transactional("xeTransactionManager")
    public void updateJobDocument(Long documentSrl, String mid, String title, String content, String status) {
        XeModule module = xeJobMapper.findModuleByMid(mid);
        if (module == null) {
            throw new IllegalArgumentException("공고 게시판을 찾을 수 없습니다.");
        }

        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setModuleSrl(module.getModuleSrl());
        document.setTitle(title);
        document.setContent(content);
        document.setStatus(status);
        document.setLastUpdate(now());
        xeJobMapper.updateJobDocument(document);
    }

    @Transactional("xeTransactionManager")
    public void updateJobStatus(Long documentSrl, String status) {
        xeJobMapper.updateJobDocumentStatus(documentSrl, status, now());
    }

    private String now() {
        return LocalDateTime.now().format(XE_DATE_FORMAT);
    }
}
