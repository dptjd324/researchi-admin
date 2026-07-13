package com.researchi.admin.legacy.research.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.mapper.AdminExportLogMapper;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationExtraAnswerMapper;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationSearchIndexMapper;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyResearchExportServiceTest {

    @Mock
    private ResearchApplicationMapper researchApplicationMapper;
    @Mock
    private LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper;
    @Mock
    private LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper;
    @Mock
    private ResearchMasterService researchMasterService;
    @Mock
    private AdminExportLogMapper adminExportLogMapper;
    @Mock
    private AdminActionLogService adminActionLogService;

    @Test
    void prepareXlsxWritesKoreanGenderLabels() throws Exception {
        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(46408L);
        research.setResearchTitle("테스트 좌담회");

        ResearchApplication male = application(1L, "1");
        ResearchApplication female = application(2L, "2");

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research);
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(male, female));

        ExportPayload payload = service().prepareXlsx(46408L, null);

        assertThat(payload.fileName()).contains("2명 소개자 하진혁");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(payload.content()))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue()).isEqualTo("남자");
            assertThat(workbook.getSheetAt(0).getRow(2).getCell(2).getStringCellValue()).isEqualTo("여자");
        }
    }

    private LegacyResearchExportService service() {
        return new LegacyResearchExportService(
                researchApplicationMapper,
                legacyApplicationExtraAnswerMapper,
                legacyApplicationSearchIndexMapper,
                researchMasterService,
                adminExportLogMapper,
                adminActionLogService
        );
    }

    private ResearchApplication application(Long seq, String sex) {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(46408L);
        application.setResearchAppSeq(seq);
        application.setAppName("신청자" + seq);
        application.setAppSex(sex);
        application.setAppBirth("900101");
        application.setAppAge("36");
        application.setAppHphone("01012345678");
        application.setProvideYn("N");
        return application;
    }
}
