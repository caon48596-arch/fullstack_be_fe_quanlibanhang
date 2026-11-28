package com.example.demo.service.cache;

/**
 * Abstraction xóa cache sản phẩm & thống kê Dashboard sau khi phát sinh/hủy đơn hàng (SRP & DIP).
 */
public interface CatalogCacheEvictor {

    void evictAfterOrderChange(Iterable<Long> affectedProductIds);
}
