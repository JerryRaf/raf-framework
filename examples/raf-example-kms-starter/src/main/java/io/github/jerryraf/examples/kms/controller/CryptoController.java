package io.github.jerryraf.examples.kms.controller;

import com.raf.framework.core.common.result.RafResult;
import com.raf.framework.kms.core.CryptoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * KMS 加解密示例 Controller。
 */
@Slf4j
@RestController
@RequestMapping("/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoManager cryptoManager;

    /**
     * 加密：GET /crypto/encrypt?text=hello&alias=aes-biz-key
     */
    @GetMapping("/encrypt")
    public RafResult<String> encrypt(@RequestParam String text,
                                     @RequestParam(defaultValue = "aes-biz-key") String alias) {
        String cipher = cryptoManager.encrypt(text, alias);
        log.info("Encrypted with alias [{}]", alias);
        return RafResult.success(cipher);
    }

    /**
     * 解密：GET /crypto/decrypt?cipher=xxx&alias=aes-biz-key
     * <p>
     * 注意：生产环境中密文不应通过 GET 参数传递（会出现在 URL 和访问日志中），
     * 此处仅为演示目的，生产请改用 POST + RequestBody。
     */
    @GetMapping("/decrypt")
    public RafResult<String> decrypt(@RequestParam String cipher,
                                     @RequestParam(defaultValue = "aes-biz-key") String alias) {
        String plain = cryptoManager.decrypt(cipher, alias);
        log.info("Decrypted with alias [{}]", alias);
        return RafResult.success(plain);
    }

    /**
     * 生成 HMAC 索引：GET /crypto/index?text=13800138000&alias=aes-biz-key
     */
    @GetMapping("/index")
    public RafResult<String> index(@RequestParam String text,
                                   @RequestParam(defaultValue = "aes-biz-key") String alias) {
        String idx = cryptoManager.generateIndex(text, alias);
        return RafResult.success(idx);
    }
}
