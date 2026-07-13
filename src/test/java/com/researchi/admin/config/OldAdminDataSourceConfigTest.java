package com.researchi.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class OldAdminDataSourceConfigTest {

    @Test
    void exposesDedicatedOldAdminTransactionManagerBean() throws Exception {
        Bean bean = OldAdminDataSourceConfig.class
                .getMethod("oldAdminTransactionManager", DataSource.class)
                .getAnnotation(Bean.class);

        assertThat(bean).isNotNull();
        assertThat(bean.name()).contains("oldAdminTransactionManager");
    }
}
