package com.researchi.admin.legacy.research.service;

import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchApplicationDuplicateGroup;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationExtraAnswerMapper;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationSearchIndexMapper;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.mapper.ResearchMasterMapper;
import com.researchi.admin.legacy.revision.service.LegacyRevisionLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResearchApplicationServiceTest {

    @Mock
    private ResearchApplicationMapper researchApplicationMapper;
    @Mock
    private LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper;
    @Mock
    private LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper;
    @Mock
    private ResearchMasterMapper researchMasterMapper;
    @Mock
    private LegacyRevisionLogService legacyRevisionLogService;

    @Test
    void completeProvisionDeduplicatesApplicationSeqsBeforeUpdating() {
        ResearchApplicationService service = new ResearchApplicationService(
                researchApplicationMapper,
                legacyApplicationExtraAnswerMapper,
                legacyApplicationSearchIndexMapper,
                researchMasterMapper,
                legacyRevisionLogService
        );
        ResearchApplication first = application(101L, "N");
        ResearchApplication duplicate = application(101L, "N");

        when(researchApplicationMapper.findUnprovidedByResearchNo(46408L)).thenReturn(List.of(first, duplicate));
        when(researchApplicationMapper.findByResearchNoAndSeq(46408L, 101L)).thenReturn(first);
        when(researchApplicationMapper.updateProvideYn(46408L, 101L, "Y")).thenReturn(2);

        int updatedCount = service.completeProvisionForUnprovided(46408L, null);

        assertThat(updatedCount).isEqualTo(1);
        verify(researchApplicationMapper).updateProvideYn(46408L, 101L, "Y");
    }

    @Test
    void getDuplicateGroupsReadsDuplicateApplicationKeys() {
        ResearchApplicationService service = new ResearchApplicationService(
                researchApplicationMapper,
                legacyApplicationExtraAnswerMapper,
                legacyApplicationSearchIndexMapper,
                researchMasterMapper,
                legacyRevisionLogService
        );
        ResearchApplicationDuplicateGroup duplicateGroup = new ResearchApplicationDuplicateGroup();
        duplicateGroup.setResearchNo(46408L);
        duplicateGroup.setResearchAppSeq(101L);
        duplicateGroup.setDuplicateCount(2);
        when(researchApplicationMapper.findDuplicateGroupsByResearchNo(46408L)).thenReturn(List.of(duplicateGroup));

        List<ResearchApplicationDuplicateGroup> duplicateGroups = service.getDuplicateGroups(46408L);

        assertThat(duplicateGroups).singleElement().satisfies(group -> {
            assertThat(group.getResearchNo()).isEqualTo(46408L);
            assertThat(group.getResearchAppSeq()).isEqualTo(101L);
            assertThat(group.getDuplicateCount()).isEqualTo(2);
        });
    }

    @Test
    void matchingCandidatePageEnrichesEmailFromSearchIndex() {
        ResearchApplicationService service = new ResearchApplicationService(
                researchApplicationMapper,
                legacyApplicationExtraAnswerMapper,
                legacyApplicationSearchIndexMapper,
                researchMasterMapper,
                legacyRevisionLogService
        );
        ResearchApplication candidate = application(101L, "N");
        ResearchApplication indexed = application(101L, "N");
        indexed.setAppEmail("applicant@example.com");
        when(researchApplicationMapper.findMatchingIndexCandidatePage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(candidate));
        when(legacyApplicationSearchIndexMapper.findByResearchNoAndSeq(46408L, 101L)).thenReturn(indexed);

        List<ResearchApplication> candidates = service.getMatchingIndexCandidatePage(
                com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition.empty(),
                100,
                0
        );

        assertThat(candidates).singleElement()
                .extracting(ResearchApplication::getAppEmail)
                .isEqualTo("applicant@example.com");
    }

    @Test
    void countUnprovidedAutomaticallyMarksBlacklistedApplicationsAsProvided() {
        ResearchApplicationService service = new ResearchApplicationService(
                researchApplicationMapper,
                legacyApplicationExtraAnswerMapper,
                legacyApplicationSearchIndexMapper,
                researchMasterMapper,
                legacyRevisionLogService
        );
        ResearchApplication blacklisted = application(101L, "N");
        when(researchApplicationMapper.findBlacklistedUnprovidedSeqsByResearchNo(46408L)).thenReturn(List.of(101L));
        when(researchApplicationMapper.findByResearchNoAndSeq(46408L, 101L)).thenReturn(blacklisted);
        when(researchApplicationMapper.updateProvideYn(46408L, 101L, "Y")).thenReturn(1);
        when(researchApplicationMapper.countUnprovidedByResearchNo(46408L)).thenReturn(0);

        int count = service.countUnprovidedApplications(46408L);

        assertThat(count).isEqualTo(0);
        verify(researchApplicationMapper).updateProvideYn(46408L, 101L, "Y");
    }

    private ResearchApplication application(Long researchAppSeq, String provideYn) {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(46408L);
        application.setResearchAppSeq(researchAppSeq);
        application.setProvideYn(provideYn);
        return application;
    }
}
