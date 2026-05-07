/**
 * Kafka integration module
 * Provides best practice encapsulation for Apache Kafka, supporting normal messages and transactional messages.
 *
 * <p>Features:
 * <ul>
 *   <li>Normal message (synchronous/asynchronous send)</li>
 *   <li>Transactional message for distributed transaction</li>
 *   <li>Distributed tracing integration</li>
 *   <li>Auto serialization/deserialization with JSON</li>
 *   <li>Consumer manual/auto commit mode</li>
 *   <li>Security support (SASL/SSL)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * // Enable Kafka
 * raf:
 *   kafka:
 *     enabled: true
 *     bootstrapServers: localhost:9092
 *
 * // Producer
 * {@literal @}Autowired
 * private KafkaProducer kafkaProducer;
 *
 * kafkaProducer.sendSync("topic", "key", messageBody);
 *
 * // Consumer
 * {@literal @}Slf4j
 * {@literal @}KafkaConsumer(topics = "order_topic", groupId = "order_consumer_group")
 * public class OrderListener extends AbstractKafkaConsumerListener&lt;OrderDTO&gt; {
 *     {@literal @}Override
 *     protected boolean handleMessage(OrderDTO message, ConsumerRecord&lt;String, String&gt; record) {
 *         // Business logic here
 *         return true;
 *     }
 * }
 * </pre>
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
package com.raf.framework.kafka;
