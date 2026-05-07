package io.github.jerryraf.examples.redis.service;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Stock service demonstrating Redisson distributed lock for concurrent stock deduction.
 *
 * <p>Pattern: tryLock with timeout — avoids deadlock and limits wait time.
 * WatchDog automatically renews the lock TTL while the thread holds it.
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_KEY = "stock:product:";
    private static final String LOCK_KEY  = "lock:stock:product:";
    private static final long   STOCK_TTL_MINUTES = 60;

    private final RedisService   redisService;
    private final RedissonClient redissonClient;

    /**
     * Initialize stock for a product (for demo purposes).
     */
    public void initStock(Long productId, int stock) {
        redisService.set(STOCK_KEY + productId, stock, STOCK_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("Stock initialized: productId={}, stock={}", productId, stock);
    }

    /**
     * Get current stock.
     */
    public Integer getStock(Long productId) {
        Integer stock = redisService.get(STOCK_KEY + productId);
        log.info("Stock queried: productId={}, stock={}", productId, stock);
        return stock;
    }

    /**
     * Deduct stock with distributed lock.
     * Demonstrates: Redisson tryLock — wait 3s, auto-release after 10s (WatchDog renews if held).
     */
    public void deductStock(Long productId, int quantity) {
        RLock lock = redissonClient.getLock(LOCK_KEY + productId);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("Failed to acquire lock: productId={}", productId);
                throw new BusinessException(RafResponseEnum.SERVER_ERROR, "Stock operation busy, please retry");
            }

            Integer current = redisService.get(STOCK_KEY + productId);
            if (current == null) {
                throw new BusinessException(RafResponseEnum.NOT_FOUND, "Product not found: " + productId);
            }
            if (current < quantity) {
                throw new BusinessException(RafResponseEnum.SERVER_ERROR,
                        "Insufficient stock: available=" + current + ", requested=" + quantity);
            }

            redisService.set(STOCK_KEY + productId, current - quantity, STOCK_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Stock deducted: productId={}, deducted={}, remaining={}", productId, quantity, current - quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InfrastructureException("Lock interrupted: productId=" + productId, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
