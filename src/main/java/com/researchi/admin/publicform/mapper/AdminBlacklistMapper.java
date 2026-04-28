package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminBlacklistMapper {

    List<AdminBlacklist> findActiveMatches(
            @Param("blackName") String blackName,
            @Param("blackBirthDate") LocalDate blackBirthDate,
            @Param("blackMobilePhoneHashes") List<String> blackMobilePhoneHashes
    );
}
