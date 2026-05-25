<template>

  <!-- ── Stats ─────────────────────────────────────────────────────────────── -->
  <section class="raf-stats">
    <div class="raf-stats__wrap">
      <div v-for="(stat, i) in stats" :key="i" class="raf-stat">
        <span class="raf-stat__value">{{ stat.value }}</span>
        <span class="raf-stat__label">{{ stat.label }}</span>
      </div>
    </div>
  </section>

  <!-- ── Feature Cards ─────────────────────────────────────────────────────── -->
  <section class="raf-features">
    <div class="raf-features__header">
      <span class="raf-eyebrow">CORE CAPABILITIES</span>
      <h2 class="raf-features__title">四大核心能力</h2>
      <p class="raf-features__sub">从接入规范到安全合规，覆盖微服务开发的全部横切关注点。</p>
    </div>
    <div class="raf-features__grid">
      <article
        v-for="(feature, i) in features"
        :key="i"
        class="raf-card"
        :style="{ '--delay': `${i * 90}ms` }"
      >
        <div class="raf-card__index">{{ String(i + 1).padStart(2, '0') }}</div>
        <div class="raf-card__icon" v-html="feature.icon"></div>
        <h3 class="raf-card__title">{{ feature.title }}</h3>
        <p class="raf-card__details">{{ feature.details }}</p>
        <div class="raf-card__tag">{{ feature.tag }}</div>
      </article>
    </div>
  </section>

  <!-- ── Design Philosophy ─────────────────────────────────────────────────── -->
  <section class="raf-section raf-section--philosophy">
    <div class="raf-section__wrap">
      <header class="raf-section__header">
        <span class="raf-eyebrow">DESIGN PHILOSOPHY</span>
        <h2 class="raf-section__title">三条核心设计原则</h2>
        <p class="raf-section__sub">
          不是另一个脚手架，而是一个中间件接入层。每一条原则都来自真实的团队痛点。
        </p>
      </header>
      <div class="raf-philosophy">
        <div v-for="(p, i) in philosophy" :key="i" class="raf-phi">
          <div class="raf-phi__num">{{ String(i + 1).padStart(2, '0') }}</div>
          <div class="raf-phi__body">
            <div class="raf-phi__title">{{ p.title }}</div>
            <div class="raf-phi__desc">{{ p.desc }}</div>
            <code class="raf-phi__code">{{ p.code }}</code>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- ── Architecture ───────────────────────────────────────────────────────── -->
  <section class="raf-section">
    <div class="raf-section__wrap">
  <header class="raf-section__header">
        <span class="raf-eyebrow">ARCHITECTURE</span>
        <h2 class="raf-section__title">四层模块体系</h2>
        <p class="raf-section__sub">
          从依赖治理到业务接入，每一层职责清晰、边界分明。<br>
          业务项目只需选择所需 Starter，其余全部由框架托管。
        </p>
      </header>
      <div class="raf-layers">
        <div v-for="(layer, i) in layers" :key="layer.name" class="raf-layer" :style="{ '--i': i }">
          <div class="raf-layer__connector" v-if="i < layers.length - 1"></div>
          <div class="raf-layer__badge">{{ layer.badge }}</div>
          <div class="raf-layer__body">
            <div class="raf-layer__name">{{ layer.name }}</div>
            <div class="raf-layer__desc">{{ layer.desc }}</div>
          </div>
          <div class="raf-layer__arrow" v-html="arrowSvg"></div>
        </div>
      </div>
    </div>
  </section>

  <!-- ── Starter Catalog ───────────────────────────────────────────────────── -->
  <section class="raf-section raf-section--catalog">

    <div class="raf-section__wrap">
      <header class="raf-section__header">
        <span class="raf-eyebrow">STARTER CATALOG</span>
        <h2 class="raf-section__title">开箱即用的组件库</h2>
        <p class="raf-section__sub">
          引入对应 Starter 并设置 <code>enabled=true</code>，即可获得完整的生产级能力。
        </p>
      </header>
      <div class="raf-catalog">
        <div v-for="group in catalog" :key="group.group" class="raf-catalog__group">
          <div class="raf-catalog__group-header">
            <span class="raf-catalog__group-icon" v-html="group.icon"></span>
            <span class="raf-catalog__group-name">{{ group.group }}</span>
          </div>
          <div class="raf-catalog__items">
            <a
              v-for="item in group.items"
              :key="item.name"
              :href="item.link"
              class="raf-catalog__item"
            >
              <span class="raf-catalog__item-name">{{ item.name }}</span>
              <span class="raf-catalog__item-arrow">→</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  </section>

