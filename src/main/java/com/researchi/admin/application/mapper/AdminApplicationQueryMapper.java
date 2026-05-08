package com.researchi.admin.application.mapper;

import com.researchi.admin.application.domain.ApplicationAnswerItem;
import com.researchi.admin.application.domain.ApplicationJobCount;
import com.researchi.admin.application.domain.ApplicationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminApplicationQueryMapper {

    List<ApplicationRecord> findAll();

    List<ApplicationRecord> findByDocumentSrl(@Param("documentSrl") Long documentSrl);

    List<ApplicationRecord> findPage(
            @Param("documentSrl") Long documentSrl,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ApplicationRecord> findSearchPage(
            @Param("documentSrl") Long documentSrl,
            @Param("keyword") String keyword,
            @Param("jobDocumentSrls") List<Long> jobDocumentSrls,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    int count(@Param("documentSrl") Long documentSrl);

    int countSearch(
            @Param("documentSrl") Long documentSrl,
            @Param("keyword") String keyword,
            @Param("jobDocumentSrls") List<Long> jobDocumentSrls
    );

    List<ApplicationJobCount> countByDocumentSrl();

    ApplicationRecord findById(@Param("id") Long id);

    List<ApplicationAnswerItem> findAnswersByApplicationId(@Param("applicationId") Long applicationId);

    int updateStatus(@Param("id") Long id, @Param("applicationStatus") String applicationStatus);

    int updateBlacklistState(
            @Param("id") Long id,
            @Param("applicationStatus") String applicationStatus,
            @Param("blackModeApplied") String blackModeApplied
    );

    int clearBlacklistState(@Param("id") Long id);

    int restoreBlacklistApplications(
            @Param("blacklistId") Long blacklistId,
            @Param("applicationStatus") String applicationStatus
    );

    int restoreBlacklistApplicationsByProfile(
            @Param("blacklistId") Long blacklistId,
            @Param("blackName") String blackName,
            @Param("blackBirthDate") LocalDate blackBirthDate,
            @Param("blackModeApplied") String blackModeApplied,
            @Param("applicationStatus") String applicationStatus
    );
}
