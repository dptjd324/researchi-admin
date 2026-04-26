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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@MapperScan(
        basePackages = "com.researchi.admin.xe.mapper",
        sqlSessionTemplateRef = "xeSqlSessionTemplate"
)
public class XeDataSourceConfig {

    @Bean(name = "xeDataSource")
    @ConfigurationProperties("spring.datasource.xe")
    public DataSource xeDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "xeSqlSessionFactory")
    public SqlSessionFactory xeSqlSessionFactory(
            @Qualifier("xeDataSource") DataSource xeDataSource,
            ApplicationContext applicationContext
    ) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(xeDataSource);
        try {
            factoryBean.setMapperLocations(applicationContext.getResources("classpath*:/mapper/xe/**/*.xml"));
        } catch (FileNotFoundException ignored) {
            factoryBean.setMapperLocations(new org.springframework.core.io.Resource[0]);
        }
        return factoryBean.getObject();
    }

    @Bean(name = "xeSqlSessionTemplate")
    public SqlSessionTemplate xeSqlSessionTemplate(
            @Qualifier("xeSqlSessionFactory") SqlSessionFactory xeSqlSessionFactory
    ) {
        return new SqlSessionTemplate(xeSqlSessionFactory);
    }

    @Bean(name = "xeTransactionManager")
    public PlatformTransactionManager xeTransactionManager(
            @Qualifier("xeDataSource") DataSource xeDataSource
    ) {
        return new DataSourceTransactionManager(xeDataSource);
    }
}
