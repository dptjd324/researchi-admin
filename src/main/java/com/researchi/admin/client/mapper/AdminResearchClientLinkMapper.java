package com.researchi.admin.client.mapper;

import com.researchi.admin.client.domain.AdminResearchClientLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminResearchClientLinkMapper {

    AdminResearchClientLink findByResearchNo(@Param("researchNo") Long researchNo);

    List<AdminResearchClientLink> findByClientId(@Param("clientId") Long clientId);

    void upsert(AdminResearchClientLink link);

    int deleteByResearchNo(@Param("researchNo") Long researchNo);

    int deleteByClientId(@Param("clientId") Long clientId);
}
