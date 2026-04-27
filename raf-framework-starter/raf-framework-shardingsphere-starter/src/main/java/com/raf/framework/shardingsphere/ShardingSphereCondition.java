package com.raf.framework.shardingsphere;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * ShardingSphere 条件判断
 * <p>
 * 当 raf.shardingsphere.enabled=true 时，禁用框架的多数据源配置
 * </p>
 *
 * @author RAF Framework
 */
public class ShardingSphereCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("raf.shardingsphere.enabled");

        if ("true".equalsIgnoreCase(enabled)) {
            return ConditionOutcome.noMatch("ShardingSphere is enabled, skipping RAF DataSource configuration");
        }

        return ConditionOutcome.match("ShardingSphere is not enabled, using RAF DataSource configuration");
    }
}
