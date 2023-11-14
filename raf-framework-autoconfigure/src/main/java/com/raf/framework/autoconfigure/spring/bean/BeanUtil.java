package com.raf.framework.autoconfigure.spring.bean;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author Jerry
 * @date 2021/09/23
 */
public class BeanUtil {
    public static void register(ConfigurableListableBeanFactory beanFactory, Object bean, String name, String alias) {
        beanFactory.registerSingleton(name, bean);
        if (!beanFactory.containsSingleton(alias)) {
            beanFactory.registerAlias(name, alias);
        }
    }
}
