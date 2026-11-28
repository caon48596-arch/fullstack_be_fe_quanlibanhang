"use client";

import React, { useEffect, useState, useCallback, useRef } from 'react';
import Link from 'next/link';
import { fetchClient, getUserRoleFromToken, getValidMediaUrl } from '@/util/fetchClient';
import { productService, Product } from '@/util/productService';
import { useCart } from '@/context/CartContext';
import {
    ShoppingCart,
    Trash2,
    Plus,
    Search,
    Filter,
    ArrowUpDown,
    ChevronLeft,
    ChevronRight,
    Home,
    X,
    Edit2,
    Sparkles,
} from 'lucide-react';

interface CategoryProductPageProps {
    categoryName: string;
    categorySlug: string;
    categoryAliases?: string[];
    bannerImage: string;
    videoUrl?: string;
    icon?: React.ReactNode;
    initialCategoryId?: number | null;
    initialProducts?: Product[];
    initialTotalElements?: number;
    initialTotalPages?: number;
}

type PriceRange = 'all' | 'under5' | '5to15' | '15to30' | 'above30';
type SortBy = 'newest' | 'price-asc' | 'price-desc' | 'name-asc';
const PAGE_SIZE = 12;

function getPriceBounds(range: PriceRange): { minPrice: number | null; maxPrice: number | null } {
    switch (range) {
        case 'under5': return { minPrice: null, maxPrice: 4_999_999 };
        case '5to15': return { minPrice: 5_000_000, maxPrice: 15_000_000 };
        case '15to30': return { minPrice: 15_000_000, maxPrice: 30_000_000 };
        case 'above30': return { minPrice: 30_000_001, maxPrice: null };
        default: return { minPrice: null, maxPrice: null };
    }
}

function getProductImageUrl(product: Product, fallback: string): string {
    return (
        (product.imageUrl?.trim() !== '' ? product.imageUrl : null) ||
        product.image_url ||
        product.image ||
        fallback
    );
}

