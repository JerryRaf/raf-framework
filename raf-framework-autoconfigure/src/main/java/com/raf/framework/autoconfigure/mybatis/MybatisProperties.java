package com.raf.framework.autoconfigure.mybatis;

import lombok.Data;
import org.apache.ibatis.session.ExecutorType;

import java.util.Map;
import java.util.Properties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
public class MybatisProperties {

  private Map<String, MybatisDataSourceProperties> dataSource;

  @Data
  public static class MybatisDataSourceProperties{
    private String configLocation;

    private String[] mapperLocations;

    private String typeAliasesPackage;

    private String typeHandlersPackage;

    private String basePackage;

    private ExecutorType executorType;

    private Properties configurationProperties;
  }
}
