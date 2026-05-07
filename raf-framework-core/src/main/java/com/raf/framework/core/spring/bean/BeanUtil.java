package com.raf.framework.core.spring.bean;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author Jerry
 * @date 2021/09/23
 */
public class BeanUtil {

    /**
     * 私有构造器，防止实例化工具类
     */
    private BeanUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void register(ConfigurableListableBeanFactory beanFactory, Object bean, String name, String alias) {
        beanFactory.registerSingleton(name, bean);
        if (!beanFactory.containsSingleton(alias)) {
            beanFactory.registerAlias(name, alias);
        }
    }
}
