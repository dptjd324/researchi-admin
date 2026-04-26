package com.researchi.admin.matching.mapper;

import com.researchi.admin.matching.domain.AdminKeywordMatchTarget;
import com.researchi.admin.matching.domain.MatchingTargetView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminKeywordMatchTargetMapper {

    void insert(AdminKeywordMatchTarget matchTarget);

    List<MatchingTargetView> findViewsByMatchJobId(@Param("matchJobId") Long matchJobId);

    List<AdminKeywordMatchTarget> findByMatchJobId(@Param("matchJobId") Long matchJobId);

    int updateNotificationState(
            @Param("id") Long id,
            @Param("notifyStatus") String notifyStatus,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("failReason") String failReason
    );
}
