package com.raf.framework.autoconfigure.spring;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Jerry
 * @date 2021/09/23
 */
@Slf4j
@Configuration
public class EnvConfig implements EnvironmentAware {

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    public String get(String key) {
        try {
            return ConfigUtil.resolveSetting(key, String.class, environment);
        } catch (Exception ex) {
            log.warn("The configuration was not found:{}", key);
            return null;
        }
    }
}
