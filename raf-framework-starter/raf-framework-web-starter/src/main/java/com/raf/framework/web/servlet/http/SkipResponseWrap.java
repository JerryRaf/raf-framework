package com.raf.framework.web.servlet.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 跳过统一响应包装。
 *
 * <p>框架默认对所有 {@code @RestController} 的返回值自动包装为 {@link com.raf.framework.core.common.result.RafResult}。
 * 在以下场景需要跳过包装时，在类或方法上标注此注解：
 * <ul>
 *   <li>文件下载接口（返回 {@code ResponseEntity<Resource>}）</li>
 *   <li>第三方回调接口（要求返回特定格式，如微信支付回调）</li>
 *   <li>SSE / WebSocket 接口</li>
 *   <li>已手动构造 {@link com.raf.framework.core.common.result.RafResult} 的接口</li>
 * </ul>
 *
 * <p>标注在类上时，该类所有方法均跳过包装；标注在方法上时，仅该方法跳过。
 *
 * @author Jerry
 * @see ResponseResult
 */
@Retention(RUNTIME)
@Target({TYPE, ElementType.METHOD})
@Documented
public @interface SkipResponseWrap {
}
