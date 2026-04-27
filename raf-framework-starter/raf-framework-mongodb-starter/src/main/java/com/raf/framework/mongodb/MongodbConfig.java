package com.raf.framework.mongodb;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.util.StringUtils;

/**
 * MongoDB 自动配置
 *
 * @author Jerry
 * @since 3.0.1
 */
@Slf4j
@Configuration
@ConditionalOnClass(MongoClient.class)
@ConditionalOnProperty(prefix = "raf.mongodb", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MongodbProperties.class)
public class MongodbConfig {

    private final MongodbProperties properties;

    public MongodbConfig(MongodbProperties properties) {
        this.properties = properties;
        log.info("MongoDB auto-configuration enabled");
    }

    /**
     * 创建主 MongoClient
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "mongoClient")
    public MongoClient mongoClient() {
        if (!StringUtils.hasText(properties.getUri())) {
            throw new IllegalArgumentException("MongoDB URI must not be empty. Please configure 'raf.mongodb.uri'");
        }

        log.info("Creating primary MongoDB client with URI: {}", maskPassword(properties.getUri()));
        return createMongoClient(properties.getUri(), properties.getPool());
    }

    /**
     * 创建主 MongoDatabaseFactory
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "mongoDatabaseFactory")
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        String database = extractDatabase(properties.getUri(), properties.getDatabase());
        log.info("Creating primary MongoDatabaseFactory for database: {}", database);
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    /**
     * 创建主 MongoTemplate
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "mongoTemplate")
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        log.info("Creating primary MongoTemplate");
        return new MongoTemplate(mongoDatabaseFactory);
    }

    /**
     * 创建多数据源 MongoTemplate Map
     */
    @Bean
    @ConditionalOnMissingBean(name = "mongoTemplateMap")
    public Map<String, MongoTemplate> mongoTemplateMap() {
        Map<String, MongoTemplate> templateMap = new HashMap<>();

        // 添加主数据源
        templateMap.put("primary", mongoTemplate(mongoDatabaseFactory(mongoClient())));

        // 添加其他数据源
        if (properties.getDataSources() != null && !properties.getDataSources().isEmpty()) {
            properties.getDataSources().forEach((name, config) -> {
                if (!StringUtils.hasText(config.getUri())) {
                    log.warn("Data source '{}' has no URI configured, skipping", name);
                    return;
                }

                log.info("Creating MongoDB data source: {}", name);

                // 使用数据源自己的连接池配置，如果没有则使用全局配置
                MongodbProperties.PoolConfig poolConfig = config.getPool() != null
                        ? config.getPool()
                        : properties.getPool();

                MongoClient client = createMongoClient(config.getUri(), poolConfig);
                String database = extractDatabase(config.getUri(), config.getDatabase());
                MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(client, database);
                MongoTemplate template = new MongoTemplate(factory);

                templateMap.put(name, template);
            });
        }

        log.info("MongoDB data sources initialized: {}", templateMap.keySet());
        return templateMap;
    }

    /**
     * 创建 MongoClient
     */
    private MongoClient createMongoClient(String uri, MongodbProperties.PoolConfig poolConfig) {
        ConnectionString connectionString = new ConnectionString(uri);

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(connectionString);

        // 配置连接池
        settingsBuilder.applyToConnectionPoolSettings(builder -> {
            builder.maxSize(poolConfig.getMaxSize())
                    .minSize(poolConfig.getMinSize())
                    .maxWaitTime(poolConfig.getMaxWaitTime(), TimeUnit.MILLISECONDS);

            if (poolConfig.getMaxConnectionIdleTime() > 0) {
                builder.maxConnectionIdleTime(poolConfig.getMaxConnectionIdleTime(), TimeUnit.MILLISECONDS);
            }

            if (poolConfig.getMaxConnectionLifeTime() > 0) {
                builder.maxConnectionLifeTime(poolConfig.getMaxConnectionLifeTime(), TimeUnit.MILLISECONDS);
            }
        });

        // 配置 Socket 设置
        settingsBuilder.applyToSocketSettings(builder -> {
            builder.connectTimeout(poolConfig.getConnectTimeout(), TimeUnit.MILLISECONDS);

            if (poolConfig.getSocketTimeout() > 0) {
                builder.readTimeout(poolConfig.getSocketTimeout(), TimeUnit.MILLISECONDS);
            }
        });

        return MongoClients.create(settingsBuilder.build());
    }

    /**
     * 从 URI 或配置中提取数据库名称
     */
    private String extractDatabase(String uri, String configDatabase) {
        ConnectionString connectionString = new ConnectionString(uri);
        String database = connectionString.getDatabase();

        if (!StringUtils.hasText(database)) {
            database = configDatabase;
        }

        if (!StringUtils.hasText(database)) {
            throw new IllegalArgumentException("Database name must be specified in URI or configuration");
        }

        return database;
    }

    /**
     * 隐藏 URI 中的密码
     */
    private String maskPassword(String uri) {
        if (uri.contains("@")) {
            return uri.replaceAll("://[^:]+:[^@]+@", "://***:***@");
        }
        return uri;
    }
}
