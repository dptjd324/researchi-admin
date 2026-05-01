package com.researchi.admin.xe.service;

import com.researchi.admin.job.domain.BoardConfig;
import com.researchi.admin.xe.domain.XeJobDocument;
import com.researchi.admin.xe.domain.XeModule;
import com.researchi.admin.xe.mapper.XeJobMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class XeJobService {

    private static final DateTimeFormatter XE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int CREATE_RETRY_LIMIT = 3;

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

        DuplicateKeyException lastDuplicateKeyException = null;
        for (int attempt = 1; attempt <= CREATE_RETRY_LIMIT; attempt++) {
            Long nextDocumentSrl = nextDocumentSrl();
            Long nextListOrder = nextListOrder();
            XeJobDocument document = newJobDocument(module, nextDocumentSrl, nextListOrder, title, content, status, ipAddress);
            try {
                xeJobMapper.insertJobDocument(document);
                return nextDocumentSrl;
            } catch (DuplicateKeyException ex) {
                lastDuplicateKeyException = ex;
            }
        }
        throw lastDuplicateKeyException;
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
        int updated = xeJobMapper.updateJobDocument(document, BoardConfig.managedMids());
        if (updated == 0) {
            throw new IllegalArgumentException("공고 게시판을 찾을 수 없습니다.");
        }
    }

    @Transactional("xeTransactionManager")
    public void updateJobStatus(Long documentSrl, String status) {
        int updated = xeJobMapper.updateJobDocumentStatus(documentSrl, status, now(), BoardConfig.managedMids());
        if (updated == 0) {
            throw new IllegalArgumentException("공고 게시판을 찾을 수 없습니다.");
        }
    }

    @Transactional("xeTransactionManager")
    public void deleteJobDocument(Long documentSrl) {
        int deleted = xeJobMapper.deleteJobDocument(documentSrl, BoardConfig.managedMids());
        if (deleted == 0) {
            throw new IllegalArgumentException("공고 게시판을 찾을 수 없습니다.");
        }
    }

    @Transactional("xeTransactionManager")
    public boolean deleteJobDocumentIfPresent(Long documentSrl) {
        return xeJobMapper.deleteJobDocument(documentSrl, BoardConfig.managedMids()) > 0;
    }

    private Long nextDocumentSrl() {
        Long nextDocumentSrl = xeJobMapper.findNextDocumentSrl();
        return nextDocumentSrl == null ? 1L : nextDocumentSrl;
    }

    private Long nextListOrder() {
        Long nextListOrder = xeJobMapper.findNextListOrder();
        return nextListOrder == null ? -1L : nextListOrder;
    }

    private XeJobDocument newJobDocument(
            XeModule module,
            Long documentSrl,
            Long listOrder,
            String title,
            String content,
            String status,
            String ipAddress
    ) {
        String now = now();
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setModuleSrl(module.getModuleSrl());
        document.setTitle(title);
        document.setContent(content);
        document.setStatus(status);
        document.setRegdate(now);
        document.setLastUpdate(now);
        document.setIpAddress(ipAddress == null || ipAddress.isBlank() ? "127.0.0.1" : ipAddress);
        document.setListOrder(listOrder);
        return document;
    }

    private String now() {
        return LocalDateTime.now().format(XE_DATE_FORMAT);
    }
}
