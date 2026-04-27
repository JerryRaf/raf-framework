/**
 * MongoDB 自动配置包
 * <p>
 * 提供开箱即用的 MongoDB 配置能力，基于 Spring Data MongoDB 实现。
 * <p>
 * 主要功能：
 * <ul>
 *     <li>MongoDB 自动配置（默认禁用，需显式启用）</li>
 *     <li>多 MongoDB 数据源支持（基于 ThreadLocal 动态切换）</li>
 *     <li>分布式追踪集成（ContextHolder 传播）</li>
 *     <li>统一异常处理（转换为 InfrastructureException）</li>
 *     <li>连接池配置和监控</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>{@code
 * # 1. 在 pom.xml 中引入依赖
 * <dependency>
 *     <groupId>io.github.jerryraf</groupId>
 *     <artifactId>raf-framework-mongodb-starter</artifactId>
 * </dependency>
 *
 * # 2. 在配置文件中启用并配置
 * raf:
 *   mongodb:
 *     enabled: true
 *     uri: mongodb://localhost:27017/mydb
 *
 * # 3. 注入 MongoTemplate 使用
 * @Autowired
 * private MongoTemplate mongoTemplate;
 * }</pre>
 *
 * @author Jerry
 * @since 3.0.1
 */
package com.raf.framework.mongodb;
