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
      { text: '规范', link: '/components/api-response' },
      { text: '示例', link: '/examples/raf-example-web-starter' },
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
          text: '版本管理',
          items: [
            { text: '依赖版本清单', link: '/components/dependency-versions' },
          ]
        }
      ],
      '/examples/': [
        {
          text: 'Web 与基础',
          items: [
            { text: 'Web 基础', link: '/examples/raf-example-web-starter' },
            { text: 'API 文档（OpenAPI）', link: '/examples/raf-example-openapi-starter' },
          ]
        },
        {
          text: '数据存储',
          items: [
            { text: 'MyBatis 多数据源', link: '/examples/raf-example-mybatis-starter' },
            { text: 'Redis 缓存与分布式锁', link: '/examples/raf-example-redis-starter' },
            { text: 'MongoDB', link: '/examples/raf-example-mongodb-starter' },
            { text: 'Elasticsearch', link: '/examples/raf-example-elasticsearch-starter' },
            { text: 'ShardingSphere 分库分表', link: '/examples/raf-example-shardingsphere-starter' },
          ]
        },
        {
          text: '消息队列',
          items: [
            { text: 'RabbitMQ', link: '/examples/raf-example-rabbit-starter' },
            { text: 'RocketMQ', link: '/examples/raf-example-rocketmq-starter' },
            { text: 'Kafka', link: '/examples/raf-example-kafka-starter' },
          ]
        },
        {
          text: '微服务',
          items: [
            { text: 'Nacos 配置中心 & 服务发现', link: '/examples/raf-example-nacos-starter' },
            { text: 'Dubbo RPC', link: '/examples/raf-example-dubbo-starter' },
            { text: 'API 网关', link: '/examples/raf-example-gateway-starter' },
            { text: 'OkHttp 第三方调用', link: '/examples/raf-example-okhttp-starter' },
          ]
        },
        {
          text: '安全',
          items: [
            { text: 'KMS 多云密钥管理', link: '/examples/raf-example-kms-starter' },
          ]
        },
        {
          text: '可观测性',
          items: [
            { text: 'Prometheus 监控', link: '/examples/raf-example-monitor-starter' },
            { text: 'Sentry 错误追踪', link: '/examples/raf-example-sentry-starter' },
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
