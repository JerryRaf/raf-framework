package com.raf.framework.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

/**
 * @author Jerry
 * @date 2021/09/23
 */
@Slf4j
@Configuration
public class EnvConfig implements EnvironmentAware {

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
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
