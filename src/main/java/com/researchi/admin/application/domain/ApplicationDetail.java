package com.researchi.admin.application.domain;

import com.researchi.admin.job.domain.JobDetail;

import java.util.List;

public record ApplicationDetail(
        ApplicationRecord application,
        JobDetail jobDetail,
        List<ApplicationAnswerItem> answers,
        List<ApplicationExtraAnswerItem> extraAnswers
) {
}