export default function CategoryProductPage({
    categoryName,
    categorySlug,
    categoryAliases = [],
    bannerImage,
    videoUrl,
    initialCategoryId = null,
    initialProducts = [],
    initialTotalElements = 0,
    initialTotalPages = 1,
}: CategoryProductPageProps) {
    const { addToCart } = useCart();

    const [products, setProducts] = useState<Product[]>(initialProducts);
    const [categoryId, setCategoryId] = useState<number | null>(initialCategoryId);
    const [page, setPage] = useState<number>(0);
    const [totalElements, setTotalElements] = useState<number>(
        initialTotalElements || initialProducts.length
    );
    const [totalPages, setTotalPages] = useState<number>(Math.max(1, initialTotalPages));
    const [isLoading, setIsLoading] = useState<boolean>(initialProducts.length === 0);
    const [userRole, setUserRole] = useState<string | null>(null);

    const [searchKeyword, setSearchKeyword] = useState<string>('');
    const [debouncedKeyword, setDebouncedKeyword] = useState<string>('');
    const [priceRange, setPriceRange] = useState<PriceRange>('all');
    const [sortBy, setSortBy] = useState<SortBy>('newest');

    const hasInitialSsrData = useRef<boolean>(initialProducts.length > 0);
    const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);
    const [isSaving, setIsSaving] = useState<boolean>(false);
    const [newProduct, setNewProduct] = useState({
        name: '',
        price: '',
        stock: '10',
        imageUrl: bannerImage || '/HINH_TRANG_CHU/logo2.jpg',
        videoUrl: '',
    });

    const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'ADMIN';

    useEffect(() => {
        setUserRole(getUserRoleFromToken());
    }, []);

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedKeyword(searchKeyword.trim());
            setPage(0);
        }, 250);
        return () => clearTimeout(timer);
    }, [searchKeyword]);

    // Tìm categoryId từ backend để truy vấn phân trang server-side và gắn khi tạo sản phẩm mới
    const resolveCategoryId = useCallback(async (): Promise<number | null> => {
        if (categoryId != null) return categoryId;
        try {
            const categories = await fetchClient('/categories');
            const searchNames = [categoryName, ...categoryAliases].map(s => s.toLowerCase().trim());

            const matched = Array.isArray(categories)
                ? categories.find((c: any) =>
                    searchNames.includes(c.name?.toLowerCase().trim()) ||
                    searchNames.some(sn => c.name?.toLowerCase().includes(sn))
                )
                : null;

            if (matched?.id) {
                setCategoryId(matched.id);
                return matched.id;
            }
        } catch (err) {
            console.warn('[CategoryProductPage] Lỗi lấy danh mục:', err);
        }
        return null;
    }, [categoryId, categoryName, categoryAliases]);

    const loadProducts = useCallback(async () => {
        setIsLoading(true);
        try {
            const resolvedCatId = await resolveCategoryId();
            const { minPrice, maxPrice } = getPriceBounds(priceRange);

            if (resolvedCatId != null) {
                const paged = await productService.getPagedProducts({
                    page,
                    size: PAGE_SIZE,
                    categoryId: resolvedCatId,
                    keyword: debouncedKeyword || undefined,
                    minPrice,
                    maxPrice,
                    sort: sortBy,
                });
                setProducts(paged.content);
                setTotalElements(paged.totalElements);
                setTotalPages(Math.max(1, paged.totalPages));
                return;
            }

            // Fallback nếu chưa ánh xạ được categoryId trong DB
            const items = await productService.getProductsByCategory(
                categorySlug,
                [categoryName, ...categoryAliases]
            );
            const filtered = items
                .filter(p => {
                    if (debouncedKeyword && !p.name?.toLowerCase().includes(debouncedKeyword.toLowerCase())) return false;
                    const price = p.price || 0;
                    if (minPrice != null && price < minPrice) return false;
                    if (maxPrice != null && price > maxPrice) return false;
                    return true;
                })
                .sort((a, b) => {
                    if (sortBy === 'price-asc') return (a.price || 0) - (b.price || 0);
                    if (sortBy === 'price-desc') return (b.price || 0) - (a.price || 0);
                    if (sortBy === 'name-asc') return (a.name || '').localeCompare(b.name || '');
                    return (b.id || 0) - (a.id || 0);
                });
            setTotalElements(filtered.length);
            setTotalPages(Math.max(1, Math.ceil(filtered.length / PAGE_SIZE)));
            setProducts(filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE));
        } catch (err) {
            console.error('[CategoryProductPage] Lỗi tải sản phẩm:', err);
            setProducts([]);
        } finally {
            setIsLoading(false);
        }
    }, [resolveCategoryId, page, priceRange, debouncedKeyword, sortBy, categorySlug, categoryName, categoryAliases]);

    useEffect(() => {
        if (hasInitialSsrData.current && page === 0 && !debouncedKeyword && priceRange === 'all' && sortBy === 'newest') {
            hasInitialSsrData.current = false;
            return;
        }
        loadProducts();
    }, [loadProducts, page, debouncedKeyword, priceRange, sortBy]);

    useEffect(() => {
        const unsubscribe = productService.subscribe(() => {
            loadProducts();
        });
        return () => unsubscribe();
    }, [loadProducts]);

    const filteredProducts = products;

    const handleAddToCart = (product: Product) => {
        const imageUrl = getProductImageUrl(product, bannerImage);
        addToCart({
            id: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            imageUrl: getValidMediaUrl(imageUrl),
        });
        alert(`Đã thêm "${product.name}" vào giỏ hàng!`);
    };

    const handleDeleteProduct = async (id: number, name: string) => {
        const role = getUserRoleFromToken();
        if (role !== 'ROLE_ADMIN' && role !== 'ADMIN') {
            alert('Cấm tuyệt đối: Chỉ có Quản trị viên (Admin) mới có quyền xóa sản phẩm!');
            return;
        }
        if (!confirm(`Bạn có chắc muốn xóa "${name}"? Thao tác này sẽ xóa sản phẩm khỏi toàn bộ hệ thống.`)) return;
        try {
            await productService.deleteProduct(id, name);
            setProducts(prev => prev.filter(p => p.id !== id));
            alert('Đã xóa sản phẩm thành công!');
        } catch (err: any) {
            console.error('Lỗi khi xóa:', err);
            alert(err.message || 'Lỗi khi xóa sản phẩm');
        }
    };

    const handleSaveNewProduct = async (e: React.FormEvent) => {
        e.preventDefault();
        const role = getUserRoleFromToken();
        if (role !== 'ROLE_ADMIN' && role !== 'ADMIN') {
            alert('Cấm tuyệt đối: Chỉ có Quản trị viên (Admin) mới có quyền thêm sản phẩm!');
            setIsAddModalOpen(false);
            return;
        }
        if (!newProduct.name.trim() || !newProduct.price) {
            alert('Vui lòng nhập tên và giá sản phẩm!');
            return;
        }

        setIsSaving(true);
        try {
            const resolvedId = categoryId || (await resolveCategoryId()) || 1;

            const payload = {
                name: newProduct.name.trim(),
                price: parseFloat(newProduct.price),
                stock: parseInt(newProduct.stock) || 10,
                description: `Sản phẩm ${categoryName}`,
                imageUrl: newProduct.imageUrl.trim() || bannerImage,
                videoUrl: newProduct.videoUrl.trim() || null,
                category: { id: resolvedId, name: categoryName },
            };

            await productService.createProduct(payload);
            alert(`Đã lưu "${payload.name}" thành công! Sản phẩm đã xuất hiện trên Trang chủ.`);

            setIsAddModalOpen(false);
            setNewProduct({
                name: '',
                price: '',
                stock: '10',
                imageUrl: bannerImage || '/HINH_TRANG_CHU/logo2.jpg',
                videoUrl: '',
            });

            await loadProducts();
        } catch (err: any) {
            console.error('Lỗi khi thêm sản phẩm:', err);
            alert(err.message || 'Lỗi khi thêm sản phẩm');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className="w-full flex flex-col gap-6">
            <nav className="flex items-center gap-2 text-xs font-semibold text-white/90 bg-[#173B70]/80 backdrop-blur-xs px-4 py-2 rounded-xl w-fit border border-white/10 shadow-xs">
                <Link href="/" className="flex items-center gap-1 hover:text-blue-300 transition-colors">
                    <Home size={14} /> Trang chủ
                </Link>
                <ChevronRight size={14} />
                <span className="text-amber-400 font-bold">{categoryName}</span>
            </nav>

            <div className="bg-white rounded-[20px] shadow-[4px_4px_15px_#173B70] p-4 sm:p-5 border-2 border-[#173B70]/20 flex flex-col lg:flex-row items-center justify-between gap-5">
                <div className="flex items-center gap-4">
                    <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-blue-50 border-2 border-[#173B70]/20 p-2 flex items-center justify-center shrink-0 shadow-inner">
                        <img
                            src={getValidMediaUrl(bannerImage)}
                            alt={categoryName}
                            className="w-full h-full object-contain"
                            onError={(e) => { (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/logo2.jpg'); }}
                        />
                    </div>
                    <div>
                        <h1 className="text-xl sm:text-2xl font-black text-[#173B70] tracking-wide">
                            {categoryName}
                        </h1>
                        <span className="inline-block mt-1 text-xs font-bold px-3 py-1 rounded-full bg-blue-100 text-[#173B70] border border-blue-200">
                            {totalElements} sản phẩm
                        </span>
                    </div>
                </div>

                {videoUrl && (
                    <div className="w-full lg:w-96 xl:w-[420px] bg-black rounded-[16px] overflow-hidden border-2 border-[#173B70] shadow-md flex items-center justify-center max-h-[190px]">
                        <video
                            autoPlay loop muted controls playsInline
                            className="w-full h-full max-h-[190px] object-cover"
                        >
                            <source src={getValidMediaUrl(videoUrl)} type="video/mp4" />
                            Trình duyệt của bạn không hỗ trợ thẻ video.
                        </video>
                    </div>
                )}

                {isAdmin && (
                    <div className="flex items-center gap-2 shrink-0">
                        <button
                            onClick={() => setIsAddModalOpen(true)}
                            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#173B70] hover:bg-[#0866e8] text-white font-bold text-xs shadow-md transition-all whitespace-nowrap"
                        >
                            <Plus size={15} /> + Thêm SP
                        </button>
                    </div>
                )}
            </div>

            <div className="bg-white rounded-[20px] shadow-[5px_5px_15px_#173B70] p-5 sm:p-7 border border-blue-900/10">
                <div className="bg-slate-50 border border-slate-200 rounded-2xl p-3.5 mb-6 flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
                    <div className="relative flex-1">
                        <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                        <input
                            type="text"
                            value={searchKeyword}
                            onChange={(e) => setSearchKeyword(e.target.value)}
                            placeholder={`Tìm kiếm sản phẩm ${categoryName}...`}
                            className="w-full pl-9 pr-3 py-2 rounded-xl bg-white border border-slate-300 text-slate-800 placeholder-slate-400 text-xs sm:text-sm focus:outline-none focus:border-[#173B70] transition-colors"
                        />
                    </div>

                    <div className="flex items-center gap-2">
                        <Filter size={15} className="text-[#173B70] shrink-0" />
                        <select
                            value={priceRange}
                            onChange={(e) => {
                                setPriceRange(e.target.value as PriceRange);
                                setPage(0);
                            }}
                            className="bg-white border border-slate-300 rounded-xl px-3 py-1.5 text-xs font-semibold text-slate-700 focus:outline-none focus:border-[#173B70]"
                        >
                            <option value="all">Tất cả mức giá</option>
                            <option value="under5">Dưới 5 triệu</option>
                            <option value="5to15">Từ 5 - 15 triệu</option>
                            <option value="15to30">Từ 15 - 30 triệu</option>
                            <option value="above30">Trên 30 triệu</option>
                        </select>
                    </div>

                    <div className="flex items-center gap-2">
                        <ArrowUpDown size={15} className="text-[#173B70] shrink-0" />
                        <select
                            value={sortBy}
                            onChange={(e) => {
                                setSortBy(e.target.value as SortBy);
                                setPage(0);
                            }}
                            className="bg-white border border-slate-300 rounded-xl px-3 py-1.5 text-xs font-semibold text-slate-700 focus:outline-none focus:border-[#173B70]"
                        >
                            <option value="newest">Mới nhất</option>
                            <option value="price-asc">Giá tăng dần</option>
                            <option value="price-desc">Giá giảm dần</option>
                            <option value="name-asc">Tên A-Z</option>
                        </select>
                    </div>
                </div>

                {isLoading ? (
                    <div className="py-16 text-center text-[#173B70]">
                        <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-[#173B70] border-t-transparent mb-2" />
                        <p className="font-semibold text-slate-600 text-sm">Đang tải sản phẩm {categoryName}...</p>
                    </div>
                ) : filteredProducts.length === 0 ? (
                    <div className="bg-slate-50 rounded-2xl p-10 text-center text-slate-600 border border-slate-200">
                        <p className="font-bold text-sm">Không tìm thấy sản phẩm {categoryName} phù hợp</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 pt-2">
                        {filteredProducts.map((product, idx) => {
                            const finalImage = getValidMediaUrl(getProductImageUrl(product, bannerImage));
                            const discountLabel = product.discount || ['Giảm 18%', 'Giảm 15%', 'Giảm 20%', 'Giảm 12%'][idx % 4];
                            const isOutOfStock = product.stock !== undefined && product.stock <= 0;

                            return (
                                <div
                                    key={product.id}
                                    className="bg-white rounded-[16px] border border-slate-200 p-4 pt-5 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between group relative mt-3"
                                >
                                    <span className="absolute -top-3 left-4 bg-[#d70018] text-white text-[11px] font-black px-3 py-0.5 rounded-full shadow-md z-10 border-2 border-white">
                                        {discountLabel}
                                    </span>

                                    <div className="flex justify-end items-center mb-1">
                                        {product.stock !== undefined && (
                                            <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md ${
                                                isOutOfStock ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-700'
                                            }`}>
                                                {isOutOfStock ? 'Hết hàng' : `Còn ${product.stock}`}
                                            </span>
                                        )}
                                    </div>

                                    <div className="w-full h-44 sm:h-48 overflow-hidden rounded-xl bg-slate-50 flex items-center justify-center p-2 mb-3">
                                        <img
                                            src={finalImage}
                                            alt={product.name}
                                            className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
                                            onError={(e) => { (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png'); }}
                                        />
                                    </div>

                                    <div className="flex-1 flex flex-col">
                                        <h3
                                            className="font-bold text-sm text-[#173B70] line-clamp-2 min-h-[38px] group-hover:text-[#0866e8] transition-colors"
                                            title={product.name}
                                        >
                                            {product.name}
                                        </h3>
                                        <div className="flex items-center gap-1 my-1.5 text-xs text-amber-500 font-bold">
                                            <span>★★★★★</span>
                                        </div>
                                        <div className="mt-auto pt-2">
                                            <p className="text-[#d70018] font-black text-base sm:text-lg">
                                                {new Intl.NumberFormat('vi-VN').format(product.price)}đ
                                            </p>
                                        </div>
                                    </div>

                                    <div className="mt-3 pt-3 border-t border-slate-100 flex items-center gap-2">
                                        <button
                                            onClick={() => handleAddToCart(product)}
                                            disabled={isOutOfStock}
                                            className={`flex-1 py-2 rounded-[8px] font-bold text-xs text-white flex items-center justify-center gap-1.5 transition-all shadow-sm ${
                                                isOutOfStock
                                                    ? 'bg-slate-300 cursor-not-allowed'
                                                    : 'bg-[#0866e8] hover:bg-[#173B70] active:scale-95'
                                            }`}
                                        >
                                            <ShoppingCart size={15} />
                                            {isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
                                        </button>

                                        {isAdmin && (
                                            <div className="flex items-center gap-1.5">
                                                <Link
                                                    href={`/products/edit/${product.id}`}
                                                    title="Sửa sản phẩm"
                                                    className="px-2.5 py-2 rounded-[8px] bg-blue-50 hover:bg-[#173B70] text-[#173B70] hover:text-white transition-colors flex items-center gap-1 text-xs font-bold"
                                                >
                                                    <Edit2 size={14} /> Sửa
                                                </Link>
                                                <button
                                                    onClick={() => handleDeleteProduct(product.id, product.name)}
                                                    title="Xóa khỏi hệ thống"
                                                    className="p-2 rounded-[8px] bg-red-50 hover:bg-red-600 text-red-600 hover:text-white transition-colors"
                                                >
                                                    <Trash2 size={15} />
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}

                {!isLoading && totalPages > 1 && (
                    <div className="mt-8 flex items-center justify-center gap-3">
                        <button
                            onClick={() => setPage(prev => Math.max(0, prev - 1))}
                            disabled={page <= 0}
                            className="inline-flex items-center gap-1 px-4 py-2 rounded-xl text-xs font-bold border border-slate-300 text-[#173B70] hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
                        >
                            <ChevronLeft size={15} /> Trang trước
                        </button>
                        <span className="text-xs font-bold text-slate-600 px-3 py-1.5 rounded-lg bg-slate-100">
                            Trang {page + 1} / {totalPages}
                        </span>
                        <button
                            onClick={() => setPage(prev => Math.min(totalPages - 1, prev + 1))}
                            disabled={page >= totalPages - 1}
                            className="inline-flex items-center gap-1 px-4 py-2 rounded-xl text-xs font-bold border border-slate-300 text-[#173B70] hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
                        >
                            Trang sau <ChevronRight size={15} />
                        </button>
                    </div>
                )}
            </div>

            {isAddModalOpen && (
                <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
                    <div className="bg-white rounded-3xl p-6 md:p-8 max-w-lg w-full shadow-2xl relative border-2 border-[#173B70]/20">
                        <button
                            onClick={() => setIsAddModalOpen(false)}
                            className="absolute top-5 right-5 text-slate-400 hover:text-slate-700 transition-colors"
                        >
                            <X size={20} />
                        </button>

                        <h2 className="text-xl font-black text-[#173B70] mb-4">
                            Thêm sản phẩm {categoryName}
                        </h2>

                        <form onSubmit={handleSaveNewProduct} className="space-y-3">
                            <div>
                                <label className="block text-xs font-bold text-[#173B70] mb-1">Tên sản phẩm *</label>
                                <input
                                    type="text"
                                    value={newProduct.name}
                                    onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
                                    placeholder={`Ví dụ: ${categoryName} Mới`}
                                    className="w-full px-3 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:border-[#173B70]"
                                    required
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs font-bold text-[#173B70] mb-1">Giá bán (VNĐ) *</label>
                                    <input
                                        type="number"
                                        value={newProduct.price}
                                        onChange={(e) => setNewProduct({ ...newProduct, price: e.target.value })}
                                        placeholder="15000000"
                                        className="w-full px-3 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:border-[#173B70]"
                                        required
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-[#173B70] mb-1">Số lượng tồn kho</label>
                                    <input
                                        type="number"
                                        value={newProduct.stock}
                                        onChange={(e) => setNewProduct({ ...newProduct, stock: e.target.value })}
                                        className="w-full px-3 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:border-[#173B70]"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-[#173B70] mb-1">Đường dẫn hình ảnh (Image URL)</label>
                                <input
                                    type="text"
                                    value={newProduct.imageUrl}
                                    onChange={(e) => setNewProduct({ ...newProduct, imageUrl: e.target.value })}
                                    placeholder={bannerImage}
                                    className="w-full px-3 py-2 text-xs sm:text-sm rounded-xl border border-slate-300 focus:outline-none focus:border-[#173B70]"
                                />
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-[#173B70] mb-1">Đường dẫn video (Video URL)</label>
                                <input
                                    type="text"
                                    value={newProduct.videoUrl}
                                    onChange={(e) => setNewProduct({ ...newProduct, videoUrl: e.target.value })}
                                    placeholder="https://... hoặc file .mp4"
                                    className="w-full px-3 py-2 text-xs sm:text-sm rounded-xl border border-slate-300 focus:outline-none focus:border-[#173B70]"
                                />
                            </div>

                            {(newProduct.imageUrl || newProduct.videoUrl) && (
                                <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                                    <span className="block text-[11px] font-bold text-slate-500 mb-1.5 flex items-center gap-1">
                                        <Sparkles size={12} className="text-amber-500" /> Xem trước:
                                    </span>
                                    <div className="flex gap-2">
                                        {newProduct.imageUrl && (
                                            <div className="w-20 h-16 bg-white rounded border overflow-hidden flex items-center justify-center">
                                                <img
                                                    src={getValidMediaUrl(newProduct.imageUrl)}
                                                    alt="Preview"
                                                    className="w-full h-full object-contain"
                                                />
                                            </div>
                                        )}
                                        {newProduct.videoUrl && (
                                            <div className="w-24 h-16 bg-black rounded border overflow-hidden flex items-center justify-center">
                                                <video
                                                    src={getValidMediaUrl(newProduct.videoUrl)}
                                                    className="w-full h-full object-contain"
                                                    muted autoPlay loop
                                                />
                                            </div>
                                        )}
                                    </div>
                                </div>
                            )}

                            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                                <button
                                    type="button"
                                    onClick={() => setIsAddModalOpen(false)}
                                    className="px-3.5 py-1.5 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200"
                                >
                                    Hủy
                                </button>
                                <button
                                    type="submit"
                                    disabled={isSaving}
                                    className="px-4 py-1.5 rounded-xl text-xs font-bold text-white bg-[#173B70] hover:bg-[#0866e8] transition-all disabled:opacity-50"
                                >
                                    {isSaving ? 'Đang lưu...' : 'Lưu sản phẩm'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
