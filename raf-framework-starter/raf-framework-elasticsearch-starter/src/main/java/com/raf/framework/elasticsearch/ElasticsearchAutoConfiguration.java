package com.raf.framework.elasticsearch;

import java.util.ArrayList;
import java.util.List;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch 自动配置
 *
 * @author RAF Framework
 */
@Slf4j
@Configuration
@ConditionalOnClass(ElasticsearchClient.class)
@ConditionalOnProperty(prefix = "raf.elasticsearch", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchAutoConfiguration {

    private final ElasticsearchProperties properties;

    public ElasticsearchAutoConfiguration(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 RestClient
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient() {
        if (properties.getHosts() == null || properties.getHosts().isEmpty()) {
            throw new IllegalArgumentException("Elasticsearch hosts must not be empty");
        }

        // 解析 hosts
        List<HttpHost> httpHosts = new ArrayList<>();
        for (String host : properties.getHosts()) {
            String[] parts = host.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid host format: " + host + ", expected format: host:port");
            }
            String hostname = parts[0];
            int port = Integer.parseInt(parts[1]);
            httpHosts.add(new HttpHost(hostname, port, "http"));
        }

        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]));

        // 配置超时时间
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout((int) properties.getConnectTimeout().toMillis())
                        .setSocketTimeout((int) properties.getSocketTimeout().toMillis())
                        .setConnectionRequestTimeout((int) properties.getConnectionRequestTimeout().toMillis())
        );

        // 配置连接池
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder
                    .setMaxConnTotal(properties.getMaxConnections())
                    .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute());

            // 配置认证
            if (StringUtils.hasText(properties.getUsername()) && StringUtils.hasText(properties.getPassword())) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
                );
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            return httpClientBuilder;
        });

        RestClient client = builder.build();
        log.info("Elasticsearch RestClient initialized with hosts: {}", properties.getHosts());
        return client;
    }

    /**
     * 配置 ElasticsearchClient (新版 Java API Client)
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(RestClient restClient, ObjectMapper objectMapper) {
        RestClientTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper(objectMapper)
        );
        ElasticsearchClient client = new ElasticsearchClient(transport);
        log.info("ElasticsearchClient initialized");
        return client;
    }

    /**
     * 配置 ElasticsearchTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient client, ObjectMapper objectMapper) {
        log.info("ElasticsearchTemplate initialized");
        return new ElasticsearchTemplate(client, objectMapper, properties);
    }
}
