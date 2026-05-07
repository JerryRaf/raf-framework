package com.raf.framework.core.spring;

import org.springframework.beans.FatalBeanException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author Jerry
 * @date 2021/09/23
 */
public class ConfigUtil {

    /**
     * 私有构造器，防止实例化工具类
     */
    private ConfigUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 只支持静态变量
     *
     * @param prefix
     * @param clazz
     * @param propertySources
     * @param <T>
     * @return
     */
    public static <T> T resolveSettingForStatic(String prefix, Class<T> clazz, MutablePropertySources propertySources) {
        return new Binder(ConfigurationPropertySources.from(propertySources))
                .bind(prefix, Bindable.of(clazz))
                .orElseThrow(() -> new FatalBeanException(String.format("Could not bind [%s] properties", prefix)));
    }

    /**
     * 支持placeholder表达式
     *
     * @param prefix
     * @param clazz
     * @param environment
     * @param <T>
     * @return
     */
    public static <T> T resolveSetting(String prefix, Class<T> clazz, ConfigurableEnvironment environment) {
        return Binder.get(environment)
                .bind(prefix, Bindable.of(clazz))
                .orElseThrow(() -> new FatalBeanException(String.format("Could not bind [%s] properties", prefix)));
    }
}
