import type { Category, Product } from './productService';

const SERVER_API_URL = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';
const FALLBACK_API_URL = 'https://project1-production-6826.up.railway.app/api';

async function fetchJsonWithTimeout<T>(path: string, revalidateSeconds = 60): Promise<T | null> {
    const urls = SERVER_API_URL.includes('localhost') && SERVER_API_URL !== FALLBACK_API_URL
        ? [`${SERVER_API_URL}${path}`, `${FALLBACK_API_URL}${path}`]
        : [`${SERVER_API_URL}${path}`];

    for (const url of urls) {
        try {
            const res = await fetch(url, {
                next: { revalidate: revalidateSeconds },
                signal: AbortSignal.timeout(3000),
            });
            if (res.ok) {
                return (await res.json()) as T;
            }
        } catch {
            // Thử URL kế tiếp hoặc để client tự hydrate
        }
    }
    return null;
}

export interface InitialCatalogData {
    products: Product[];
    totalElements: number;
    totalPages: number;
    categories: Category[];
}

export async function fetchInitialHomeData(pageSize = 12): Promise<InitialCatalogData> {
    const [pagedRes, categoriesRes] = await Promise.all([
        fetchJsonWithTimeout<any>(`/products/paged?page=0&size=${pageSize}&sort=newest`, 60),
        fetchJsonWithTimeout<Category[]>('/categories', 120),
    ]);

    const categories = Array.isArray(categoriesRes) ? categoriesRes : [];

    if (pagedRes && Array.isArray(pagedRes.content)) {
        return {
            products: pagedRes.content,
            totalElements: pagedRes.totalElements ?? pagedRes.content.length,
            totalPages: Math.max(1, pagedRes.totalPages ?? 1),
            categories,
        };
    }

    // Fallback nếu server production chưa có endpoint /products/paged
    const allProducts = await fetchJsonWithTimeout<Product[]>('/products', 60);
    if (Array.isArray(allProducts)) {
        return {
            products: allProducts.slice(0, pageSize),
            totalElements: allProducts.length,
            totalPages: Math.max(1, Math.ceil(allProducts.length / pageSize)),
            categories,
        };
    }

    return {
        products: [],
        totalElements: 0,
        totalPages: 1,
        categories,
    };
}

export interface InitialCategoryData {
    categoryId: number | null;
    products: Product[];
    totalElements: number;
    totalPages: number;
}

export async function fetchInitialCategoryData(
    categoryName: string,
    aliases: string[] = [],
    pageSize = 12
): Promise<InitialCategoryData> {
    const categories = await fetchJsonWithTimeout<Category[]>('/categories', 120);
    const searchNames = [categoryName, ...aliases].map(s => s.toLowerCase().trim());

    let categoryId: number | null = null;
    if (Array.isArray(categories)) {
        const matched = categories.find(c => {
            const name = c.name?.toLowerCase().trim() || '';
            return searchNames.includes(name) || searchNames.some(sn => name.includes(sn));
        });
        if (matched?.id) {
            categoryId = matched.id;
        }
    }

    if (categoryId != null) {
        const paged = await fetchJsonWithTimeout<any>(
            `/products/paged?page=0&size=${pageSize}&categoryId=${categoryId}&sort=newest`,
            60
        );
        if (paged && Array.isArray(paged.content)) {
            return {
                categoryId,
                products: paged.content,
                totalElements: paged.totalElements ?? paged.content.length,
                totalPages: Math.max(1, paged.totalPages ?? 1),
            };
        }

        const byCat = await fetchJsonWithTimeout<Product[]>(`/products/category/${categoryId}`, 60);
        if (Array.isArray(byCat)) {
            return {
                categoryId,
                products: byCat.slice(0, pageSize),
                totalElements: byCat.length,
                totalPages: Math.max(1, Math.ceil(byCat.length / pageSize)),
            };
        }
    }

    return {
        categoryId,
        products: [],
        totalElements: 0,
        totalPages: 1,
    };
}
