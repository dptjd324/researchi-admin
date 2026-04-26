package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminPrivacyConsent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminPrivacyConsentMapper {

    void insert(AdminPrivacyConsent consent);
}
