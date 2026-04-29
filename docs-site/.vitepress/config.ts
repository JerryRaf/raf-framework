import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'RAF Framework',
  description: '企业级 Spring Boot 3.x 微服务开发框架',
  base: '/raf-framework/',
  lang: 'zh-CN',
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: '指南', link: '/guide/getting-started' },
      { text: '组件', link: '/components/web' },
      { text: '示例', link: '/examples/gateway' },
      { text: 'GitHub', link: 'https://github.com/JerryRaf/raf-framework' }
    ],
    sidebar: {
      '/guide/': [
        {
          text: '入门指南',
          items: [
            { text: '快速开始', link: '/guide/getting-started' },
            { text: '常见问题', link: '/guide/faq' },
            { text: '版本升级指南', link: '/guide/upgrade' },
          ]
        }
      ],
      '/components/': [
        {
          text: '规范文档',
          items: [
            { text: 'API 响应与错误处理', link: '/components/api-response' },
            { text: 'App API 安全设计', link: '/components/api-security' },
          ]
        },
        {
          text: 'Web 与基础',
          items: [
            { text: 'Web 基础', link: '/components/web' },
            { text: 'API 文档（OpenAPI）', link: '/components/openapi' },
            { text: 'Prometheus 监控', link: '/components/monitor' },
            { text: 'Sentry 错误追踪', link: '/components/sentry' },
          ]
        },
        {
          text: '数据存储',
          items: [
            { text: 'MyBatis 多数据源', link: '/components/mybatis' },
            { text: '数据源连接池（Druid）', link: '/components/datasource' },
            { text: 'Redis 缓存与分布式锁', link: '/components/redis' },
            { text: 'MongoDB 多数据源', link: '/components/mongodb' },
            { text: 'Elasticsearch', link: '/components/elasticsearch' },
            { text: 'ShardingSphere 分库分表', link: '/components/shardingsphere' },
          ]
        },
        {
          text: '消息队列',
          items: [
            { text: 'RabbitMQ', link: '/components/rabbit' },
            { text: 'RocketMQ', link: '/components/rocketmq' },
            { text: 'Kafka', link: '/components/kafka' },
          ]
        },
        {
          text: '微服务',
          items: [
            { text: 'Dubbo RPC', link: '/components/dubbo' },
            { text: 'Nacos 配置中心', link: '/components/nacos-config' },
            { text: 'Nacos 服务发现', link: '/components/nacos-discovery' },
            { text: 'API 网关', link: '/components/gateway' },
            { text: 'OkHttp 第三方调用', link: '/components/okhttp' },
          ]
        }
      ],
      '/examples/': [
        {
          text: '示例项目',
          items: [
            { text: 'Gateway 安全网关', link: '/examples/gateway' },
            { text: 'Dubbo RPC', link: '/examples/dubbo' },
            { text: '分布式事务', link: '/examples/distributed-tx' },
            { text: '全栈电商系统', link: '/examples/full-stack' },
          ]
        }
      ]
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/JerryRaf/raf-framework' }
    ],
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2026-present Jerry'
    },
    search: { provider: 'local' }
  }
})
