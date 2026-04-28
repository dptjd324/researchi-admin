package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminApplicationDuplicateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminApplicationDuplicateLogMapper {

    AdminApplicationDuplicateLog findLatestByDocumentSrlAndMobilePhoneHashes(
            @Param("documentSrl") Long documentSrl,
            @Param("mobilePhoneHashes") List<String> mobilePhoneHashes
    );

    int countPrimaryByMobilePhoneHashes(@Param("mobilePhoneHashes") List<String> mobilePhoneHashes);

    void insert(AdminApplicationDuplicateLog duplicateLog);
}
