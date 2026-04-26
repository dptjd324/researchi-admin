package com.researchi.admin.client.mapper;

import com.researchi.admin.client.domain.AdminClientContact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminClientContactMapper {

    List<AdminClientContact> findByClientId(@Param("clientId") Long clientId);

    void insert(AdminClientContact contact);

    int deleteByClientId(@Param("clientId") Long clientId);

    int deleteById(@Param("id") Long id);
}
