package com.raf.framework.core.util;

import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class BeanRegistrationUtils {

    /**
     * 私有构造器,防止实例化工具类
     */
    private BeanRegistrationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 动态注册带有 Qualifier 的 Bean
     *
     * @param factory       Bean 工厂
     * @param beanName      Bean 名称
     * @param beanClass     Bean 类型
     * @param qualifierType Qualifier 注解类型
     * @param qualifierAttr Qualifier 属性键值对
     * @param supplier      Bean 实例的 Supplier
     */
    public static <T> void registerBeanWithQualifier(
            DefaultListableBeanFactory factory,
            String beanName,
            Class<T> beanClass,
            Class<?> qualifierType,
            Map<String, Object> qualifierAttr,
            Supplier<T> supplier) {

        // 创建 Bean 定义
        RootBeanDefinition definition = new RootBeanDefinition();
        definition.setBeanClass(beanClass);
        definition.setInstanceSupplier(supplier);

        // 添加 Qualifier
        definition.addQualifier(new AutowireCandidateQualifier(qualifierType, qualifierAttr));

        // 注册 Bean 定义和单例实例
        factory.registerBeanDefinition(beanName, definition);
        factory.registerSingleton(beanName, supplier.get());
    }
}