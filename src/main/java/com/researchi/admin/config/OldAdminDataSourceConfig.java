package com.researchi.admin.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.FileNotFoundException;

@Configuration
@ConditionalOnProperty(name = "spring.datasource.old-admin.jdbc-url")
@MapperScan(
        basePackages = {
                "com.researchi.admin.legacy.research.mapper",
                "com.researchi.admin.legacy.blacklist.mapper"
        },
        sqlSessionTemplateRef = "oldAdminSqlSessionTemplate"
)
public class OldAdminDataSourceConfig {

    @Bean(name = "oldAdminDataSource")
    @ConfigurationProperties("spring.datasource.old-admin")
    public DataSource oldAdminDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "oldAdminSqlSessionFactory")
    public SqlSessionFactory oldAdminSqlSessionFactory(
            @Qualifier("oldAdminDataSource") DataSource oldAdminDataSource,
            ApplicationContext applicationContext
    ) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(oldAdminDataSource);
        try {
            factoryBean.setMapperLocations(applicationContext.getResources("classpath*:/mapper/oldadmin/**/*.xml"));
        } catch (FileNotFoundException ignored) {
            factoryBean.setMapperLocations(new org.springframework.core.io.Resource[0]);
        }
        return factoryBean.getObject();
    }

    @Bean(name = "oldAdminSqlSessionTemplate")
    public SqlSessionTemplate oldAdminSqlSessionTemplate(
            @Qualifier("oldAdminSqlSessionFactory") SqlSessionFactory oldAdminSqlSessionFactory
    ) {
        return new SqlSessionTemplate(oldAdminSqlSessionFactory);
    }

    @Bean(name = "oldAdminTransactionManager")
    public PlatformTransactionManager oldAdminTransactionManager(
            @Qualifier("oldAdminDataSource") DataSource oldAdminDataSource
    ) {
        return new DataSourceTransactionManager(oldAdminDataSource);
    }
}
