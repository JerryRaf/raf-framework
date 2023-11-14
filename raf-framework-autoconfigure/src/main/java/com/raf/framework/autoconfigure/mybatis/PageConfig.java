package com.raf.framework.autoconfigure.mybatis;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import com.raf.framework.autoconfigure.spring.condition.ConditionalOnMapProperty;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Configuration
@ConditionalOnMapProperty(prefix = "raf.datasource")
@ConditionalOnClass({PageHelper.class, SqlSessionFactory.class})
@EnableConfigurationProperties(PageProperties.class)
public class PageConfig {

  private final List<SqlSessionFactory> sqlSessionFactoryList;

  private final PageProperties pageProperties;

  public PageConfig(ObjectProvider<List<SqlSessionFactory>> sqlSessionFactoryList,
                    PageProperties pageProperties) {
    this.sqlSessionFactoryList = sqlSessionFactoryList.getIfAvailable();
    this.pageProperties = pageProperties;
  }

  @PostConstruct
  public void addPageInterceptor() {
    PageInterceptor interceptor = new PageInterceptor();
    Properties properties = pageProperties.getProperties();
    interceptor.setProperties(properties);
    Optional.ofNullable(sqlSessionFactoryList).ifPresent(list -> list.forEach(
        sqlSessionFactory -> sqlSessionFactory.getConfiguration().addInterceptor(interceptor)));
  }
}
