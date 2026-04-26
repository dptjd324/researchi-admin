package com.researchi.admin.application.mapper;

import com.researchi.admin.application.domain.ApplicationAnswerItem;
import com.researchi.admin.application.domain.ApplicationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminApplicationQueryMapper {

    List<ApplicationRecord> findAll();

    List<ApplicationRecord> findByDocumentSrl(@Param("documentSrl") Long documentSrl);

    ApplicationRecord findById(@Param("id") Long id);

    List<ApplicationAnswerItem> findAnswersByApplicationId(@Param("applicationId") Long applicationId);

    int updateStatus(@Param("id") Long id, @Param("applicationStatus") String applicationStatus);

    int updateBlacklistState(
            @Param("id") Long id,
            @Param("applicationStatus") String applicationStatus,
            @Param("blackModeApplied") String blackModeApplied
    );
}
