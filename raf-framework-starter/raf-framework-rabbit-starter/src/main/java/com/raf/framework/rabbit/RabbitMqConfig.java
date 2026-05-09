package com.raf.framework.rabbit;

import com.raf.framework.core.jackson.JsonService;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

/**
 * RabbitMQ 自动配置。
 *
 * <p>拓扑声明（Exchange/Queue/Binding）由生产者服务通过 {@code raf.rabbit.bindings} 配置驱动，
 * 消费者服务只需通过 {@code @RabbitMqConsumer(queue = "...")} 指定监听队列。
 *
 * @author Jerry
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "raf.rabbit", name = "enabled", havingValue = "true")
@ConditionalOnClass(CachingConnectionFactory.class)
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig implements BeanFactoryPostProcessor, ApplicationContextAware {

    private ConfigurableListableBeanFactory beanFactory;
    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    // -------------------------------------------------------------------------
    // 连接工厂
    // -------------------------------------------------------------------------

    @Bean
    public CachingConnectionFactory connectionFactory(RabbitMqProperties props) {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setAddresses(props.getAddresses());
        factory.setUsername(props.getUsername());
        factory.setPassword(props.getPassword());
        factory.setVirtualHost(props.getVirtualHost());
        String appName = applicationContext.getEnvironment()
                .getProperty("spring.application.name", "unknown");
        factory.setConnectionNameStrategy(c -> appName + "-" + c.getHost());
        // 修复 bug：publisher confirm 仅在 raf.rabbit.provider.ack=true 时开启
        if (props.getProvider() != null && props.getProvider().isAck()) {
            factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            factory.setPublisherReturns(true);
        }
        if (props.getSsl() != null && props.getSsl().isEnabled()) {
            applySsl(factory, props.getSsl());
        }
        return factory;
    }

    private void applySsl(CachingConnectionFactory factory, RabbitMqProperties.Ssl ssl) {
        try {
            SSLContext ctx = SSLContextBuilder.create()
                    .loadKeyMaterial(ResourceUtils.getFile(ssl.getKeyStore()),
                            ssl.getKeyStorePassword().toCharArray(),
                            ssl.getKeyStorePassword().toCharArray())
                    .loadTrustMaterial(ResourceUtils.getFile(ssl.getTrustStore()),
                            ssl.getTrustStorePassword().toCharArray())
                    .build();
            factory.getRabbitConnectionFactory().useSslProtocol(ctx);
        } catch (Exception e) {
            throw new BeanInitializationException("RabbitMQ SSL configuration failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // RabbitTemplate / RabbitAdmin / RabbitMqMessageSender
    // -------------------------------------------------------------------------

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory factory, RabbitMqProperties props) {
        RabbitTemplate template = new RabbitTemplate(factory);
        if (props.getProvider() != null && props.getProvider().isAck()) {
            template.setMandatory(true);
            Map<String, AbstractRabbitSenderConfirm> confirms =
                    applicationContext.getBeansOfType(AbstractRabbitSenderConfirm.class);
            if (!confirms.isEmpty()) {
                AbstractRabbitSenderConfirm confirm = confirms.values().iterator().next();
                template.setConfirmCallback(confirm);
                template.setReturnsCallback(confirm);
                log.info("RabbitMQ confirm callback registered: {}", confirm.getClass().getSimpleName());
            } else {
                log.warn("raf.rabbit.provider.ack=true but no AbstractRabbitSenderConfirm bean found. "
                        + "Implement AbstractRabbitSenderConfirm to handle send failures.");
            }
        }
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public RabbitMqMessageSender rabbitMqMessageSender(RabbitTemplate rabbitTemplate, JsonService json) {
        return new RabbitMqMessageSender(rabbitTemplate, json);
    }

    // -------------------------------------------------------------------------
    // 拓扑声明：由 raf.rabbit.bindings 配置驱动
    // -------------------------------------------------------------------------

    /**
     * 根据 {@code raf.rabbit.bindings} 配置自动声明 Exchange/Queue/Binding。
     *
     * <p>生产者服务配置此项，消费者服务不需要配置 bindings。
     * {@link RabbitAdmin} 会在连接建立后自动执行声明。
     */
    @Bean
    public List<Declarable> rabbitTopologyDeclarations(RabbitMqProperties props) {
        List<RabbitMqProperties.BindingDefinition> bindings = props.getBindings();
        if (CollectionUtils.isEmpty(bindings)) {
            log.debug("raf.rabbit.bindings is empty, no topology will be declared by this service.");
            return List.of();
        }
        return bindings.stream()
                .flatMap(def -> buildDeclarables(def).stream())
                .collect(Collectors.toList());
    }

    private List<Declarable> buildDeclarables(RabbitMqProperties.BindingDefinition def) {
        List<Declarable> result = new ArrayList<>();

        // 1. 声明 Exchange
        Exchange exchange = buildExchange(def.getExchange(), def.getExchangeType(), def.isDurable());
        result.add(exchange);

        // 2. 声明 Queue（带自定义参数）
        Map<String, Object> args = new HashMap<>(def.getArguments());
        if (def.getDelay() != null && def.getDelay().getTtl() > 0) {
            args.put("x-message-ttl", def.getDelay().getTtl());
        }
        Queue queue = QueueBuilder.durable(def.getQueue()).withArguments(args).build();
        result.add(queue);

        // 3. 声明 Binding（queue -> exchange）
        result.add(buildBinding(queue, exchange, def.getRoutingKey()));

        // 4. 延迟队列：额外声明 dead exchange + dead queue + dead binding
        if (def.getDelay() != null) {
            RabbitMqProperties.DelayConfig delay = def.getDelay();

            Exchange deadExchange = buildExchange(
                    delay.getDeadExchange(), delay.getDeadExchangeType(), def.isDurable());
            result.add(deadExchange);

            // dead queue 设置 DLX 参数，消息过期后路由到 receive exchange
            Map<String, Object> deadArgs = new HashMap<>();
            deadArgs.put("x-dead-letter-exchange", def.getExchange());
            deadArgs.put("x-dead-letter-routing-key", def.getRoutingKey());
            Queue deadQueue = QueueBuilder.durable(delay.getDeadQueue())
                    .withArguments(deadArgs).build();
            result.add(deadQueue);

            result.add(buildBinding(deadQueue, deadExchange, delay.getDeadRoutingKey()));

            log.info("Declared delay topology: deadExchange={}, deadQueue={} -> exchange={}, queue={}",
                    delay.getDeadExchange(), delay.getDeadQueue(), def.getExchange(), def.getQueue());
        } else {
            log.info("Declared topology: exchange={}, queue={}, routingKey={}",
                    def.getExchange(), def.getQueue(), def.getRoutingKey());
        }
        return result;
    }

    private Exchange buildExchange(String name, String type, boolean durable) {
        switch (type.toLowerCase()) {
            case "topic":   return ExchangeBuilder.topicExchange(name).durable(durable).build();
            case "fanout":  return ExchangeBuilder.fanoutExchange(name).durable(durable).build();
            case "headers": return ExchangeBuilder.headersExchange(name).durable(durable).build();
            default:        return ExchangeBuilder.directExchange(name).durable(durable).build();
        }
    }

    private Binding buildBinding(Queue queue, Exchange exchange, String routingKey) {
        if (exchange instanceof FanoutExchange) {
            return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }
        if (exchange instanceof TopicExchange) {
            return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(routingKey);
        }
        if (exchange instanceof HeadersExchange) {
            return BindingBuilder.bind(queue).to((HeadersExchange) exchange).whereAny(new HashMap<>()).match();
        }
        // default: DirectExchange
        return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(routingKey);
    }

    // -------------------------------------------------------------------------
    // 消费者监听容器注册
    // -------------------------------------------------------------------------

    /**
     * 扫描所有带 {@link RabbitMqConsumer} 注解的 Bean，注册 {@link SimpleMessageListenerContainer}。
     * 同时兼容扫描已废弃的 {@link RabbitMqDelayConsumer} 注解。
     *
     * <p>消费者只需指定 queue 名，不再负责声明拓扑。
     * queue 名支持 {@code ${...}} 占位符，从 Spring Environment 解析。
     */
    @Bean
    public List<SimpleMessageListenerContainer> listenerContainers(
            CachingConnectionFactory factory, RabbitMqProperties props) {
        List<SimpleMessageListenerContainer> containers = new ArrayList<>();

        // 扫描 @RabbitMqConsumer
        String[] consumerBeans = applicationContext.getBeanNamesForAnnotation(RabbitMqConsumer.class);
        for (String name : consumerBeans) {
            containers.add(buildListenerContainer(name, factory, props));
        }

        // 兼容扫描 @RabbitMqDelayConsumer（已废弃，建议迁移到 @RabbitMqConsumer）
        String[] delayBeans = applicationContext.getBeanNamesForAnnotation(RabbitMqDelayConsumer.class);
        for (String name : delayBeans) {
            containers.add(buildDelayListenerContainer(name, factory, props));
        }

        return containers;
    }

    private SimpleMessageListenerContainer buildListenerContainer(
            String beanName, CachingConnectionFactory factory, RabbitMqProperties props) {
        AbstractRabbitConsumerListener listener =
                applicationContext.getBean(beanName, AbstractRabbitConsumerListener.class);
        Class<?> clazz = AopUtils.isAopProxy(listener) ? AopUtils.getTargetClass(listener) : listener.getClass();
        RabbitMqConsumer annotation = clazz.getAnnotation(RabbitMqConsumer.class);

        // 支持 ${...} 占位符解析，实现多环境队列名配置
        String queueName = applicationContext.getEnvironment()
                .resolvePlaceholders(annotation.queue());

        if (!org.springframework.util.StringUtils.hasText(queueName)) {
            throw new BeanInitializationException(
                    "RabbitMqConsumer on " + clazz.getSimpleName() + " resolved to blank queue name. " +
                    "Check @RabbitMqConsumer(queue=...) and your configuration.");
        }

        Queue queue = new Queue(queueName, true);
        if (beanFactory.containsSingleton(queueName + "_queue")) {
            log.warn("Duplicate @RabbitMqConsumer for queue '{}' detected. " +
                    "Multiple consumers on the same queue in one service is unusual.", queueName);
        }
        beanFactory.registerSingleton(queueName + "_queue", queue);

        RabbitMqProperties.Consumer consumer = props.getConsumer();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factory);
        container.setQueues(queue);
        container.setExposeListenerChannel(true);
        container.setConcurrentConsumers(consumer != null ? consumer.getConcurrentConsumers() : 3);
        container.setMaxConcurrentConsumers(consumer != null ? consumer.getMaxConcurrentConsumers() : 10);
        container.setAcknowledgeMode(annotation.ackModel());
        container.setMessageListener(listener);

        beanFactory.registerSingleton(queueName + "_container", container);
        log.info("Registered listener container: queue={}, consumer={}", queueName, clazz.getSimpleName());
        return container;
    }

    @SuppressWarnings("deprecation")
    private SimpleMessageListenerContainer buildDelayListenerContainer(
            String beanName, CachingConnectionFactory factory, RabbitMqProperties props) {
        AbstractRabbitConsumerListener listener =
                applicationContext.getBean(beanName, AbstractRabbitConsumerListener.class);
        Class<?> clazz = AopUtils.isAopProxy(listener) ? AopUtils.getTargetClass(listener) : listener.getClass();
        RabbitMqDelayConsumer annotation = clazz.getAnnotation(RabbitMqDelayConsumer.class);

        String queueName = applicationContext.getEnvironment()
                .resolvePlaceholders(annotation.businessName());

        if (!org.springframework.util.StringUtils.hasText(queueName)) {
            throw new BeanInitializationException(
                    "RabbitMqDelayConsumer on " + clazz.getSimpleName() + " resolved to blank queue name. " +
                    "Check @RabbitMqDelayConsumer(businessName=...) and your configuration.");
        }

        log.warn("@RabbitMqDelayConsumer on {} is deprecated. Migrate to @RabbitMqConsumer(queue=\"{}\").",
                clazz.getSimpleName(), queueName);

        Queue queue = new Queue(queueName, true);
        if (beanFactory.containsSingleton(queueName + "_queue")) {
            log.warn("Duplicate consumer for queue '{}' detected.", queueName);
        }
        beanFactory.registerSingleton(queueName + "_queue", queue);

        RabbitMqProperties.Consumer consumer = props.getConsumer();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factory);
        container.setQueues(queue);
        container.setExposeListenerChannel(true);
        container.setConcurrentConsumers(consumer != null ? consumer.getConcurrentConsumers() : 3);
        container.setMaxConcurrentConsumers(consumer != null ? consumer.getMaxConcurrentConsumers() : 10);
        container.setAcknowledgeMode(annotation.ackModel());
        container.setMessageListener(listener);

        beanFactory.registerSingleton(queueName + "_container", container);
        log.info("Registered delay listener container (deprecated): queue={}, consumer={}",
                queueName, clazz.getSimpleName());
        return container;
    }
}
