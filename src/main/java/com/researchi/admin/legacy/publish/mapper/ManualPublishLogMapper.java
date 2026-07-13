package com.researchi.admin.legacy.publish.mapper;

import com.researchi.admin.legacy.publish.domain.ManualPublishLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ManualPublishLogMapper {

    ManualPublishLog findLatestByResearchNo(@Param("researchNo") Long researchNo);

    void insert(ManualPublishLog manualPublishLog);
}
