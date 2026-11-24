package com.erp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Hệ thống Bộ nhớ đệm Đa Tầng (High-Performance In-Memory Cache)
 * Sử dụng Caffeine Cache tối ưu cho JVM - Tốc độ truy xuất nanosecond, Zero-serialization overhead.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_ACTIVE_VOUCHERS = "active_vouchers";
    public static final String CACHE_TIERED_PRICING = "tiered_pricing";
    public static final String CACHE_ROLE_PERMISSIONS = "role_permissions";
    public static final String CACHE_TREASURY_STATS = "treasury_stats";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(2000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());

        cacheManager.setCacheNames(List.of(
                CACHE_PRODUCTS,
                CACHE_ACTIVE_VOUCHERS,
                CACHE_TIERED_PRICING,
                CACHE_ROLE_PERMISSIONS,
                CACHE_TREASURY_STATS
        ));

        return cacheManager;
    }
}
