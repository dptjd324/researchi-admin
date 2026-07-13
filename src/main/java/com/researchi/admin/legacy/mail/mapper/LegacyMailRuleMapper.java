package com.researchi.admin.legacy.mail.mapper;

import com.researchi.admin.legacy.mail.domain.LegacyMailRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LegacyMailRuleMapper {

    LegacyMailRule findByResearchNo(@Param("researchNo") Long researchNo);

    LegacyMailRule findRuleItemById(@Param("id") Long id);

    List<LegacyMailRule> findEnabled();

    List<LegacyMailRule> findEnabledRuleItems();

    List<LegacyMailRule> findRuleItemsByResearchNo(@Param("researchNo") Long researchNo);

    void upsert(LegacyMailRule rule);

    void insertRuleItem(LegacyMailRule rule);

    int deleteRuleItem(@Param("id") Long id, @Param("researchNo") Long researchNo);

    int disableByResearchNo(@Param("researchNo") Long researchNo);

    int completeByResearchNo(
            @Param("researchNo") Long researchNo,
            @Param("lastTriggeredAt") LocalDateTime lastTriggeredAt
    );

    int completeRuleItemById(
            @Param("id") Long id,
            @Param("lastTriggeredAt") LocalDateTime lastTriggeredAt
    );

    int touchLastTriggeredAt(
            @Param("researchNo") Long researchNo,
            @Param("lastTriggeredAt") LocalDateTime lastTriggeredAt
    );

    int touchRuleItemLastTriggeredAt(
            @Param("id") Long id,
            @Param("lastTriggeredAt") LocalDateTime lastTriggeredAt
    );
}
