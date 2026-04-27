package com.raf.framework.kafka;

import com.raf.framework.core.common.RafConstant;
import com.raf.framework.core.jackson.JsonService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

/**
 * Kafka auto-configuration class
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.kafka.enabled", havingValue = "true")
@ConditionalOnClass(org.apache.kafka.clients.producer.Producer.class)
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig implements BeanFactoryPostProcessor, EnvironmentAware, ApplicationContextAware {

    private ConfigurableListableBeanFactory beanFactory;
    private ApplicationContext applicationContext;
    private ConfigurableEnvironment environment;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    /**
     * Create Kafka producer factory
     */
    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public ProducerFactory<String, String> producerFactory(KafkaProperties properties) {
        Map<String, Object> props = buildProducerConfigs(properties);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Create transactional Kafka producer factory
     */
    @Bean
    @ConditionalOnMissingBean(name = "transactionalProducerFactory")
    @ConditionalOnProperty(prefix = "raf.kafka.producer", name = "transactionalIdPrefix")
    public ProducerFactory<String, String> transactionalProducerFactory(KafkaProperties properties) {
        Map<String, Object> props = buildProducerConfigs(properties);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,
                properties.getProducer().getTransactionalIdPrefix() + "-" + UUID.randomUUID().toString());

        DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(props);
        factory.setTransactionIdPrefix(properties.getProducer().getTransactionalIdPrefix());

        log.info("Kafka transactional producer factory created. prefix:{}",
                properties.getProducer().getTransactionalIdPrefix());

        return factory;
    }

    /**
     * Create Kafka producer wrapper
     */
    @Bean
    @ConditionalOnMissingBean(KafkaProducer.class)
    public KafkaProducer kafkaProducer(ProducerFactory<String, String> producerFactory,
                                       KafkaProperties properties,
                                       JsonService jsonService) {
        org.apache.kafka.clients.producer.Producer<String, String> producer = producerFactory.createProducer();

        org.apache.kafka.clients.producer.Producer<String, String> transactionalProducer = null;
        boolean enableTransaction = false;

        try {
            ProducerFactory<String, String> transactionalFactory =
                    applicationContext.getBean("transactionalProducerFactory", ProducerFactory.class);
            transactionalProducer = transactionalFactory.createProducer();
            enableTransaction = true;
            log.info("Kafka transactional producer initialized");
        } catch (Exception e) {
            log.info("Kafka transactional producer not configured");
        }

        log.info("Kafka producer initialized. bootstrapServers:{}", properties.getBootstrapServers());
        return new KafkaProducer(producer, transactionalProducer, jsonService, enableTransaction);
    }

    /**
     * Create Kafka consumer factory
     */
    @Bean
    @ConditionalOnMissingBean(ConsumerFactory.class)
    public ConsumerFactory<String, String> consumerFactory(KafkaProperties properties) {
        Map<String, Object> props = buildConsumerConfigs(properties);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Create Kafka listener container factory
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaProperties properties) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        KafkaProperties.Consumer consumerConfig = properties.getConsumer();
        if (consumerConfig != null) {
            factory.setConcurrency(consumerConfig.getConcurrency());
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
            factory.getContainerProperties().setPollTimeout(consumerConfig.getPollTimeout());
        }

        log.info("Kafka listener container factory initialized");
        return factory;
    }

    /**
     * Register Kafka consumers
     */
    @Bean
    @ConditionalOnProperty(prefix = "raf.kafka", name = "bootstrapServers")
    public String registerKafkaConsumers(KafkaProperties properties,
                                         ConsumerFactory<String, String> consumerFactory) {
        String[] consumerBeans = applicationContext.getBeanNamesForAnnotation(KafkaConsumer.class);

        if (consumerBeans.length == 0) {
            log.warn("No @KafkaConsumer annotation found");
            return "noConsumer";
        }

        Arrays.stream(consumerBeans)
                .forEach(beanName -> registerKafkaConsumer(beanName, properties, consumerFactory));

        log.info("Kafka consumers registered successfully, total: {}", consumerBeans.length);
        return "success";
    }

    /**
     * Register single Kafka consumer
     */
    private void registerKafkaConsumer(String beanName,
                                       KafkaProperties properties,
                                       ConsumerFactory<String, String> consumerFactory) {
        try {
            AbstractKafkaConsumerListener<?> listener =
                    applicationContext.getBean(beanName, AbstractKafkaConsumerListener.class);

            Class<?> clazz = listener.getClass();
            if (AopUtils.isAopProxy(listener)) {
                clazz = AopUtils.getTargetClass(listener);
            }

            KafkaConsumer annotation = clazz.getAnnotation(KafkaConsumer.class);
            if (annotation == null) {
                return;
            }

            String groupId = parseProperty(annotation.groupId());
            String[] topics = Arrays.stream(annotation.topics())
                    .map(this::parseProperty)
                    .toArray(String[]::new);

            ConcurrentKafkaListenerContainerFactory<String, String> containerFactory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            containerFactory.setConsumerFactory(consumerFactory);
            containerFactory.setConcurrency(annotation.concurrency());
            containerFactory.setAutoStartup(annotation.autoStartup());

            ContainerProperties containerProperties = containerFactory.getContainerProperties();
            containerProperties.setAckMode(ContainerProperties.AckMode.valueOf(annotation.ackMode()));
            containerProperties.setPollTimeout(annotation.pollTimeout());

            if (annotation.containerType() == KafkaConsumer.ContainerType.SINGLE) {
                containerProperties.setMessageListener(
                        (org.springframework.kafka.listener.MessageListener<String, String>) listener::onMessage
                );
            }

            org.springframework.kafka.listener.ConcurrentMessageListenerContainer<String, String> container =
                    containerFactory.createContainer(topics);
            container.start();

            String containerBeanName = groupId + "_" + String.join("_", topics) + "_Container";
            beanFactory.registerSingleton(containerBeanName, container);

            log.info("Kafka consumer registered successfully. groupId:{}, topics:{}", groupId, String.join(",", topics));

        } catch (Exception e) {
            log.error("Kafka consumer registration failed: {}", beanName, e);
            throw new RuntimeException("Kafka consumer registration failed", e);
        }
    }

    /**
     * Build producer configurations
     */
    private Map<String, Object> buildProducerConfigs(KafkaProperties properties) {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, properties.getProducer().getKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, properties.getProducer().getValueSerializer());

        KafkaProperties.Producer producerConfig = properties.getProducer();
        props.put(ProducerConfig.RETRIES_CONFIG, producerConfig.getRetries());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerConfig.getRequestTimeout());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, producerConfig.getBatchSize());
        props.put(ProducerConfig.LINGER_MS_CONFIG, producerConfig.getLingerMs());
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, producerConfig.getBufferMemory());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, producerConfig.getCompressionType());
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, producerConfig.getMaxInFlightRequestsPerConnection());
        props.put(ProducerConfig.ACKS_CONFIG, producerConfig.getAcks());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producerConfig.isEnableIdempotence());

        if (producerConfig.getTransactionalIdPrefix() != null) {
            props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, producerConfig.getTransactionTimeout());
        }

        applySecurityConfig(props, properties);

        return props;
    }

    /**
     * Build consumer configurations
     */
    private Map<String, Object> buildConsumerConfigs(KafkaProperties properties) {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, properties.getConsumer().getKeyDeserializer());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, properties.getConsumer().getValueDeserializer());

        KafkaProperties.Consumer consumerConfig = properties.getConsumer();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerConfig.getGroupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerConfig.isEnableAutoCommit());
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, consumerConfig.getAutoCommitIntervalMs());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerConfig.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumerConfig.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerConfig.getMaxPollRecords());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumerConfig.getMaxPollIntervalMs());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getAutoOffsetReset());
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, consumerConfig.getIsolationLevel());

        applySecurityConfig(props, properties);

        return props;
    }

    /**
     * Apply security configurations
     */
    private void applySecurityConfig(Map<String, Object> props, KafkaProperties properties) {
        KafkaProperties.Security security = properties.getSecurity();
        if (security == null) {
            return;
        }

        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, security.getProtocol());

        if (StringUtils.isNotBlank(security.getSaslMechanism())) {
            props.put(SaslConfigs.SASL_MECHANISM, security.getSaslMechanism());
        }

        if (StringUtils.isNotBlank(security.getSaslJaasConfig())) {
            props.put(SaslConfigs.SASL_JAAS_CONFIG, security.getSaslJaasConfig());
        }

        if (StringUtils.isNotBlank(security.getSslTruststoreLocation())) {
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, security.getSslTruststoreLocation());
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, security.getSslTruststorePassword());
        }

        if (StringUtils.isNotBlank(security.getSslKeystoreLocation())) {
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, security.getSslKeystoreLocation());
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, security.getSslKeystorePassword());
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, security.getSslKeyPassword());
        }
    }

    /**
     * Parse SpEL expression or placeholder
     */
    private String parseProperty(String property) {
        if (StringUtils.isBlank(property)) {
            return property;
        }

        if (property.startsWith(RafConstant.DOLLAR_LEFT_BRACE)
                && property.endsWith(RafConstant.RIGHT_BRACE)) {
            String key = property.substring(2, property.length() - 1);
            String[] parts = key.split(":");
            String propertyKey = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : "";
            return environment.getProperty(propertyKey, defaultValue);
        }

        return property;
    }
}
