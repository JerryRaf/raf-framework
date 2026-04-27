package com.raf.example.redis.service;

import com.raf.framework.autoconfigure.common.exception.BusinessException;
import com.raf.framework.autoconfigure.common.exception.InfrastructureException;
import com.raf.framework.autoconfigure.common.result.RafResponseEnum;
import com.raf.framework.autoconfigure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Product stock service - demonstrates Redis cache patterns and distributed lock.
 *
 * <p>Patterns demonstrated:
 * <ul>
 *   <li>Cache-aside pattern with null value protection (cache penetration prevention)</li>
 *   <li>Random TTL for cache avalanche prevention</li>
 *   <li>Redisson tryLock for distributed lock (deadlock-safe)</li>
 * </ul>
 *
 * @author Jerry
 * @since 2026-04-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_CACHE_KEY = "stock:product:";
    private static final String STOCK_LOCK_KEY = "lock:stock:deduct:";
    private static final int BASE_TTL_MINUTES = 30;
    private static final int TTL_JITTER_MINUTES = 10;

    private final RedisService redisService;
    private final RedissonClient redissonClient;

    /**
     * Get product stock with cache-aside pattern.
     * Demonstrates: cache penetration prevention (null value caching).
     *
     * @param productId product ID
     * @return current stock quantity, or null if product not found
     */
    public Integer getStock(Long productId) {
        String key = STOCK_CACHE_KEY + productId;
        Object cached = redisService.get(key);

        if (cached != null) {
            log.info("Stock cache hit: productId={}", productId);
            // Sentinel value -1 means product not found (cache penetration protection)
            Integer stock = (Integer) cached;
            return stock == -1 ? null : stock;
        }

        // Cache miss: load from "database" (simulated here)
        log.info("Stock cache miss, loading from DB: productId={}", productId);
        Integer stock = loadStockFromDb(productId);

        if (stock != null) {
            // Random TTL to prevent cache avalanche
            long ttl = BASE_TTL_MINUTES + ThreadLocalRandom.current().nextInt(TTL_JITTER_MINUTES);
            redisService.set(key, stock, ttl, TimeUnit.MINUTES);
            log.info("Stock cached: productId={}, stock={}, ttl={}min", productId, stock, ttl);
        } else {
            // Cache null value to prevent cache penetration
            redisService.set(key, -1, 5, TimeUnit.MINUTES);
            log.info("Null stock cached (penetration protection): productId={}", productId);
        }

        return stock;
    }

    /**
     * Deduct product stock with distributed lock.
     * Demonstrates: Redisson tryLock pattern (deadlock-safe).
     *
     * @param productId product ID
     * @param quantity  quantity to deduct
     * @throws BusinessException if stock is insufficient or product not found
     */
    public void deductStock(Long productId, int quantity) {
        String lockKey = STOCK_LOCK_KEY + productId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            // tryLock: wait up to 3s, auto-release after 10s (prevents deadlock)
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("Failed to acquire stock lock: productId={}", productId);
                throw new BusinessException(RafResponseEnum.SERVER_ERROR, "Stock operation is busy, please retry");
            }

            String key = STOCK_CACHE_KEY + productId;
            Object cached = redisService.get(key);
            if (cached == null) {
                throw new BusinessException(RafResponseEnum.NOT_FOUND, "Product not found: " + productId);
            }

            int currentStock = (Integer) cached;
            if (currentStock < quantity) {
                throw new BusinessException(RafResponseEnum.SERVER_ERROR,
                        "Insufficient stock: available=" + currentStock + ", requested=" + quantity);
            }

            int newStock = currentStock - quantity;
            redisService.set(key, newStock, BASE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Stock deducted: productId={}, deducted={}, remaining={}", productId, quantity, newStock);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InfrastructureException("Stock lock interrupted: productId=" + productId, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Initialize product stock (for demo purposes).
     *
     * @param productId product ID
     * @param stock     initial stock quantity
     */
    public void initStock(Long productId, int stock) {
        String key = STOCK_CACHE_KEY + productId;
        long ttl = BASE_TTL_MINUTES + ThreadLocalRandom.current().nextInt(TTL_JITTER_MINUTES);
        redisService.set(key, stock, ttl, TimeUnit.MINUTES);
        log.info("Stock initialized: productId={}, stock={}", productId, stock);
    }

    /**
     * Simulates loading stock from database.
     * In real applications, this would call a mapper/repository.
     */
    private Integer loadStockFromDb(Long productId) {
        // Simulate: products 1-100 exist with stock=100, others don't exist
        if (productId >= 1 && productId <= 100) {
            return 100;
        }
        return null;
    }
}
