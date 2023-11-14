package com.raf.framework.autoconfigure.redis;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.TransportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * redisson装配
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.redisson.enabled")
@ConditionalOnClass({Redisson.class})
@AutoConfigureAfter({RedisAutoConfiguration.class})
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {
    @Autowired
    private RedissonProperties redissonProperties;

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean({RedissonClient.class})
    public RedissonClient redisson() {
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec());
        config.setTransportMode(TransportMode.NIO);
        Optional.of(redissonProperties.getThreads())
                .ifPresent(config::setThreads);
        Optional.of(redissonProperties.getNettyThreads())
                .ifPresent(config::setNettyThreads);

        if (redissonProperties.getCluster() != null) {
            log.info("redission cluster init start.");
            //集群模式配置
            List<String> nodes = Arrays.stream(StringUtils.split(redissonProperties.getCluster().getNodes(),",")).collect(Collectors.toList());

            List<String> clusterNodes = new ArrayList<>();
            for (String node : nodes) {
                clusterNodes.add("redis://" + node);
            }
            ClusterServersConfig clusterServersConfig = config.useClusterServers()
                    .addNodeAddress(clusterNodes.toArray(new String[clusterNodes.size()]));

            clusterServersConfig.setCheckSlotsCoverage(false);
            RedissonProperties.RedissonPropertiesCluster cluster = redissonProperties.getCluster();
            //修复redisson密码未设置缺陷
            if (StringUtils.isNotEmpty(cluster.getPassword())) {
                clusterServersConfig.setPassword(cluster.getPassword());
            }

            Optional.ofNullable(cluster.getIdleConnectionTimeout())
                    .ifPresent(clusterServersConfig::setIdleConnectionTimeout);
            Optional.ofNullable(cluster.getConnectTimeout())
                    .ifPresent(clusterServersConfig::setConnectTimeout);
            Optional.ofNullable(cluster.getTimeout())
                    .ifPresent(clusterServersConfig::setTimeout);

            Optional.ofNullable(cluster.getRetryAttempts())
                    .ifPresent(clusterServersConfig::setRetryAttempts);
            Optional.ofNullable(cluster.getRetryInterval())
                    .ifPresent(clusterServersConfig::setRetryInterval);

            Optional.ofNullable(cluster.getSubscriptionsPerConnection())
                    .ifPresent(clusterServersConfig::setSubscriptionsPerConnection);
            Optional.ofNullable(cluster.getClientName())
                    .ifPresent(clusterServersConfig::setClientName);
        }else{
            log.info("redission single init start.");
            RedissonProperties.RedissonPropertiesSingle single = redissonProperties.getSingle();
            String address = "redis://".concat(single.getHost()).concat(":").concat(single.getPort());
            SingleServerConfig singleServerConfig = config.useSingleServer().setAddress(address);
            //修复redisson密码未设置缺陷
            if (StringUtils.isNotEmpty(single.getPassword())) {
                singleServerConfig.setPassword(single.getPassword());
            }
            Optional.ofNullable(single.getDatabase())
                    .ifPresent(singleServerConfig::setDatabase);

            Optional.ofNullable(single.getIdleConnectionTimeout())
                    .ifPresent(singleServerConfig::setIdleConnectionTimeout);
            Optional.ofNullable(single.getConnectTimeout())
                    .ifPresent(singleServerConfig::setConnectTimeout);
            Optional.ofNullable(single.getTimeout())
                    .ifPresent(singleServerConfig::setTimeout);

            Optional.ofNullable(single.getRetryAttempts())
                    .ifPresent(singleServerConfig::setRetryAttempts);
            Optional.ofNullable(single.getRetryInterval())
                    .ifPresent(singleServerConfig::setRetryInterval);

            Optional.ofNullable(single.getSubscriptionsPerConnection())
                    .ifPresent(singleServerConfig::setSubscriptionsPerConnection);
            Optional.ofNullable(single.getClientName())
                    .ifPresent(singleServerConfig::setClientName);
        }

        return Redisson.create(config);
    }
}