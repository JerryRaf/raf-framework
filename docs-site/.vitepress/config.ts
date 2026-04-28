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
      { text: '示例', link: '/examples/gateway' },
      { text: 'GitHub', link: 'https://github.com/JerryRaf/raf-framework' }
    ],
    sidebar: {
      '/guide/': [
        {
          text: '入门',
          items: [
            { text: '快速开始', link: '/guide/getting-started' }
          ]
        }
      ],
      '/components/': [
        {
          text: '规范文档',
          items: [
            { text: 'API 响应与错误处理', link: '/components/api-response' },
            { text: 'App API 安全设计', link: '/components/api-security' }
          ]
        }
      ],
      '/examples/': [
        {
          text: '示例项目',
          items: [
            { text: 'Gateway 网关', link: '/examples/gateway' },
            { text: 'Dubbo RPC', link: '/examples/dubbo' },
            { text: '分布式事务', link: '/examples/distributed-tx' },
            { text: '全栈电商', link: '/examples/full-stack' }
          ]
        }
      ]
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/JerryRaf/raf-framework' }
    ],
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2024-present Jerry'
    },
    search: { provider: 'local' }
  }
})