</template>

<script setup lang="ts">
const arrowSvg = `<svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 8h10M9 4l4 4-4 4"/></svg>`

const features = [
  {
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>`,
    title: '按需接入，零副作用',
    details: '所有组件默认关闭。raf.{component}.enabled=true 一行启用，不引入 Starter 则零自动装配、零 Bean 注册，对现有工程完全无侵入。',
    tag: '@ConditionalOnProperty',
  },
  {
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>`,
    title: '全链路规范，开箱即得',
    details: 'RafResult<T> 统一响应、四层异常体系、HTTP 请求日志、分布式 TraceId 全链路传播。团队无需自定义规范，接入即对齐。',
    tag: 'RFC-compliant',
  },
  {
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>`,
    title: '内置可观测，无需额外搭建',
    details: '慢 SQL 300ms/500ms 双阈值告警、线程池 Prometheus 指标暴露、优雅停机 30s 等待、Sentry 错误上报，生产监控开箱即用。',
    tag: 'Production-grade',
  },
  {
    icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
    title: '多云安全，通过企业审计',
    details: '网关层 ECIES 加密 + ECDSA 签名 + 防重放，多云 KMS 统一密钥管理，Jasypt 配置加密，XSS 过滤。通过 OWASP / Fortify 安全扫描。',
    tag: 'OWASP-compliant',
  },
]

const stats = [
  { value: '21+', label: 'Starters' },
  { value: 'Spring Boot 3.4', label: '基础框架' },
  { value: 'Java 17+', label: '运行环境' },
  { value: '4 大云厂商', label: 'KMS 多云支持' },
  { value: '0 侵入', label: '不引入零副作用' },
  { value: 'AI-Ready', label: 'Spring AI 集成' },
]

const philosophy = [
  {
    title: '热插拔，不引入零侵入',
    desc: '每个微服务项目都要重复写 Redis 配置、MQ 消费者、多数据源路由、全局异常处理。RAF 把这些横切关注点封装成独立 Starter，引入依赖即生效，不引入则对工程完全透明。',
    code: 'raf.redis.enabled=true',
  },
  {
    title: '零硬编码，配置与代码彻底解耦',
    desc: '地址、密钥、账号等所有环境配置全部由外部配置中心（Nacos）注入。代码仓库中不存在任何环境相关的硬编码值，同一份制品可在开发、测试、生产环境无缝流转。',
    code: 'spring.cloud.nacos.config.server-addr=${NACOS_ADDR}',
  },
  {
    title: '双接入模式，适配任何团队规范',
    desc: 'Maven 单继承限制是企业项目的常见约束。RAF 同时支持 parent 继承和 BOM 组合两种方式，无论团队是否已有父工程，都能以最小改动完成接入，不破坏现有工程结构。',
    code: '<type>pom</type><scope>import</scope>',
  },
]

const layers = [  {
    badge: 'BOM',
    name: 'raf-framework-dependencies',
    desc: '统一管理所有第三方依赖版本，业务项目无需关心版本冲突',
  },
  {
    badge: 'PARENT',
    name: 'raf-framework-parent',
    desc: '提供 Maven 插件配置、flatten 版本展开、编译参数统一设置',
  },
  {
    badge: 'CORE',
    name: 'raf-framework-core',
    desc: '自动配置实现层：Jackson、异常处理、TraceId、工具类等核心能力',
  },
  {
    badge: 'STARTER',
    name: 'raf-framework-*-starter',
    desc: '按功能拆分的热插拔组件，引入即可用，不引入零副作用',
  },
]

const catalog = [
  {
    group: 'Web 与基础',
    icon: `<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="16" height="14" rx="2"/><path d="M2 7h16"/></svg>`,
    items: [
      { name: 'web-starter', link: '/raf-framework/examples/raf-example-web-starter' },
      { name: 'openapi-starter', link: '/raf-framework/examples/raf-example-openapi-starter' },
    ],
  },
  {
    group: '数据存储',
    icon: `<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"><ellipse cx="10" cy="5" rx="7" ry="2.5"/><path d="M3 5v10c0 1.38 3.13 2.5 7 2.5s7-1.12 7-2.5V5"/><path d="M3 10c0 1.38 3.13 2.5 7 2.5s7-1.12 7-2.5"/></svg>`,
    items: [
      { name: 'mybatis-starter', link: '/raf-framework/examples/raf-example-mybatis-starter' },
      { name: 'redis-starter', link: '/raf-framework/examples/raf-example-redis-starter' },
      { name: 'mongodb-starter', link: '/raf-framework/examples/raf-example-mongodb-starter' },
      { name: 'elasticsearch-starter', link: '/raf-framework/examples/raf-example-elasticsearch-starter' },
      { name: 'shardingsphere-starter', link: '/raf-framework/examples/raf-example-shardingsphere-starter' },
    ],
  },
  {
    group: '消息队列',
    icon: `<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 6h14M3 10h14M3 14h14"/></svg>`,
    items: [
      { name: 'rabbit-starter', link: '/raf-framework/examples/raf-example-rabbit-starter' },
      { name: 'rocketmq-starter', link: '/raf-framework/examples/raf-example-rocketmq-starter' },
      { name: 'kafka-starter', link: '/raf-framework/examples/raf-example-kafka-starter' },
    ],
  },
  {
    group: '微服务',
    icon: `<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="10" cy="10" r="3"/><circle cx="3" cy="4" r="1.5"/><circle cx="17" cy="4" r="1.5"/><circle cx="3" cy="16" r="1.5"/><circle cx="17" cy="16" r="1.5"/><path d="M4.5 5l4 3.5M15.5 5l-4 3.5M4.5 15l4-3.5M15.5 15l-4-3.5"/></svg>`,
    items: [
      { name: 'nacos-starter', link: '/raf-framework/examples/raf-example-nacos-starter' },
      { name: 'dubbo-starter', link: '/raf-framework/examples/raf-example-dubbo-starter' },
      { name: 'gateway-starter', link: '/raf-framework/examples/raf-example-gateway-starter' },
      { name: 'okhttp-starter', link: '/raf-framework/examples/raf-example-okhttp-starter' },
    ],
  },
  {
    group: '安全',
    icon: `<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 2l7 3v5c0 4-3 7-7 8-4-1-7-4-7-8V5l7-3z"/></svg>`,
    items: [
      { name: 'kms-starter', link: '/raf-framework/examples/raf-example-kms-starter' },
    ],
  },
  {
    group: '可观测性',
    icon: `<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M2 14l4-5 3 3 4-6 5 4"/></svg>`,
    items: [
      { name: 'monitor-starter', link: '/raf-framework/examples/raf-example-monitor-starter' },
      { name: 'sentry-starter', link: '/raf-framework/examples/raf-example-sentry-starter' },
    ],
  },
  {
    group: 'AI 集成',
    icon: `<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="10" cy="10" r="3"/><path d="M10 2v2M10 16v2M2 10h2M16 10h2M4.22 4.22l1.42 1.42M14.36 14.36l1.42 1.42M4.22 15.78l1.42-1.42M14.36 5.64l1.42-1.42"/></svg>`,
    items: [
      { name: 'ai-starter', link: '/raf-framework/examples/raf-framework-ai-starter' },
    ],
  },
]
</script>

