package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminApplicationDuplicateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminApplicationDuplicateLogMapper {

    AdminApplicationDuplicateLog findLatestByDocumentSrlAndMobilePhoneHash(
            @Param("documentSrl") Long documentSrl,
            @Param("mobilePhoneHash") String mobilePhoneHash
    );

    int countPrimaryByMobilePhoneHash(@Param("mobilePhoneHash") String mobilePhoneHash);

    void insert(AdminApplicationDuplicateLog duplicateLog);
}
