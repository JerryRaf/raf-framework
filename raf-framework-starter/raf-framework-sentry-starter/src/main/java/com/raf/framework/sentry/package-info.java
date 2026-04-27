/**
 * Sentry 错误追踪集成
 * <p>
 * 提供企业级 Sentry 集成，支持：
 * <ul>
 *     <li>多种 TraceId 来源（框架自定义 / APM 平台）</li>
 *     <li>智能异常过滤（BusinessException / SystemException / InfrastructureException）</li>
 *     <li>性能监控（可选）</li>
 *     <li>灵活的采样率配置</li>
 *     <li>与 APM 平台（SkyWalking、Zipkin、Jaeger、Elastic APM 等）无缝集成</li>
 * </ul>
 *
 * <h2>快速开始</h2>
 * <pre>{@code
 * # application.yml
 * raf:
 *   sentry:
 *     enabled: true
 *     dsn: https://your-dsn@sentry.io/project-id
 *     trace-id-source: framework  # 或 apm
 *     sample-rate: 1.0
 *     traces-sample-rate: 0.1
 *     enable-performance: false
 *     exception-filter:
 *       enabled: true
 *       report-business-exception: false
 * }</pre>
 *
 * <h2>TraceId 集成</h2>
 * <p>支持两种 TraceId 来源：</p>
 * <ul>
 *     <li><b>framework</b>: 使用框架的 {@code ContextHolder.getTraceId()}</li>
 *     <li><b>apm</b>: 使用 APM 平台的 traceId（自动检测 SkyWalking、Zipkin、Jaeger、Elastic APM、Datadog）</li>
 * </ul>
 *
 * <h2>异常过滤</h2>
 * <p>默认只上报 {@code SystemException} 和 {@code InfrastructureException}，
 * 不上报 {@code BusinessException}（业务异常通常不需要告警）。</p>
 *
 * <h2>性能监控</h2>
 * <p>通过 {@code enable-performance: true} 启用性能追踪，
 * 使用 {@code traces-sample-rate} 控制采样率（默认 10%）。</p>
 *
 * @author Jerry
 * @date 2026/04/20
 * @see com.raf.framework.sentry.SentryAutoConfiguration
 * @see com.raf.framework.sentry.SentryProperties
 * @see com.raf.framework.sentry.trace.TraceIdProvider
 */
package com.raf.framework.sentry;