<style scoped>
/* ── Feature Cards ─────────────────────────────────────────────────────────── */
.raf-features {
  max-width: 1152px;
  margin: 0 auto;
  padding: 0 32px 0;
}

.raf-features__header {
  text-align: left;
  padding: 48px 0 36px;
}

.raf-features__title {
  font-family: 'Sora', sans-serif;
  font-size: clamp(22px, 2.8vw, 30px);
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--raf-text-primary);
  margin: 12px 0 14px;
}

.raf-features__sub {
  font-family: 'Sora', sans-serif;
  font-size: 14px;
  line-height: 1.7;
  color: var(--raf-text-secondary);
  max-width: 480px;
}

.raf-features__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1px;
  background: var(--raf-border);
  border: 1px solid var(--raf-border);
}

@media (max-width: 768px) {
  .raf-features__grid { grid-template-columns: 1fr; }
}

.raf-card {
  position: relative;
  background: var(--raf-surface);
  padding: 52px 48px 44px;
  overflow: hidden;
  transition: background 0.25s ease;
  animation: fadeUp 0.55s ease both;
  animation-delay: var(--delay);
}

.raf-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 47px, var(--raf-grid) 47px, var(--raf-grid) 48px),
    repeating-linear-gradient(90deg, transparent, transparent 47px, var(--raf-grid) 47px, var(--raf-grid) 48px);
  opacity: 0.35;
  pointer-events: none;
}

