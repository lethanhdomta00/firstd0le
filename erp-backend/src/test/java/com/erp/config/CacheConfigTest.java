package com.erp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Kiểm tra CacheManager khởi tạo dạng Caffeine và chứa đầy đủ cache names")
    void testCacheManagerConfiguration() {
        assertNotNull(cacheManager, "CacheManager không được null");
        assertTrue(cacheManager instanceof CaffeineCacheManager, "CacheManager phải là CaffeineCacheManager");

        assertTrue(cacheManager.getCacheNames().contains("products"), "Phải chứa cache 'products'");
        assertTrue(cacheManager.getCacheNames().contains("active_vouchers"), "Phải chứa cache 'active_vouchers'");
        assertTrue(cacheManager.getCacheNames().contains("tiered_pricing"), "Phải chứa cache 'tiered_pricing'");
        assertTrue(cacheManager.getCacheNames().contains("role_permissions"), "Phải chứa cache 'role_permissions'");
        assertTrue(cacheManager.getCacheNames().contains("treasury_stats"), "Phải chứa cache 'treasury_stats'");
    }

    @Test
    @DisplayName("Kiểm tra ghi và đọc dữ liệu từ Caffeine Cache")
    void testCachePutAndGet() {
        Cache cache = cacheManager.getCache("products");
        assertNotNull(cache, "Cache 'products' phải tồn tại");

        String key = "test_product_123";
        String value = "iPhone 15 Pro Max";

        cache.put(key, value);

        Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper, "Dữ liệu cache không được null sau khi put");
        assertEquals(value, wrapper.get());

        cache.evict(key);
        assertNull(cache.get(key), "Dữ liệu phải bị xóa sau khi evict");
    }
}
