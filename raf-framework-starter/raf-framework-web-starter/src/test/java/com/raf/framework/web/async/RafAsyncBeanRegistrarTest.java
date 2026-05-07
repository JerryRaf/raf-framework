package com.raf.framework.web.async;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

/**
 * Tests for RafAsyncBeanRegistrar.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class RafAsyncBeanRegistrarTest {

    @Test
    void registerBeanDefinitions_withEnabledAndPools_registersBeans() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("raf.async.enabled", "true");
        env.setProperty("raf.async.pools.order-pool.core-size", "5");
        env.setProperty("raf.async.pools.order-pool.max-size", "20");
        env.setProperty("raf.async.pools.order-pool.queue-capacity", "50");

        RafAsyncBeanRegistrar registrar = new RafAsyncBeanRegistrar();
        registrar.setEnvironment(env);

        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(RafAsyncConfig.class), registry);

        Assertions.assertTrue(registry.containsBeanDefinition("order-pool"));
    }

    @Test
    void registerBeanDefinitions_withDisabled_registersNoBeans() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("raf.async.enabled", "false");
        env.setProperty("raf.async.pools.order-pool.core-size", "5");

        RafAsyncBeanRegistrar registrar = new RafAsyncBeanRegistrar();
        registrar.setEnvironment(env);

        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(RafAsyncConfig.class), registry);

        Assertions.assertFalse(registry.containsBeanDefinition("order-pool"));
    }

    @Test
    void registerBeanDefinitions_withNoPools_registersNoBeans() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("raf.async.enabled", "true");
        // No pools configured

        RafAsyncBeanRegistrar registrar = new RafAsyncBeanRegistrar();
        registrar.setEnvironment(env);

        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(RafAsyncConfig.class), registry);

        Assertions.assertEquals(0, registry.getBeanDefinitionCount());
    }

    @Test
    void registerBeanDefinitions_withNotBound_registersNoBeans() {
        MockEnvironment env = new MockEnvironment();
        // No raf.async properties at all

        RafAsyncBeanRegistrar registrar = new RafAsyncBeanRegistrar();
        registrar.setEnvironment(env);

        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(RafAsyncConfig.class), registry);

        Assertions.assertEquals(0, registry.getBeanDefinitionCount());
    }

    @Test
    void registerBeanDefinitions_withMultiplePools_registersAllBeans() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("raf.async.enabled", "true");
        env.setProperty("raf.async.pools.pool-a.core-size", "2");
        env.setProperty("raf.async.pools.pool-b.core-size", "4");

        RafAsyncBeanRegistrar registrar = new RafAsyncBeanRegistrar();
        registrar.setEnvironment(env);

        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(RafAsyncConfig.class), registry);

        Assertions.assertTrue(registry.containsBeanDefinition("pool-a"));
        Assertions.assertTrue(registry.containsBeanDefinition("pool-b"));
    }
}
