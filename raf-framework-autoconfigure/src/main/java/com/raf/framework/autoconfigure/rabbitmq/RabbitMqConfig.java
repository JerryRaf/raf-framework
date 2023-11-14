package com.raf.framework.autoconfigure.rabbitmq;

import com.google.common.collect.Maps;
import com.raf.framework.autoconfigure.common.RafConstant;
import com.raf.framework.autoconfigure.jackson.Json;
import com.raf.framework.autoconfigure.spring.condition.ConditionalOnMapProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.rabbit.enabled")
@ConditionalOnClass(CachingConnectionFactory.class)
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig implements BeanFactoryPostProcessor, EnvironmentAware, ApplicationContextAware {
    private ConfigurableListableBeanFactory beanFactory;
    private ApplicationContext applicationContext;
    private ConfigurableEnvironment environment;
    private Json json;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.json = applicationContext.getBean(Json.class);
    }


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Bean
    public CachingConnectionFactory connectionFactory(RabbitMqProperties rabbitMqProperties) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(rabbitMqProperties.getAddresses());
        connectionFactory.setUsername(rabbitMqProperties.getUsername());
        connectionFactory.setPassword(rabbitMqProperties.getPassword());
        Optional.ofNullable(rabbitMqProperties.getProvider()).ifPresent(c -> {
            //生成者confirm开关
            connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            connectionFactory.setPublisherReturns(c.isAck());
        });
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cachingConnectionFactory) {
        return new RabbitTemplate(cachingConnectionFactory);
    }

    @Bean
    @ConditionalOnBean(Json.class)
    public RabbitMessageCacheMgr messageCacheMgr(RabbitTemplate rabbitTemplate) {
        return new RabbitMessageCacheMgr(rabbitTemplate, json);
    }

    @Bean("rabbitMqMessageSender")
    @ConditionalOnMapProperty(prefix = "raf.rabbit.provider")
    public RabbitMqMessageSender rabbitMqMessageSender(RabbitTemplate rabbitTemplate, RabbitMessageCacheMgr rabbitMessageCacheMgr, RabbitMqProperties rabbitMqProperties) {
        Map<String, AbstractRabbitSenderConfirm> confirmReturnCallbacks = applicationContext.getBeansOfType(AbstractRabbitSenderConfirm.class);
        if (!confirmReturnCallbacks.isEmpty()) {
            AbstractRabbitSenderConfirm confirmReturnCallback = confirmReturnCallbacks.values().toArray(new AbstractRabbitSenderConfirm[]{})[0];
            rabbitTemplate.setConfirmCallback(confirmReturnCallback);
            rabbitTemplate.setReturnCallback(confirmReturnCallback);
        }
        //为true时,消息通过交换器无法匹配到队列会返回给生产者，为false时匹配不到会直接被丢弃
        rabbitTemplate.setMandatory(true);
        return new RabbitMqMessageSender(rabbitTemplate, rabbitMqProperties.getProvider(), rabbitMqProperties.getDelay(), json, rabbitMessageCacheMgr);
    }


    @Bean("listenerContainers")
    @ConditionalOnMapProperty(prefix = "raf.rabbit.consumer")
    public List<SimpleMessageListenerContainer> listenerContainers(CachingConnectionFactory factory, RabbitMqProperties rabbitMqProperties) {
        String[] consumers = applicationContext.getBeanNamesForAnnotation(RabbitMqConsumer.class);
        return Arrays.stream(consumers).map(c -> bindConsumerListener(c, factory, rabbitMqProperties)
        ).collect(Collectors.toList());
    }

    private void register(ConfigurableListableBeanFactory beanFactory, Object bean, String name) {
        beanFactory.registerSingleton(name, bean);
    }

    /**
     * 绑定消费者监听器
     */
    private SimpleMessageListenerContainer bindConsumerListener(String beanName, CachingConnectionFactory factory, RabbitMqProperties rabbitMqProperties) {
        AbstractRabbitConsumerListener listener = applicationContext.getBean(beanName, AbstractRabbitConsumerListener.class);
        Class<?> clazz = listener.getClass();
        if (AopUtils.isAopProxy(listener)) {
            clazz = AopUtils.getTargetClass(listener);
        }
        RabbitMqConsumer consumerAnnotation = clazz.getAnnotation(RabbitMqConsumer.class);
        String qName = parseVars(consumerAnnotation.queue());
        String exchange = parseVars(consumerAnnotation.exchange());
        String routingKey = parseVars(consumerAnnotation.routingKey());

        TopicExchange topicExchange = new TopicExchange(exchange);
        register(beanFactory, topicExchange, exchange + "_exchange");

        //创建队列
        Queue queue = new Queue(qName);

        //注册queue对象
        register(beanFactory, queue, rabbitMqProperties.getConsumer().getGroup() + "_" + listener.getClass().getSimpleName() + "_queue");
        //注册绑定关系
        Binding binding = BindingBuilder.bind(queue).to(topicExchange).with(routingKey);
        register(beanFactory, binding, listener.getClass().getSimpleName() + "_binding");

        //创建消息监听容器
        return createMessageListenerContainer(queue, listener, consumerAnnotation.ackModel(), factory);
    }

    /**
     * 会去rabbmitmq创建对应的队列，交换机，路由
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    /**
     * 创建消息监听容器
     */
    private SimpleMessageListenerContainer createMessageListenerContainer(Queue queue, AbstractRabbitConsumerListener listener, AcknowledgeMode model, CachingConnectionFactory factory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factory);
        container.setQueues(queue);
        container.setExposeListenerChannel(true);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(3);
        container.setAcknowledgeMode(model);
        container.setMessageListener(listener);
        register(beanFactory, container, queue.getName() + "_listenerContainer");
        return container;
    }

    @Bean("listenerDelayContainers")
    @ConditionalOnMapProperty(prefix = "raf.rabbit.delay")
    public String listenerDelayContainers(RabbitMqProperties rabbitMqProperties) {
        RabbitMqProperties.RabbitMqDelayProvider rabbitMqDelayProvider = rabbitMqProperties.getDelay();
        String groupName = rabbitMqProperties.getConsumer().getGroup() + "-";

        TopicExchange deadExchange = new TopicExchange(rabbitMqDelayProvider.getDeadExchange());
        register(beanFactory, deadExchange, groupName.concat("deadExchange"));

        TopicExchange receiveExchange = new TopicExchange(rabbitMqDelayProvider.getReceiveExchange());
        register(beanFactory, receiveExchange, groupName.concat("receiveExchange"));

        if (CollectionUtils.isEmpty(rabbitMqDelayProvider.getQueuePrefix())) {
            return Strings.EMPTY;
        }

        rabbitMqDelayProvider.getQueuePrefix().forEach(c -> {
            String receiveQueueStr = c.concat(".receive.queue");
            String receiveRouteStr = c.concat(".receive.route");
            String deadQueueStr = c.concat(".dead.queue");
            String deadRouteStr = c.concat(".dead.route");

            Queue queue = new Queue(receiveQueueStr);
            register(beanFactory, queue, groupName.concat(receiveQueueStr));

            Binding binding = BindingBuilder.bind(queue).to(receiveExchange).with(receiveRouteStr);
            register(beanFactory, binding, groupName.concat(receiveQueueStr).concat("_binding"));

            Map<String, Object> arguments = Maps.newHashMapWithExpectedSize(4);
            arguments.put("x-dead-letter-exchange", rabbitMqDelayProvider.getReceiveExchange());
            arguments.put("x-dead-letter-routing-key", receiveRouteStr);
            Queue deadQueue = new Queue(deadQueueStr, true, false, false, arguments);
            register(beanFactory, deadQueue, groupName.concat(deadQueueStr));

            Binding delayBinding = BindingBuilder.bind(deadQueue).to(deadExchange).with(deadRouteStr);
            register(beanFactory, delayBinding, groupName.concat(deadQueueStr).concat("_binding"));
        });
        return Strings.EMPTY;
    }

    private String parseVars(String el) {
        if (el.startsWith(RafConstant.DOLLAR_LEFT_BRACE) && el.endsWith(RafConstant.RIGHT_BRACE)) {
            return this.environment.getProperty(StringUtils.removeEnd(StringUtils.removeStart(el, RafConstant.DOLLAR_LEFT_BRACE), RafConstant.RIGHT_BRACE));
        }
        return el;
    }
}
