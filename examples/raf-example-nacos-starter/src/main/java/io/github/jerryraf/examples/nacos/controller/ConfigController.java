package io.github.jerryraf.examples.nacos.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.nacos.config.DynamicConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes dynamic config values for verification.
 *
 * <p>Test hot-reload: update app.title in Nacos console, then call GET /config/info again.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final DynamicConfig dynamicConfig;

    @GetMapping("/info")
    public RafResult<Map<String, String>> getConfigInfo() {
        return RafResult.success(Map.of(
                "title",   dynamicConfig.getTitle(),
                "version", dynamicConfig.getVersion()
        ));
    }
}
