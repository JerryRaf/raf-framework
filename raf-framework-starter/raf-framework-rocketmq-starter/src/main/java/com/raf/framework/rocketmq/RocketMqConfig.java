package com.raf.framework.rocketmq;

import com.raf.framework.core.common.RafConstant;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.jackson.JsonService;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.RPCHook;
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

/**
 * RocketMQ 自动配置类
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.rocketmq.enabled", havingValue = "true")
@ConditionalOnClass(DefaultMQProducer.class)
@EnableConfigurationProperties(RocketMqProperties.class)
public class RocketMqConfig implements BeanFactoryPostProcessor, EnvironmentAware, ApplicationContextAware {

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
     * 创建默认生产者
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(DefaultMQProducer.class)
    @ConditionalOnProperty(prefix = "raf.rocketmq.producer", name = "group")
    public DefaultMQProducer defaultMQProducer(RocketMqProperties properties) throws MQClientException {
        RocketMqProperties.Producer producerConfig = properties.getProducer();

        RPCHook rpcHook = buildRpcHook(properties);
        DefaultMQProducer producer = rpcHook != null
                ? new DefaultMQProducer(producerConfig.getGroup(), rpcHook)
                : new DefaultMQProducer(producerConfig.getGroup());
        producer.setNamesrvAddr(properties.getNameServer());
        producer.setSendMsgTimeout(producerConfig.getSendMsgTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMsgBodyOverHowmuch());
        producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryAnotherBrokerWhenNotStoreOK());

        log.info("RocketMQ Producer started. NameServer:{}, Group:{}",
                properties.getNameServer(), producerConfig.getGroup());

        return producer;
    }

    /**
     * 创建事务生产者
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(TransactionMQProducer.class)
    @ConditionalOnProperty(prefix = "raf.rocketmq.producer", name = {"group", "enableTransaction"}, havingValue = "true")
    public TransactionMQProducer transactionMQProducer(RocketMqProperties properties) throws MQClientException {
        RocketMqProperties.Producer producerConfig = properties.getProducer();
        String txGroup = producerConfig.getGroup() + "_TRANSACTION";

        RPCHook rpcHook = buildRpcHook(properties);
        TransactionMQProducer producer = rpcHook != null
                ? new TransactionMQProducer(txGroup, rpcHook)
                : new TransactionMQProducer(txGroup);
        producer.setNamesrvAddr(properties.getNameServer());
        producer.setSendMsgTimeout(producerConfig.getSendMsgTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());

        // Transaction check thread pool
        ExecutorService executorService = new ThreadPoolExecutor(
                producerConfig.getCheckThreadPoolSize(),
                producerConfig.getCheckThreadPoolSize(),
                100,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(producerConfig.getCheckThreadPoolQueueCapacity()),
                r -> new Thread(r, "RocketMQ-Transaction-Check-Thread")
        );
        producer.setExecutorService(executorService);

        // Set transaction listener
        Map<String, RocketMqTransactionListener> listeners =
                applicationContext.getBeansOfType(RocketMqTransactionListener.class);
        if (!listeners.isEmpty()) {
            RocketMqTransactionListener listener = listeners.values().iterator().next();
            producer.setTransactionListener(listener);
            log.info("RocketMQ transaction listener set: {}", listener.getClass().getSimpleName());
        }

        log.info("RocketMQ Transaction Producer started. NameServer:{}, Group:{}",
                properties.getNameServer(), txGroup);

        return producer;
    }

    /**
     * 创建RocketMQ生产者封装
     */
    @Bean
    @ConditionalOnMissingBean(RocketMqProducer.class)
    public RocketMqProducer rocketMqProducer(DefaultMQProducer producer,
                                             RocketMqProperties properties,
                                             JsonService jsonService) {
        TransactionMQProducer transactionProducer = null;
        try {
            transactionProducer = applicationContext.getBean(TransactionMQProducer.class);
        } catch (Exception e) {
            // 事务生产者未启用
        }

        return new RocketMqProducer(
                producer,
                transactionProducer,
                jsonService,
                properties.getProducer() != null && properties.getProducer().isEnableTransaction()
        );
    }

    /**
     * 注册消费者
     */
    @Bean
    @ConditionalOnProperty(prefix = "raf.rocketmq.consumer", name = "group")
    public String registerConsumers(RocketMqProperties properties) {
        String[] consumerBeans = applicationContext.getBeanNamesForAnnotation(RocketMqConsumer.class);

        if (consumerBeans.length == 0) {
        log.warn("No @RocketMqConsumer annotated consumers found");
            return "noConsumer";
        }

        Arrays.stream(consumerBeans)
                .forEach(beanName -> registerConsumer(beanName, properties));

            log.info("RocketMQ consumers registered, total={}", consumerBeans.length);
        return "success";
    }

    /**
     * 注册单个消费者
     */
    private void registerConsumer(String beanName, RocketMqProperties properties) {
        try {
            AbstractRocketMqConsumerListener<?> listener =
                    applicationContext.getBean(beanName, AbstractRocketMqConsumerListener.class);

            Class<?> clazz = listener.getClass();
            if (AopUtils.isAopProxy(listener)) {
                clazz = AopUtils.getTargetClass(listener);
            }

            RocketMqConsumer annotation = clazz.getAnnotation(RocketMqConsumer.class);
            if (annotation == null) {
                return;
            }

            // 解析配置
            String consumerGroup = parseProperty(annotation.consumerGroup());
            String topic = parseProperty(annotation.topic());
            String tag = parseProperty(annotation.tag());

            // 创建消费者
            RPCHook rpcHook = buildRpcHook(properties);
            DefaultMQPushConsumer consumer = rpcHook != null
                    ? new DefaultMQPushConsumer(consumerGroup, rpcHook)
                    : new DefaultMQPushConsumer(consumerGroup);
            consumer.setNamesrvAddr(properties.getNameServer());

            RocketMqProperties.Consumer consumerConfig = properties.getConsumer();
            if (consumerConfig != null) {
                // 消费模式
                consumer.setMessageModel(annotation.messageModel());

                // 消费起始位置
                if ("CONSUME_FROM_FIRST_OFFSET".equals(consumerConfig.getConsumeFromWhere())) {
                    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                } else if ("CONSUME_FROM_TIMESTAMP".equals(consumerConfig.getConsumeFromWhere())) {
                    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
                } else {
                    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
                }

                // 线程池配置
                consumer.setConsumeThreadMin(annotation.consumeThreadMin());
                consumer.setConsumeThreadMax(annotation.consumeThreadMax());
                consumer.setConsumeMessageBatchMaxSize(consumerConfig.getConsumeMessageBatchMaxSize());
                consumer.setPullBatchSize(consumerConfig.getPullBatchSize());
                consumer.setConsumeTimeout(consumerConfig.getConsumeTimeout());

                if (consumerConfig.getMaxReconsumeTimes() > 0) {
                    consumer.setMaxReconsumeTimes(consumerConfig.getMaxReconsumeTimes());
                }
            }

            // 订阅主题
            consumer.subscribe(topic, tag);

            // Set message listener
            if (annotation.messageType() == RocketMqMessage.MessageType.FIFO) {
                // Orderly message
                consumer.registerMessageListener((MessageListenerOrderly) listener);
            } else {
                // Concurrent / delay message
                consumer.registerMessageListener((MessageListenerConcurrently) listener);
            }

            // Start consumer
            consumer.start();

            // 注册到Spring容器
            String consumerBeanName = consumerGroup + "_" + topic + "_Consumer";
            beanFactory.registerSingleton(consumerBeanName, consumer);

            log.info("RocketMQ consumer started. Group:{}, Topic:{}, Tag:{}, MessageType:{}",
                    consumerGroup, topic, tag, annotation.messageType());

        } catch (Exception e) {
            log.error("RocketMQ consumer registration failed: {}", beanName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "RocketMQ consumer registration failed", e);
        }
    }

    /**
     * Build ACL RPCHook if credentials are configured (Alibaba Cloud RocketMQ).
     *
     * @param properties RocketMQ properties
     * @return RPCHook instance, or null if credentials are not configured
     */
    private RPCHook buildRpcHook(RocketMqProperties properties) {
        RocketMqProperties.Credentials credentials = properties.getCredentials();
        if (credentials != null
                && StringUtils.isNotBlank(credentials.getAccessKey())
                && StringUtils.isNotBlank(credentials.getSecretKey())) {
            return new AclClientRPCHook(
                    new SessionCredentials(credentials.getAccessKey(), credentials.getSecretKey())
            );
        }
        return null;
    }

    /**
     * 解析SpEL表达式或占位符
     */
    private String parseProperty(String property) {
        if (StringUtils.isBlank(property)) {
            return property;
        }

        // 解析占位符 ${xxx}
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
