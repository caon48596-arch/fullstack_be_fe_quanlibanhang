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

const STORAGE_KEY_PRODUCTS = 'ipworld_products_cache';
const EVENT_NAME = 'ipworld_products_updated';
const LEGACY_FAKE_IDS = [12, 13, 14, 15, 16, 17, 18];

export const INITIAL_CATALOG: Product[] = [];

// Loại bỏ sản phẩm mẫu cũ (ID 12-18 hoặc >= 1000) còn sót trong localStorage
const isValidRealProduct = (p: Product) =>
    Boolean(p.id) && !LEGACY_FAKE_IDS.includes(p.id) && p.id < 1000;

const cleanLegacyFakeProducts = () => {
    if (typeof window === 'undefined') return;
    try {
        const raw = localStorage.getItem(STORAGE_KEY_PRODUCTS);
        if (raw) {
            const list: Product[] = JSON.parse(raw);
            if (Array.isArray(list)) {
                const cleaned = list.filter(isValidRealProduct);
                if (cleaned.length !== list.length) {
                    localStorage.setItem(STORAGE_KEY_PRODUCTS, JSON.stringify(cleaned));
                }
            }
        }
    } catch {}
};

if (typeof window !== 'undefined') {
    cleanLegacyFakeProducts();
}

export const broadcastProductChange = (action: 'create' | 'update' | 'delete', data?: any) => {
    if (typeof window === 'undefined') return;
    try {
        window.dispatchEvent(new CustomEvent(EVENT_NAME, { detail: { action, data, timestamp: Date.now() } }));
    } catch (e) {
        console.error('[productService] Lỗi broadcast:', e);
    }
};

export const productService = {
    async getAllProducts(): Promise<Product[]> {
        try {
            const data = await fetchClient('/products');
            if (Array.isArray(data)) {
                // Lưu cache cục bộ để fallback khi mất kết nối backend
                if (typeof window !== 'undefined') {
                    try {
                        localStorage.setItem(STORAGE_KEY_PRODUCTS, JSON.stringify(data));
                    } catch {}
                }
                return data;
            }
        } catch (err: any) {
            console.warn('[productService] Không thể kết nối backend để tải sản phẩm, thử đọc cache cục bộ:', err.message);
        }

        if (typeof window !== 'undefined') {
            try {
                const raw = localStorage.getItem(STORAGE_KEY_PRODUCTS);
                if (raw) {
                    const parsed = JSON.parse(raw);
                    if (Array.isArray(parsed)) {
                        return parsed.filter(isValidRealProduct);
                    }
                }
            } catch {}
        }

        return [];
    },

    async getProductsByCategory(categoryNameOrSlug: string, aliases: string[] = []): Promise<Product[]> {
        const all = await this.getAllProducts();
        const searchTerms = [categoryNameOrSlug, ...aliases].map(s => s.toLowerCase().trim());

        return all.filter(p => {
            const catName = p.category?.name?.toLowerCase().trim() || '';
            const prodName = p.name?.toLowerCase().trim() || '';
            const desc = p.description?.toLowerCase().trim() || '';

            if (searchTerms.some(term => catName.includes(term) || term.includes(catName))) {
                return true;
            }

            // Fallback khớp theo từ khóa nếu sản phẩm chưa được gắn danh mục chuẩn
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

        if (typeof window !== 'undefined') {
            try {
                const raw = localStorage.getItem(STORAGE_KEY_PRODUCTS);
                if (raw) {
                    const list: Product[] = JSON.parse(raw);
                    const updated = list.filter(p => p.id !== id);
                    localStorage.setItem(STORAGE_KEY_PRODUCTS, JSON.stringify(updated));
                }
            } catch {}
        }

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

        if (typeof window !== 'undefined') {
            try {
                const raw = localStorage.getItem(STORAGE_KEY_PRODUCTS);
                const current: Product[] = raw ? JSON.parse(raw) : [];
                const next = [savedProduct, ...current.filter(p => p.id !== savedProduct.id)];
                localStorage.setItem(STORAGE_KEY_PRODUCTS, JSON.stringify(next));
            } catch {}
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

        if (typeof window !== 'undefined') {
            try {
                const raw = localStorage.getItem(STORAGE_KEY_PRODUCTS);
                if (raw) {
                    const current: Product[] = JSON.parse(raw);
                    const next = current.map(p => (p.id === id ? { ...p, ...updated } : p));
                    localStorage.setItem(STORAGE_KEY_PRODUCTS, JSON.stringify(next));
                }
            } catch {}
        }

        broadcastProductChange('update', updated);
        return updated;
    },

    subscribe(callback: () => void): () => void {
        if (typeof window === 'undefined') return () => {};

        const handler = () => {
            callback();
        };

        window.addEventListener(EVENT_NAME, handler);
        window.addEventListener('storage', handler);

        return () => {
            window.removeEventListener(EVENT_NAME, handler);
            window.removeEventListener('storage', handler);
        };
    },
};
