package com.researchi.admin.search.mapper;

import com.researchi.admin.search.domain.AdminSearchLog;
import com.researchi.admin.search.domain.SearchLogItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminSearchLogMapper {

    void insert(AdminSearchLog searchLog);

    List<SearchLogItem> findAll();

    List<SearchLogItem> findPage(@Param("limit") int limit, @Param("offset") int offset);

    long countAll();

    LocalDateTime findLatestSearchedAt();
}
