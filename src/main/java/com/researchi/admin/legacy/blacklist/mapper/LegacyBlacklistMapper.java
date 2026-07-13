package com.researchi.admin.legacy.blacklist.mapper;

import com.researchi.admin.legacy.blacklist.domain.Blacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LegacyBlacklistMapper {

    Blacklist findByBlacklistNo(@Param("blacklistNo") Long blacklistNo);

    List<Blacklist> findPage(
            @Param("keyword") String keyword,
            @Param("birth") String birth,
            @Param("name") String name,
            @Param("blackYn") String blackYn,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    int count(
            @Param("keyword") String keyword,
            @Param("birth") String birth,
            @Param("name") String name,
            @Param("blackYn") String blackYn
    );

    Long findNextBlacklistNo();

    void insert(Blacklist blacklist);

    int update(Blacklist blacklist);

    int updateBlackYn(
            @Param("blacklistNo") Long blacklistNo,
            @Param("blackYn") String blackYn
    );

    int deleteByBlacklistNo(@Param("blacklistNo") Long blacklistNo);

    int countActiveMatch(
            @Param("name") String name,
            @Param("birth") String birth,
            @Param("contact") String contact
    );
}
