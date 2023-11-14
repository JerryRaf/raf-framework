package com.raf.framework.autoconfigure.servlet.shutdown;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
public class TomcatGracefulShutdown implements TomcatConnectorCustomizer,ApplicationListener<ContextClosedEvent> {

    private volatile Connector connector;

    private final TomcatGracefulShutdownProperties tomcatGracefulShutdownProperties;

    public TomcatGracefulShutdown(TomcatGracefulShutdownProperties tomcatGracefulShutdownProperties) {
        this.tomcatGracefulShutdownProperties = tomcatGracefulShutdownProperties;
    }

    @Override
    public void customize(Connector connector) {
        this.connector = connector;
    }

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        if (connector == null) {
            log.info("We are running unit test..");
            return;
        }
        final Executor executor = connector.getProtocolHandler().getExecutor();
        if (executor instanceof ThreadPoolExecutor) {
            log.info("executor is ThreadPoolExecutor");
            final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
            if (threadPoolExecutor.isTerminated()) {
                log.info("thread pool executor has terminated");
            } else {
                LocalDateTime startShutdown = LocalDateTime.now();
                LocalDateTime stopShutdown = LocalDateTime.now();

                try {
                    threadPoolExecutor.shutdown();
                    if (!threadPoolExecutor.awaitTermination(tomcatGracefulShutdownProperties.getWaitTime(), TimeUnit.SECONDS)) {
                        log.warn("Tomcat thread pool did not shutdown gracefully within {}s. Proceeding with force shutdown",tomcatGracefulShutdownProperties.getWaitTime());
                        threadPoolExecutor.shutdownNow();
                    } else {
                        log.info("Tomcat thread pool is empty, we stop now");
                    }
                    stopShutdown = LocalDateTime.now();
                } catch (final InterruptedException ex) {
                    log.error("优雅关机失败", ex);
                    Thread.currentThread().interrupt();
                } finally {
                    final long seconds = Duration.between(startShutdown, stopShutdown).getSeconds();
                    log.info("Shutdown performed in {}s",seconds);
                }
            }
        }
    }
}