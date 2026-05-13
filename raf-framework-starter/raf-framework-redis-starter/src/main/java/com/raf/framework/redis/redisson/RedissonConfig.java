package com.raf.framework.redis.redisson;

import java.util.Arrays;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raf.framework.redis.aspect.DistributedLockAspect;
import com.raf.framework.redis.aspect.IdempotentConsumerAspect;
import com.raf.framework.redis.aspect.MultiLevelCacheAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.*;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson
 *
 * @author Jerry
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.redisson.enabled", havingValue = "true")
@ConditionalOnClass({Redisson.class})
@AutoConfigureAfter({RedisAutoConfiguration.class, JacksonAutoConfiguration.class})
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {

    // 云环境最佳实践默认值
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_RETRY_INTERVAL = 2000;
    private static final int DEFAULT_PING_INTERVAL = 30000;
    private static final int DEFAULT_SUBSCRIPTIONS = 5;
    private static final int DEFAULT_SCAN_INTERVAL = 5000;
    private static final int DEFAULT_POOL_MIN = 24;
    private static final int DEFAULT_POOL_MAX = 64;

    private final RedissonProperties properties;
    private final ObjectMapper objectMapper;

    public RedissonConfig(RedissonProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean({RedissonClient.class})
    public RedissonClient redisson() {
        Config config = new Config();
        // ✅ 复用 Spring 管理的 ObjectMapper（已配置好 JSR310 等模块）
        config.setCodec(new JsonJacksonCodec(objectMapper));
        config.setTransportMode(TransportMode.NIO);

        if (properties.getThreads() != null) {
            config.setThreads(properties.getThreads());
        }
        if (properties.getNettyThreads() != null) {
            config.setNettyThreads(properties.getNettyThreads());
        }

        if (properties.getCluster() != null && StringUtils.isNotBlank(properties.getCluster().getNodes())) {
            log.info("Redisson initializing in CLUSTER mode.");
            initClusterConfig(config);
        } else {
            log.info("Redisson initializing in SINGLE mode.");
            initSingleConfig(config);
        }

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            log.error("Failed to initialize Redisson client", e);
            throw new RuntimeException("Failed to initialize Redisson client", e);
        }
    }

    private void initClusterConfig(Config config) {
        RedissonProperties.RedissonPropertiesCluster clusterProp = properties.getCluster();

        String[] nodes = Arrays.stream(StringUtils.split(clusterProp.getNodes(), ",")).map(this::formatAddress)
                .toArray(String[]::new);

        ClusterServersConfig serverConfig = config.useClusterServers().addNodeAddress(nodes);

        if (StringUtils.isNotBlank(clusterProp.getPassword())) {
            serverConfig.setPassword(clusterProp.getPassword());
        }

        // 集群特定配置
        serverConfig.setCheckSlotsCoverage(false);
        serverConfig.setScanInterval(ObjectUtils.defaultIfNull(clusterProp.getScanInterval(), DEFAULT_SCAN_INTERVAL));
        serverConfig.setMasterConnectionPoolSize(ObjectUtils.defaultIfNull(clusterProp.getMasterConnectionPoolMaxSize(), DEFAULT_POOL_MAX));
        serverConfig.setMasterConnectionMinimumIdleSize(ObjectUtils.defaultIfNull(clusterProp.getMasterConnectionPoolMinSize(), DEFAULT_POOL_MIN));

        // 判断是否需要 SSL
        boolean isSsl = Boolean.TRUE.equals(clusterProp.getSsl()) || (nodes.length > 0 && nodes[0].startsWith("rediss://"));

        // 应用通用配置 (含SSL逻辑)
        applyCommonConfig(serverConfig, isSsl);
    }

    private void initSingleConfig(Config config) {
        RedissonProperties.RedissonPropertiesSingle singleProp = properties.getSingle();
        if (singleProp == null || StringUtils.isBlank(singleProp.getHost())) {
            throw new IllegalStateException(
                "[raf-framework] raf.redisson.single.host must be configured when running in single mode. "
                + "Please check your application configuration.");
        }
        if (singleProp.getPort() == null) {
            throw new IllegalStateException(
                "[raf-framework] raf.redisson.single.port must be configured when running in single mode.");
        }

        String address = formatAddress(singleProp.getHost() + ":" + singleProp.getPort());
        SingleServerConfig serverConfig = config.useSingleServer().setAddress(address);

        if (StringUtils.isNotBlank(singleProp.getPassword())) {
            serverConfig.setPassword(singleProp.getPassword());
        }
        if (singleProp.getDatabase() != null) {
            serverConfig.setDatabase(singleProp.getDatabase());
        }

        boolean isSsl = Boolean.TRUE.equals(singleProp.getSsl()) || address.startsWith("rediss://");
        applyCommonConfig(serverConfig, isSsl);
    }

    /**
     * 应用通用配置
     * 修复点：参数类型改为 BaseConfig<?>，这是 Single 和 Cluster 配置的共同父类
     */
    private void applyCommonConfig(BaseConfig<?> serverConfig, boolean isSsl) {
        serverConfig.setTimeout(ObjectUtils.defaultIfNull(properties.getTimeout(), DEFAULT_TIMEOUT));
        serverConfig.setConnectTimeout(ObjectUtils.defaultIfNull(properties.getConnectTimeout(), DEFAULT_CONNECT_TIMEOUT));
        serverConfig.setRetryAttempts(ObjectUtils.defaultIfNull(properties.getRetryAttempts(), DEFAULT_RETRY_ATTEMPTS));
        serverConfig.setRetryInterval(ObjectUtils.defaultIfNull(properties.getRetryInterval(), DEFAULT_RETRY_INTERVAL));
        serverConfig.setPingConnectionInterval(ObjectUtils.defaultIfNull(properties.getPingInterval(), DEFAULT_PING_INTERVAL));
        serverConfig.setSubscriptionsPerConnection(ObjectUtils.defaultIfNull(properties.getSubscriptionsPerConnection(), DEFAULT_SUBSCRIPTIONS));

        if (StringUtils.isNotBlank(properties.getClientName())) {
            serverConfig.setClientName(properties.getClientName());
        }

        // 统一处理 SSL / OpenSSL 逻辑
        if (isSsl) {
            log.info("Redisson SSL enabled. sslEndpointIdentification={}",
                     properties.isSslEndpointIdentification());
            serverConfig.setSslEnableEndpointIdentification(properties.isSslEndpointIdentification());
            serverConfig.setSslProvider(SslProvider.OPENSSL);
        }
    }

    private String formatAddress(String node) {
        if (StringUtils.isBlank(node)) {
            return "";
        }
        node = node.trim();
        if (node.startsWith("redis://") || node.startsWith("rediss://")) {
            return node;
        }
        return "redis://" + node;
    }

    @Bean
    public RedissonService redissonHelper(RedissonClient redissonClient) {
        return new RedissonService(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(RedisJsonHelper.class)
    public RedisJsonHelper redisJsonHelper(ObjectMapper objectMapper) {
        return new RedisJsonHelper(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(MultiLevelCacheService.class)
    public MultiLevelCacheService multiLevelCacheService(RedissonService redissonService,
                                                         RedisJsonHelper redisJsonHelper) {
        log.info("Initializing MultiLevelCacheService with RedissonService");
        return new MultiLevelCacheService(redissonService, redisJsonHelper);
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLockAspect.class)
    public DistributedLockAspect distributedLockAspect(RedissonService redissonService) {
        return new DistributedLockAspect(redissonService);
    }

    @Bean
    @ConditionalOnMissingBean(MultiLevelCacheAspect.class)
    public MultiLevelCacheAspect multiLevelCacheAspect(MultiLevelCacheService multiLevelCacheService) {
        return new MultiLevelCacheAspect(multiLevelCacheService);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotentConsumerAspect.class)
    public IdempotentConsumerAspect idempotentConsumerAspect(RedissonService redissonService) {
        return new IdempotentConsumerAspect(redissonService);
    }
}