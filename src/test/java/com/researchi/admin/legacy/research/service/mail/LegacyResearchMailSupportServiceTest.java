package com.researchi.admin.legacy.research.service.mail;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.ResearchApplicationService;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.mapper.AdminMailApplicationClaimMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyResearchMailSupportServiceTest {

    @Mock
    private AdminMailTemplateMapper adminMailTemplateMapper;
    @Mock
    private AdminMailSendJobMapper adminMailSendJobMapper;
    @Mock
    private AdminMailSendTargetMapper adminMailSendTargetMapper;
    @Mock
    private AdminMailApplicationClaimMapper adminMailApplicationClaimMapper;
    @Mock
    private ResearchApplicationMapper researchApplicationMapper;
    @Mock
    private ResearchApplicationService researchApplicationService;
    @Mock
    private AdminActionLogService adminActionLogService;

    @Test
    void validateScheduledAtAcceptsTwoMinutesLater() {
        service().validateScheduledAt(LocalDateTime.now().plusMinutes(3).truncatedTo(ChronoUnit.MINUTES));
    }

    @Test
    void validateScheduledAtRejectsLessThanTwoMinutesLater() {
        LocalDateTime tooSoon = LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);

        assertThatThrownBy(() -> service().validateScheduledAt(tooSoon))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 2분");
    }

    @Test
    void resolveDailyScheduledAtUsesTodayWhenTimeIsSafelyFuture() {
        LocalDateTime requested = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime resolved = service().resolveDailyScheduledAt(requested.toLocalTime());

        assertThat(resolved.toLocalDate()).isEqualTo(requested.toLocalDate());
    }

    @Test
    void resolveDailyScheduledAtUsesTomorrowWhenTimeAlreadyPassed() {
        LocalDateTime resolved = service().resolveDailyScheduledAt(LocalTime.now().minusHours(1).truncatedTo(ChronoUnit.MINUTES));

        assertThat(resolved.toLocalDate()).isEqualTo(LocalDateTime.now().plusDays(1).toLocalDate());
    }

    @Test
    void buildDispatchRequestUsesDefaultSubjectAndProvidedApplicantBody() {
        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(46408L);
        research.setResearchTitle("테스트 좌담회");
        ResearchApplication applicant = new ResearchApplication();
        applicant.setAppName("홍길동");
        applicant.setAppSex("1");
        applicant.setAppBirth("19900101");
        applicant.setAppAge("35");
        applicant.setAppJob("회사원");
        applicant.setAppCompany("리서치아이");
        applicant.setAppHphone("01012345678");
        applicant.setAppTele("0212345678");
        applicant.setAppAddr("서울");
        applicant.setAddComment("추가 답변");
        when(researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(46408L, List.of(101L))).thenReturn(List.of(applicant));

        var request = service().buildDispatchRequest(
                research,
                List.of("client@example.com"),
                new LegacyResearchMailContent(null, "ignored", ""),
                new ExportPayload("research.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1}, 1),
                MailAttachmentType.XLSX,
                1,
                List.of(101L)
        );

        assertThat(request.subject()).isEqualTo("테스트 좌담회 - 소개자 하진혁(010-2875-3457)");
        assertThat(request.body()).contains("성명/성별/생년월일/나이(만)/직업/회사 학교/휴대폰/유선전화/주소/추가기재사항");
        assertThat(request.body()).contains("추가기재사항" + System.lineSeparator() + System.lineSeparator() + "홍길동");
        assertThat(request.body()).contains("홍길동/남자/19900101/35/회사원/리서치아이/010-1234-5678/02-1234-5678/서울/추가 답변");
    }

    private LegacyResearchMailSupportService service() {
        return new LegacyResearchMailSupportService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailApplicationClaimMapper,
                researchApplicationMapper,
                researchApplicationService,
                adminActionLogService
        );
    }
}
