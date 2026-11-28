import { fetchClient } from './fetchClient';

export interface Category {
    id: number;
    name: string;
}

export interface Product {
    id: number;
    name: string;
    price: number;
    description?: string;
    stock?: number;
    imageUrl?: string;
    image_url?: string;
    videoUrl?: string;
    video_url?: string;
    discount?: string;
    category?: Category;
    [key: string]: any;
}

export interface PagedProductQuery {
    page?: number;
    size?: number;
    categoryId?: number | null;
    keyword?: string;
    minPrice?: number | null;
    maxPrice?: number | null;
    sort?: 'newest' | 'price-asc' | 'price-desc' | 'name-asc';
}

export interface PagedProductResponse {
    content: Product[];
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

const EVENT_NAME = 'ipworld_products_updated';
const STORAGE_SYNC_KEY = 'ipworld_products_sync_ts';
const LEGACY_CACHE_KEY = 'ipworld_products_cache';
const CACHE_TTL_MS = 30_000;

export const INITIAL_CATALOG: Product[] = [];

// In-memory TTL cache thay vì lưu toàn bộ catalog vào localStorage (giới hạn ~5MB)
let memoryProductsCache: { data: Product[]; expiresAt: number } | null = null;

if (typeof window !== 'undefined') {
    try {
        // Dọn dẹp key cache cũ để giải phóng localStorage
        localStorage.removeItem(LEGACY_CACHE_KEY);
    } catch {}
}

export const broadcastProductChange = (action: 'create' | 'update' | 'delete', data?: any) => {
    memoryProductsCache = null;
    if (typeof window === 'undefined') return;
    try {
        window.dispatchEvent(new CustomEvent(EVENT_NAME, { detail: { action, data, timestamp: Date.now() } }));
        // Chỉ ghi timestamp rất nhẹ để thông báo cross-tab thay vì serialize cả catalog
        localStorage.setItem(STORAGE_SYNC_KEY, String(Date.now()));
    } catch (e) {
        console.error('[productService] Lỗi broadcast:', e);
    }
};

function paginateAndFilterClientFallback(all: Product[], query: PagedProductQuery): PagedProductResponse {
    const page = Math.max(0, query.page ?? 0);
    const size = Math.max(1, query.size ?? 12);
    const keyword = query.keyword?.trim().toLowerCase() || '';

    let filtered = all.filter(p => {
        if (query.categoryId != null && p.category?.id !== query.categoryId) return false;
        if (keyword && !p.name?.toLowerCase().includes(keyword)) return false;
        const price = p.price || 0;
        if (query.minPrice != null && price < query.minPrice) return false;
        if (query.maxPrice != null && price > query.maxPrice) return false;
        return true;
    });

    filtered = [...filtered].sort((a, b) => {
        if (query.sort === 'price-asc') return (a.price || 0) - (b.price || 0);
        if (query.sort === 'price-desc') return (b.price || 0) - (a.price || 0);
        if (query.sort === 'name-asc') return (a.name || '').localeCompare(b.name || '');
        return (b.id || 0) - (a.id || 0);
    });

    const totalElements = filtered.length;
    const totalPages = Math.max(1, Math.ceil(totalElements / size));
    const start = page * size;
    const content = filtered.slice(start, start + size);

    return {
        content,
        number: page,
        size,
        totalElements,
        totalPages,
        first: page === 0,
        last: page >= totalPages - 1,
    };
}

export const productService = {
    async getPagedProducts(query: PagedProductQuery = {}): Promise<PagedProductResponse> {
        const params = new URLSearchParams();
        params.set('page', String(Math.max(0, query.page ?? 0)));
        params.set('size', String(Math.max(1, query.size ?? 12)));
        if (query.categoryId != null) params.set('categoryId', String(query.categoryId));
        if (query.keyword && query.keyword.trim()) params.set('keyword', query.keyword.trim());
        if (query.minPrice != null) params.set('minPrice', String(query.minPrice));
        if (query.maxPrice != null) params.set('maxPrice', String(query.maxPrice));
        if (query.sort) params.set('sort', query.sort);

        try {
            const res = await fetchClient(`/products/paged?${params.toString()}`);
            if (res && Array.isArray(res.content)) {
                return {
                    content: res.content,
                    number: res.number ?? (query.page ?? 0),
                    size: res.size ?? (query.size ?? 12),
                    totalElements: res.totalElements ?? res.content.length,
                    totalPages: Math.max(1, res.totalPages ?? 1),
                    first: Boolean(res.first ?? (query.page ?? 0) === 0),
                    last: Boolean(res.last ?? true),
                };
            }
        } catch (err: any) {
            console.warn('[productService] Endpoint /products/paged chưa sẵn sàng, fallback sang /products:', err.message);
        }

        const all = await this.getAllProducts();
        return paginateAndFilterClientFallback(all, query);
    },

    async getAllProducts(forceRefresh = false): Promise<Product[]> {
        if (!forceRefresh && memoryProductsCache && memoryProductsCache.expiresAt > Date.now()) {
            return memoryProductsCache.data;
        }

        try {
            const data = await fetchClient('/products');
            if (Array.isArray(data)) {
                memoryProductsCache = { data, expiresAt: Date.now() + CACHE_TTL_MS };
                return data;
            }
        } catch (err: any) {
            console.warn('[productService] Không thể kết nối backend để tải sản phẩm:', err.message);
        }

        return memoryProductsCache?.data ?? [];
    },

    async getProductsByCategory(categoryNameOrSlug: string, aliases: string[] = []): Promise<Product[]> {
        const all = await this.getAllProducts();
        const searchTerms = [categoryNameOrSlug, ...aliases].map(s => s.toLowerCase().trim());

        return all.filter(p => {
            const catName = p.category?.name?.toLowerCase().trim() || '';
            const prodName = p.name?.toLowerCase().trim() || '';
            const desc = p.description?.toLowerCase().trim() || '';

            if (catName && searchTerms.some(term => catName.includes(term) || term.includes(catName))) {
                return true;
            }

            const slug = categoryNameOrSlug.toLowerCase();
            if (slug.includes('dien-thoai') || slug.includes('phone')) {
                return prodName.includes('iphone') || prodName.includes('điện thoại') || prodName.includes('galaxy') || desc.includes('smartphone');
            }
            if (slug.includes('laptop')) {
                return prodName.includes('macbook') || prodName.includes('laptop') || prodName.includes('rog') || prodName.includes('nitro') || prodName.includes('thinkpad');
            }
            if (slug.includes('pc')) {
                return prodName.includes('pc') || prodName.includes('case') || prodName.includes('desktop');
            }
            if (slug.includes('man-hinh')) {
                return prodName.includes('màn hình') || prodName.includes('monitor') || prodName.includes('ultrasharp');
            }
            if (slug.includes('may-tinh-bang')) {
                return prodName.includes('ipad') || prodName.includes('máy tính bảng') || prodName.includes('tab');
            }
            if (slug.includes('phu-kien')) {
                return prodName.includes('tai nghe') || prodName.includes('chuột') || prodName.includes('airpods') || prodName.includes('bàn phím') || prodName.includes('sạc') || prodName.includes('loa') || prodName.includes('đồng hồ') || prodName.includes('watch');
            }

            return false;
        });
    },

    async deleteProduct(id: number, name?: string): Promise<boolean> {
        await fetchClient(`/products/${id}`, { method: 'DELETE' });
        broadcastProductChange('delete', { id, name });
        return true;
    },

    async createProduct(productData: any): Promise<Product> {
        const payload: any = {
            name: productData.name,
            price: Number(productData.price) || 0,
            stock: productData.stock !== undefined ? Number(productData.stock) : 10,
            description: productData.description || '',
            imageUrl: productData.imageUrl || '/HINH_TRANG_CHU/logo2.jpg',
            videoUrl: productData.videoUrl || null,
        };

        if (productData.category && productData.category.id) {
            payload.category = {
                id: Number(productData.category.id),
                name: productData.category.name || '',
            };
        }

        const savedProduct = await fetchClient('/products', {
            method: 'POST',
            body: JSON.stringify(payload),
        });

        if (!savedProduct || !savedProduct.id) {
            throw new Error('Lỗi: Máy chủ không trả về thông tin sản phẩm sau khi lưu vào database.');
        }

        broadcastProductChange('create', savedProduct);
        return savedProduct;
    },

    async updateProduct(id: number, productData: any): Promise<Product> {
        const payload: any = {
            ...productData,
            price: Number(productData.price) || 0,
            stock: productData.stock !== undefined ? Number(productData.stock) : 10,
        };

        const updated = await fetchClient(`/products/${id}`, {
            method: 'PUT',
            body: JSON.stringify(payload),
        });

        if (!updated || !updated.id) {
            throw new Error('Lỗi: Không thể cập nhật sản phẩm trong database.');
        }

        broadcastProductChange('update', updated);
        return updated;
    },

    subscribe(callback: () => void): () => void {
        if (typeof window === 'undefined') return () => {};

        const handleCustom = () => {
            memoryProductsCache = null;
            callback();
        };

        const handleStorage = (event: StorageEvent) => {
            if (event.key === STORAGE_SYNC_KEY) {
                memoryProductsCache = null;
                callback();
            }
        };

        window.addEventListener(EVENT_NAME, handleCustom);
        window.addEventListener('storage', handleStorage);

        return () => {
            window.removeEventListener(EVENT_NAME, handleCustom);
            window.removeEventListener('storage', handleStorage);
        };
    },
};
