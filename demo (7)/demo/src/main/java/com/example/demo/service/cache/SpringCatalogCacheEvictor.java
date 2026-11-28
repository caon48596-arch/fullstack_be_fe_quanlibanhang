package com.example.demo.service.cache;

import com.example.demo.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Triển khai xóa cache dựa trên Spring CacheManager (Redis / Caffeine).
 */
@Component
public class SpringCatalogCacheEvictor implements CatalogCacheEvictor {

    private final CacheManager cacheManager;

    public SpringCatalogCacheEvictor(@Autowired(required = false) CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void evictAfterOrderChange(Iterable<Long> affectedProductIds) {
        if (cacheManager == null) {
            return;
        }
        if (affectedProductIds != null) {
            Cache productDetailCache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAIL);
            if (productDetailCache != null) {
                for (Long productId : affectedProductIds) {
                    productDetailCache.evict(productId);
                }
            }
        }
        Cache byCatCache = cacheManager.getCache(CacheConfig.CACHE_PRODUCTS_BY_CATEGORY);
        if (byCatCache != null) {
            byCatCache.clear();
        }
        Cache dashboardCache = cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS);
        if (dashboardCache != null) {
            dashboardCache.clear();
        }
    }
}
