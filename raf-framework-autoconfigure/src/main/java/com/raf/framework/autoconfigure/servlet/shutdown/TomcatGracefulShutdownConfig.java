package com.raf.framework.autoconfigure.servlet.shutdown;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.startup.Tomcat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Servlet;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.tomcat.shutdown.enabled")
@EnableConfigurationProperties(TomcatGracefulShutdownProperties.class)
@ConditionalOnBean(TomcatServletWebServerFactory.class)
@ConditionalOnClass({Servlet.class, Tomcat.class})
public class TomcatGracefulShutdownConfig {

    @Autowired
    private TomcatGracefulShutdownProperties tomcatGracefulShutdownProperties;

    @Bean
    public TomcatGracefulShutdown tomcatGracefulShutdown() {
        log.info("-->tomcat graceful shutdown register success");
        return new TomcatGracefulShutdown(tomcatGracefulShutdownProperties);
    }

    @Bean
    public WebServerFactoryCustomizer tomcatFactoryCustomizer() {
        return server -> {
            if (server instanceof TomcatServletWebServerFactory) {
                ((TomcatServletWebServerFactory) server).addConnectorCustomizers(tomcatGracefulShutdown());
            }
        };
    }
}
