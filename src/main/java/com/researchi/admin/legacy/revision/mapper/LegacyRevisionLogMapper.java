package com.researchi.admin.legacy.revision.mapper;

import com.researchi.admin.legacy.revision.domain.LegacyRevisionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegacyRevisionLogMapper {

    void insert(LegacyRevisionLog revisionLog);
}
