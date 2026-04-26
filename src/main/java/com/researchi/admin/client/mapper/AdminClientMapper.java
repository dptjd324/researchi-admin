package com.researchi.admin.client.mapper;

import com.researchi.admin.client.domain.AdminClient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminClientMapper {

    List<AdminClient> findAllActive();

    List<AdminClient> findAll();

    AdminClient findById(@Param("id") Long id);

    void insert(AdminClient client);

    int update(AdminClient client);

    int deleteById(@Param("id") Long id);
}