.raf-card::after {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 2px;
  background: var(--raf-accent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.raf-card:hover { background: var(--raf-surface-hover); }
.raf-card:hover::after { transform: scaleX(1); }

.raf-card__index {
  position: absolute;
  top: 24px; right: 28px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px; font-weight: 600; letter-spacing: 0.14em;
  color: var(--raf-accent);
  opacity: 0.5;
}

.raf-card__icon {
  width: 40px; height: 40px;
  color: var(--raf-accent);
  margin-bottom: 28px;
  position: relative; z-index: 1;
}
.raf-card__icon svg { width: 100%; height: 100%; }

.raf-card__title {
  font-family: 'Sora', sans-serif;
  font-size: 19px; font-weight: 700; letter-spacing: -0.02em;
  color: var(--raf-text-primary);
  margin: 0 0 14px;
  position: relative; z-index: 1;
}

.raf-card__details {
  font-family: 'Sora', sans-serif;
  font-size: 14px; line-height: 1.8;
  color: var(--raf-text-secondary);
  margin: 0 0 28px;
  position: relative; z-index: 1;
}

.raf-card__tag {
  display: inline-block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 10.5px; font-weight: 500; letter-spacing: 0.1em;
  color: var(--raf-accent);
  background: var(--raf-accent-bg);
  border: 1px solid var(--raf-accent-border);
  padding: 4px 12px;
  position: relative; z-index: 1;
}

/* ── Stats ─────────────────────────────────────────────────────────────────── */
.raf-stats {
  max-width: 1152px;
  margin: 0 auto;
  padding: 0 32px 80px;
}

.raf-stats__wrap {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  border: 1px solid var(--raf-border);
}

@media (max-width: 960px) { .raf-stats__wrap { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 560px) { .raf-stats__wrap { grid-template-columns: repeat(2, 1fr); } }

.raf-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  border-right: 1px solid var(--raf-border);
  gap: 8px;
  transition: background 0.2s ease;
}
.raf-stat:last-child { border-right: none; }
.raf-stat:hover { background: var(--raf-surface-hover); }

.raf-stat__value {
  font-family: 'JetBrains Mono', monospace;
  font-size: 22px; font-weight: 700; letter-spacing: -0.02em;
  color: var(--raf-accent);
  white-space: nowrap;
}

.raf-stat__label {
  font-family: 'Sora', sans-serif;
  font-size: 11px; font-weight: 500; letter-spacing: 0.06em;
  color: var(--raf-text-secondary);
  text-transform: uppercase;
  white-space: nowrap;
}

/* ── Shared section wrapper ────────────────────────────────────────────────── */
.raf-section {
  max-width: 1152px;
  margin: 0 auto;
  padding: 120px 32px 0;
}

.raf-section--catalog {
  padding-bottom: 120px;
}

.raf-section__wrap {
  display: flex;
  flex-direction: column;
  gap: 64px;
}

.raf-section__header {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 560px;
}

.raf-eyebrow {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10.5px; font-weight: 600; letter-spacing: 0.2em;
  color: var(--raf-accent);
  opacity: 0.8;
}

.raf-section__title {
  font-family: 'Sora', sans-serif;
  font-size: 32px; font-weight: 700; letter-spacing: -0.03em;
  color: var(--raf-text-primary);
  margin: 0;
  line-height: 1.2;
}

.raf-section__sub {
  font-family: 'Sora', sans-serif;
  font-size: 15px; line-height: 1.75;
  color: var(--raf-text-secondary);
  margin: 0;
}

.raf-section__sub code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  color: var(--raf-accent);
  background: var(--raf-accent-bg);
  border: 1px solid var(--raf-accent-border);
  padding: 1px 6px;
}

/* ── Architecture Layers ───────────────────────────────────────────────────── */
.raf-layers {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: var(--raf-border);
  border: 1px solid var(--raf-border);
}

@media (max-width: 900px) { .raf-layers { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .raf-layers { grid-template-columns: 1fr; } }

.raf-layer {
  position: relative;
  background: var(--raf-surface);
  padding: 36px 28px 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: background 0.2s ease;
  animation: fadeUp 0.5s ease both;
  animation-delay: calc(var(--i) * 80ms);
}
.raf-layer:hover { background: var(--raf-surface-hover); }

.raf-layer__badge {
  display: inline-block;
  align-self: flex-start;
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px; font-weight: 700; letter-spacing: 0.14em;
  color: var(--raf-accent);
  background: var(--raf-accent-bg);
  border: 1px solid var(--raf-accent-border);
  padding: 4px 10px;
}

.raf-layer__name {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12.5px; font-weight: 600;
  color: var(--raf-text-primary);
  line-height: 1.5;
}

.raf-layer__desc {
  font-family: 'Sora', sans-serif;
  font-size: 13px; line-height: 1.7;
  color: var(--raf-text-secondary);
}

.raf-layer__arrow {
  display: none;
}

.raf-layer__connector { display: none; }

/* ── Starter Catalog ───────────────────────────────────────────────────────── */
.raf-catalog {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  background: var(--raf-border);
  border: 1px solid var(--raf-border);
}

@media (max-width: 900px) { .raf-catalog { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .raf-catalog { grid-template-columns: 1fr; } }

.raf-catalog__group {
  background: var(--raf-surface);
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.raf-catalog__group-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.raf-catalog__group-icon {
  width: 18px; height: 18px;
  color: var(--raf-accent);
  opacity: 0.7;
  flex-shrink: 0;
}
.raf-catalog__group-icon svg { width: 100%; height: 100%; }

.raf-catalog__group-name {
  font-family: 'Sora', sans-serif;
  font-size: 13px; font-weight: 600; letter-spacing: 0.01em;
  color: var(--raf-text-primary);
}

.raf-catalog__items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.raf-catalog__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px; font-weight: 500;
  color: var(--raf-text-secondary);
  text-decoration: none;
  padding: 9px 12px;
  border: 1px solid transparent;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.raf-catalog__item:hover {
  color: var(--raf-accent);
  border-color: var(--raf-accent-border);
  background: var(--raf-accent-bg);
}

.raf-catalog__item-arrow {
  font-size: 13px;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.raf-catalog__item:hover .raf-catalog__item-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* ── Animation ─────────────────────────────────────────────────────────────── */
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Design Philosophy ─────────────────────────────────────────────────────── */
.raf-philosophy {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  background: var(--raf-border);
  border: 1px solid var(--raf-border);
}

@media (max-width: 900px) {
  .raf-philosophy { grid-template-columns: 1fr; }
}

.raf-phi {
  background: var(--raf-surface);
  padding: 40px 36px 36px;
  position: relative;
  transition: background 0.2s ease;
}
.raf-phi:hover { background: var(--raf-surface-hover); }

.raf-phi::after {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: var(--raf-accent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.raf-phi:hover::after { transform: scaleX(1); }

.raf-phi__num {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px; font-weight: 700; letter-spacing: 0.14em;
  color: var(--raf-accent);
  opacity: 0.6;
  margin-bottom: 20px;
}

.raf-phi__title {
  font-family: 'Sora', sans-serif;
  font-size: 16px; font-weight: 700; letter-spacing: -0.01em;
  color: var(--raf-text-primary);
  margin-bottom: 14px;
  line-height: 1.4;
}

.raf-phi__desc {
  font-family: 'Sora', sans-serif;
  font-size: 13.5px; line-height: 1.8;
  color: var(--raf-text-secondary);
  margin-bottom: 24px;
}

.raf-phi__code {
  display: block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px; font-weight: 500;
  color: var(--raf-accent);
  background: var(--raf-accent-bg);
  border: 1px solid var(--raf-accent-border);
  padding: 8px 14px;
  letter-spacing: 0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
