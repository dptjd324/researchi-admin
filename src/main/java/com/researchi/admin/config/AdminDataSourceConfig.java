package com.researchi.admin.config;

import javax.sql.DataSource;
import java.io.FileNotFoundException;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@MapperScan(
        basePackages = {
                "com.researchi.admin.auth.mapper",
                "com.researchi.admin.client.mapper",
                "com.researchi.admin.export.mapper",
                "com.researchi.admin.log.mapper",
                "com.researchi.admin.legacy.application.mapper",
                "com.researchi.admin.legacy.mail.mapper",
                "com.researchi.admin.legacy.matching.mapper",
                "com.researchi.admin.legacy.publish.mapper",
                "com.researchi.admin.legacy.revision.mapper",
                "com.researchi.admin.mailing.mapper",
                "com.researchi.admin.notification.mapper",
                "com.researchi.admin.search.mapper"
        },
        sqlSessionTemplateRef = "adminSqlSessionTemplate"
)
public class AdminDataSourceConfig {

    @Bean(name = "adminDataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.admin")
    public DataSource adminDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "adminSqlSessionFactory")
    @Primary
    public SqlSessionFactory adminSqlSessionFactory(
            @Qualifier("adminDataSource") DataSource adminDataSource,
            ApplicationContext applicationContext
    ) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(adminDataSource);
        try {
            factoryBean.setMapperLocations(applicationContext.getResources("classpath*:/mapper/admin/**/*.xml"));
        } catch (FileNotFoundException ignored) {
            factoryBean.setMapperLocations(new org.springframework.core.io.Resource[0]);
        }
        return factoryBean.getObject();
    }

    @Bean(name = "adminSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate adminSqlSessionTemplate(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory adminSqlSessionFactory
    ) {
        return new SqlSessionTemplate(adminSqlSessionFactory);
    }

    @Bean(name = "adminTransactionManager")
    @Primary
    public PlatformTransactionManager adminTransactionManager(
            @Qualifier("adminDataSource") DataSource adminDataSource
    ) {
        return new DataSourceTransactionManager(adminDataSource);
    }
}
