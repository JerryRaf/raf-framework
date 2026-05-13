package com.raf.framework.core.common.result;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 错误码冲突检测配置属性。
 *
 * <p>示例配置：
 * <pre>
 * raf:
 *   error-code:
 *     conflict-check:
 *       enabled: true
 *       scan-packages: com.example.order,com.example.user
 * </pre>
 *
 * @author Jerry
 */
@Data
@ConfigurationProperties(prefix = "raf.error-code.conflict-check")
public class ErrorCodeConflictCheckProperties {

    /**
     * 是否启用错误码冲突检测，默认 true。
     * <p>建议生产环境保持开启，fail-fast 防止带冲突的服务启动。
     */
    private boolean enabled = true;

    /**
     * 扫描包路径，多个包用逗号分隔。
     * <p>默认为空，扫描所有包（性能较低，建议显式配置）。
     */
    private String scanPackages = "";
}
