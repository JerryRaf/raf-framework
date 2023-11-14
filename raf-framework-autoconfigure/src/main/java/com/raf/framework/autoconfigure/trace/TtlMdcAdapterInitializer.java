package com.raf.framework.autoconfigure.trace;

import org.slf4j.TtlMdcAdapter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 加载TtlMDCAdapter实例
 *
 * @author Jerry
 * @date 2019/01/01 12:00
 */
public class TtlMdcAdapterInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TtlMdcAdapter.getInstance();
    }
}