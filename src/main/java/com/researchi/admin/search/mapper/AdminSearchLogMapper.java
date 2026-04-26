package com.researchi.admin.search.mapper;

import com.researchi.admin.search.domain.AdminSearchLog;
import com.researchi.admin.search.domain.SearchLogItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminSearchLogMapper {

    void insert(AdminSearchLog searchLog);

    List<SearchLogItem> findAll();
}
