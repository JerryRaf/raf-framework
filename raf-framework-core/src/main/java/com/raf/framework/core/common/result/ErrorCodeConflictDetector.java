package com.raf.framework.core.common.result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 错误码冲突检测器。
 *
 * <p>在应用启动完成后（{@link ApplicationStartedEvent}）扫描所有 {@link IResponseEnum} 枚举实现，
 * 检测 code 重复并 fail-fast，防止带冲突的服务启动。
 *
 * <p>通过 {@code raf.error-code.conflict-check.enabled=false} 可关闭检测（不推荐）。
 *
 * @author Jerry
 */
@Slf4j
@RequiredArgsConstructor
public class ErrorCodeConflictDetector implements ApplicationListener<ApplicationStartedEvent> {

    private final ErrorCodeConflictCheckProperties properties;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (!properties.isEnabled()) {
            log.debug("[ErrorCodeConflict] Conflict check is disabled.");
            return;
        }

        log.info("[ErrorCodeConflict] Scanning IResponseEnum implementations for code conflicts...");

        List<Class<? extends IResponseEnum>> enumClasses = scanEnumClasses();
        if (enumClasses.isEmpty()) {
            log.info("[ErrorCodeConflict] No IResponseEnum implementations found.");
            return;
        }

        Map<Integer, String> codeMap = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        for (Class<? extends IResponseEnum> clazz : enumClasses) {
            if (!clazz.isEnum()) {
                continue;
            }
            IResponseEnum[] constants = clazz.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (IResponseEnum constant : constants) {
                int code = constant.getCode();
                String fullName = clazz.getName() + "." + ((Enum<?>) constant).name();
                String existing = codeMap.put(code, fullName);
                if (existing != null) {
                    String conflict = String.format("code=%d: [%s] vs [%s]", code, existing, fullName);
                    conflicts.add(conflict);
                    log.error("[ErrorCodeConflict] {}", conflict);
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "[ErrorCodeConflict] Found " + conflicts.size() + " error code conflict(s). " +
                    "Fix the conflicts before starting the application:\n" +
                    String.join("\n", conflicts));
        }

        log.info("[ErrorCodeConflict] No conflicts found. Scanned {} enum(s), {} code(s).",
                enumClasses.size(), codeMap.size());
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends IResponseEnum>> scanEnumClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(IResponseEnum.class));

        List<Class<? extends IResponseEnum>> result = new ArrayList<>();
        String[] packages = resolvePackages();

        for (String pkg : packages) {
            try {
                Set<BeanDefinition> candidates = scanner.findCandidateComponents(pkg);
                for (BeanDefinition bd : candidates) {
                    String className = bd.getBeanClassName();
                    if (className == null) {
                        continue;
                    }
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isEnum() && IResponseEnum.class.isAssignableFrom(clazz)) {
                            result.add((Class<? extends IResponseEnum>) clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.warn("[ErrorCodeConflict] Cannot load class: {}", className, e);
                    }
                }
            } catch (Exception e) {
                log.warn("[ErrorCodeConflict] Error scanning package: {}", pkg, e);
            }
        }
        return result;
    }

    private String[] resolvePackages() {
        String scanPackages = properties.getScanPackages();
        if (StringUtils.hasText(scanPackages)) {
            return StringUtils.commaDelimitedListToStringArray(scanPackages);
        }
        // 默认扫描根包（性能较低，建议配置 scan-packages）
        return new String[]{""};
    }
}
