"use client";

import React, { useEffect, useState, useCallback, useRef } from 'react';
import Link from 'next/link';
import {
    Smartphone,
    Laptop,
    Headphones,
    Monitor,
    Cpu,
    Tablet,
    ShoppingCart,
    Trash2,
    Edit2,
    Sparkles,
    Plus,
    ChevronLeft,
    ChevronRight,
} from 'lucide-react';
import { useCart } from '@/context/CartContext';
import { fetchClient, getUserRoleFromToken, getValidMediaUrl } from '@/util/fetchClient';
import { productService, Product, Category } from '@/util/productService';

const DEFAULT_CATEGORIES: Category[] = [
    { id: 1, name: 'Điện thoại' },
    { id: 2, name: 'Máy tính bảng' },
    { id: 3, name: 'Phụ kiện' },
    { id: 4, name: 'Laptop' },
    { id: 5, name: 'PC' },
    { id: 6, name: 'Màn hình' },
];

const DEFAULT_PRODUCT_IMAGE = '/HINH_TRANG_CHU/logo2.jpg';
const DISCOUNT_LABELS = ['Giảm 35%', 'Giảm 22%', 'Giảm 37%', 'Giảm 29%', 'Giảm 18%', 'Giảm 15%'];
const PAGE_SIZE = 12;

const NAV_CATEGORIES = [
    { name: 'Điện Thoại', href: '/dien-thoai', icon: '/HINH_TRANG_CHU/dienthoai.jpg', fallbackIcon: <Smartphone size={18} /> },
    { name: 'Laptop', href: '/laptop', icon: '/HINH_TRANG_CHU/laptop.jog.png', fallbackIcon: <Laptop size={18} /> },
    { name: 'PC', href: '/pc', icon: '/HINH_TRANG_CHU/pc.jpg', fallbackIcon: <Cpu size={18} /> },
    { name: 'Màn Hình', href: '/man-hinh', icon: '/HINH_TRANG_CHU/manhinh.png', fallbackIcon: <Monitor size={18} /> },
    { name: 'Máy Tính Bảng', href: '/may-tinh-bang', icon: '/HINH_TRANG_CHU/maytinhbang.jpg', fallbackIcon: <Tablet size={18} /> },
    { name: 'Phụ Kiện', href: '/phu-kien', icon: '/HINH_TRANG_CHU/phukien.jpg', fallbackIcon: <Headphones size={18} /> },
];

function getProductImageUrl(product: Product): string {
    return (
        (product.imageUrl?.trim() !== '' ? product.imageUrl : null) ||
        product.image_url ||
        product.image ||
        product.img ||
        DEFAULT_PRODUCT_IMAGE
    );
}

interface HomeClientProps {
    initialProducts: Product[];
    initialTotalElements: number;
    initialTotalPages: number;
    initialCategories: Category[];
}

export default function HomeClient({
    initialProducts,
    initialTotalElements,
    initialTotalPages,
    initialCategories,
}: HomeClientProps) {
    const [products, setProducts] = useState<Product[]>(initialProducts);
    const [featuredProducts, setFeaturedProducts] = useState<Product[]>(initialProducts.slice(0, 4));
    const [categories, setCategories] = useState<Category[]>(
        initialCategories.length > 0 ? initialCategories : DEFAULT_CATEGORIES
    );
    const [selectedCategoryId, setSelectedCategoryId] = useState<number | 'all'>('all');
    const [page, setPage] = useState<number>(0);
    const [totalElements, setTotalElements] = useState<number>(initialTotalElements);
    const [totalPages, setTotalPages] = useState<number>(Math.max(1, initialTotalPages));
    const [isLoading, setIsLoading] = useState<boolean>(initialProducts.length === 0);
    const [error, setError] = useState<string | null>(null);
    const [userRole, setUserRole] = useState<string | null>(null);

    const hasInitialSsrData = useRef<boolean>(initialProducts.length > 0);
    const { addToCart } = useCart();

    const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'ADMIN';

    useEffect(() => {
        setUserRole(getUserRoleFromToken());
    }, []);

    useEffect(() => {
        if (initialCategories.length > 0) return;
        const fetchCategories = async () => {
            try {
                const data = await fetchClient('/categories');
                if (Array.isArray(data) && data.length > 0) {
                    setCategories(data);
                }
            } catch (err) {
                console.warn('[HomeClient] Dùng danh mục mặc định do lỗi API:', err);
            }
        };
        fetchCategories();
    }, [initialCategories.length]);

    const loadProductsPage = useCallback(async (targetPage: number, targetCategoryId: number | 'all') => {
        setIsLoading(true);
        setError(null);
        try {
            const paged = await productService.getPagedProducts({
                page: targetPage,
                size: PAGE_SIZE,
                categoryId: targetCategoryId === 'all' ? null : targetCategoryId,
                sort: 'newest',
            });
            setProducts(paged.content);
            setTotalElements(paged.totalElements);
            setTotalPages(Math.max(1, paged.totalPages));
            if (targetCategoryId === 'all' && targetPage === 0) {
                setFeaturedProducts(paged.content.slice(0, 4));
            }
        } catch (err: any) {
            console.error('[HomeClient] Lỗi tải sản phẩm:', err);
            setError(err.message || 'Không thể kết nối đến máy chủ.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        // Nếu đã có dữ liệu SSR ở lần render đầu tiên (trang 0, tất cả danh mục), bỏ qua lần fetch trùng
        if (hasInitialSsrData.current && page === 0 && selectedCategoryId === 'all') {
            hasInitialSsrData.current = false;
            return;
        }
        loadProductsPage(page, selectedCategoryId);
    }, [page, selectedCategoryId, loadProductsPage]);

    useEffect(() => {
        const unsubscribe = productService.subscribe(() => {
            loadProductsPage(page, selectedCategoryId);
        });
        return () => unsubscribe();
    }, [page, selectedCategoryId, loadProductsPage]);

    const handleSelectCategory = (catId: number | 'all') => {
        setSelectedCategoryId(catId);
        setPage(0);
    };

    const selectedCategoryName = selectedCategoryId === 'all'
        ? null
        : categories.find(c => c.id === selectedCategoryId)?.name || 'Danh mục';

    const handleAddToCart = (product: Product) => {
        addToCart({
            id: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            imageUrl: getValidMediaUrl(getProductImageUrl(product)),
        });
        alert(`Đã thêm "${product.name}" vào giỏ hàng!`);
    };

    const handleDeleteProduct = async (id: number, name: string) => {
        const role = getUserRoleFromToken();
        if (role !== 'ROLE_ADMIN' && role !== 'ADMIN') {
            alert('Cấm tuyệt đối: Chỉ có Quản trị viên (Admin) mới có quyền xóa sản phẩm!');
            return;
        }
        if (!confirm(`Bạn có chắc muốn xóa sản phẩm "${name}" khỏi toàn bộ hệ thống?`)) return;
        try {
            await productService.deleteProduct(id, name);
            alert('Đã xóa sản phẩm thành công!');
            loadProductsPage(page, selectedCategoryId);
        } catch (err: any) {
            console.error('Lỗi xóa sản phẩm:', err);
            alert('Lỗi khi xóa sản phẩm: ' + err.message);
        }
    };

    return (
        <div className="w-full flex flex-col gap-8">
            <section className="w-full flex flex-col lg:flex-row gap-6 items-stretch justify-center">
                <div className="w-full lg:w-64 xl:w-72 shrink-0 bg-white rounded-[20px] shadow-[4px_4px_15px_rgba(23,59,112,0.15)] p-4 flex flex-col border-2 border-[#173B70]/20">
                    <div className="flex items-center pb-3 mb-3 border-b-2 border-[#173B70]">
                        <h2 className="text-sm font-black text-[#173B70] tracking-wide flex items-center gap-2 whitespace-nowrap">
                            <Sparkles size={16} className="text-amber-500 shrink-0" />
                            <span>DANH MỤC SẢN PHẨM</span>
                        </h2>
                    </div>

                    <div className="flex flex-col gap-2">
                        {NAV_CATEGORIES.map((item, idx) => (
                            <Link
                                key={idx}
                                href={item.href}
                                className="flex items-center px-3.5 py-2.5 rounded-[12px] text-xs font-bold bg-[#173B70] text-white hover:bg-[#0866e8] transition-all duration-200 shadow-xs hover:translate-x-1 group whitespace-nowrap"
                            >
                                <div className="flex items-center gap-2.5 min-w-0">
                                    <img
                                        src={getValidMediaUrl(item.icon)}
                                        alt={item.name}
                                        className="w-4 h-4 object-contain rounded bg-white/10 p-0.5 shrink-0"
                                        onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }}
                                    />
                                    <span className="font-semibold text-xs tracking-wide whitespace-nowrap truncate">
                                        {item.name}
                                    </span>
                                </div>
                            </Link>
                        ))}
                    </div>
                </div>

                <div className="flex-1 bg-white rounded-[20px] shadow-[4px_4px_15px_rgba(23,59,112,0.15)] p-3 sm:p-4 flex flex-col justify-center border-4 border-[#173B70] overflow-hidden">
                    <div className="w-full relative rounded-[14px] overflow-hidden bg-black flex items-center justify-center max-h-[320px]">
                        <video
                            autoPlay loop muted controls playsInline
                            poster={getValidMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png')}
                            className="w-full h-full max-h-[320px] object-cover"
                        >
                            <source src={getValidMediaUrl('/HINH_TRANG_CHU/gemini_generated_video_6ee14454.mp4')} type="video/mp4" />
                            <source src={getValidMediaUrl('/HINH_TRANG_CHU/videointro.mp4')} type="video/mp4" />
                            <source src={getValidMediaUrl('/HINH_DIEN_THOAI/iPhone 16 Reveal (4K).mp4')} type="video/mp4" />
                            Trình duyệt của bạn không hỗ trợ thẻ video.
                        </video>
                    </div>
                </div>
            </section>

            {featuredProducts.length > 0 && (
                <section className="w-full bg-[#173B70] rounded-[20px] shadow-[5px_5px_15px_rgba(23,59,112,0.2)] p-6 sm:p-8 border border-blue-900/40">
                    <div className="flex items-center justify-between pb-3 mb-6 border-b border-white/20">
                        <div className="flex items-center gap-3">
                            <span className="w-3 h-8 bg-amber-400 rounded-full inline-block" />
                            <h2 className="text-2xl sm:text-3xl font-black text-white tracking-wide">
                                Sản Phẩm Nổi Bật
                            </h2>
                        </div>

                        {isAdmin && (
                            <Link
                                href="/products/create"
                                className="bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold px-4 py-2 rounded-full shadow-md transition-all flex items-center gap-1.5"
                            >
                                <Plus size={14} /> Thêm Sản Phẩm Mới
                            </Link>
                        )}
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 pt-2">
                        {featuredProducts.map((product) => (
                            <ProductCard
                                key={product.id}
                                product={product}
                                discountLabel={product.discount || 'Hot'}
                                showAdminActions={false}
                                onAddToCart={handleAddToCart}
                                onDelete={handleDeleteProduct}
                            />
                        ))}
                    </div>
                </section>
            )}

            <section className="w-full bg-white rounded-[20px] shadow-[5px_5px_15px_rgba(23,59,112,0.12)] p-5 sm:p-7 border border-blue-900/10">
                <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 pb-4 mb-6 border-b-2 border-[#173B70]">
                    <div className="flex items-center gap-3">
                        <span className="w-3 h-8 bg-[#0866e8] rounded-full inline-block" />
                        <div className="flex flex-wrap items-center gap-2.5">
                            <h2 className="text-xl sm:text-2xl font-black text-[#173B70] tracking-wide">
                                Tất Cả Sản Phẩm
                            </h2>
                            {selectedCategoryName && (
                                <span className="text-xs font-extrabold px-3 py-1 rounded-full bg-amber-100 text-amber-900 border border-amber-300">
                                    {selectedCategoryName}
                                </span>
                            )}
                            <span className="text-xs font-bold px-3 py-1 rounded-full bg-blue-100 text-[#173B70] border border-blue-200">
                                Tổng cộng: {totalElements} sản phẩm
                            </span>
                        </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                        {isAdmin && (
                            <div className="flex items-center gap-2 mr-2">
                                <Link
                                    href="/products/create"
                                    className="bg-[#173B70] hover:bg-[#0866e8] text-white px-3.5 py-1.5 rounded-full font-bold text-xs transition-all shadow-sm flex items-center gap-1.5 whitespace-nowrap"
                                >
                                    <Plus size={14} /> Thêm Sản Phẩm
                                </Link>
                            </div>
                        )}

                        <div className="flex items-center gap-1.5 flex-wrap">
                            <button
                                onClick={() => handleSelectCategory('all')}
                                className={`text-xs font-bold px-3.5 py-1.5 rounded-full transition-all cursor-pointer ${selectedCategoryId === 'all'
                                    ? 'bg-[#173B70] text-white shadow-sm'
                                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                                    }`}
                            >
                                Tất cả
                            </button>

                            {categories.map((category) => (
                                <button
                                    key={category.id}
                                    onClick={() => handleSelectCategory(category.id)}
                                    className={`text-xs font-bold px-3.5 py-1.5 rounded-full transition-all cursor-pointer ${selectedCategoryId === category.id
                                        ? 'bg-[#0866e8] text-white shadow-sm'
                                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                                        }`}
                                >
                                    {category.name}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {isLoading ? (
                    <LoadingSpinner message="Đang tải sản phẩm từ hệ thống..." />
                ) : error && products.length === 0 ? (
                    <ErrorMessage message={error} />
                ) : products.length === 0 ? (
                    <EmptyState />
                ) : (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                            {products.map((product, index) => (
                                <ProductCard
                                    key={product.id}
                                    product={product}
                                    discountLabel={product.discount || DISCOUNT_LABELS[index % DISCOUNT_LABELS.length]}
                                    showAdminActions={isAdmin}
                                    onAddToCart={handleAddToCart}
                                    onDelete={handleDeleteProduct}
                                />
                            ))}
                        </div>

                        {totalPages > 1 && (
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
                    </>
                )}
            </section>
        </div>
    );
}

interface ProductCardProps {
    product: Product;
    discountLabel: string;
    showAdminActions: boolean;
    onAddToCart: (product: Product) => void;
    onDelete: (id: number, name: string) => void;
}

function ProductCard({ product, discountLabel, showAdminActions, onAddToCart, onDelete }: ProductCardProps) {
    const imageUrl = getValidMediaUrl(getProductImageUrl(product));
    const isOutOfStock = product.stock !== undefined && product.stock <= 0;

    return (
        <div className="bg-white rounded-[16px] border border-slate-200 p-4 pt-5 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between group relative mt-3">
            <span className="absolute -top-3 left-4 bg-[#d70018] text-white text-[11px] font-black px-3 py-0.5 rounded-full shadow-md z-10 border-2 border-white">
                {discountLabel}
            </span>

            <div className="flex justify-between items-center mb-1">
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-blue-50 text-[#173B70] truncate max-w-[120px]">
                    {product.category?.name || 'Công nghệ'}
                </span>
                {product.stock !== undefined && (
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md ${isOutOfStock ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-700'
                        }`}>
                        {isOutOfStock ? 'Hết hàng' : `Còn ${product.stock}`}
                    </span>
                )}
            </div>

            <div className="w-full h-44 sm:h-48 overflow-hidden rounded-xl bg-slate-50 flex items-center justify-center p-2 mb-3">
                <img
                    src={imageUrl}
                    alt={product.name}
                    className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
                    onError={(e) => { (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png'); }}
                />
            </div>

            <div className="flex-1 flex flex-col">
                <h3
                    className="font-bold text-xs sm:text-sm text-[#173B70] line-clamp-2 min-h-[36px] group-hover:text-[#0866e8] transition-colors"
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
                {showAdminActions ? (
                    <div className="w-full flex gap-2">
                        <Link
                            href={`/products/edit/${product.id}`}
                            className="flex-1 py-2 rounded-lg bg-blue-50 text-[#173B70] hover:bg-[#173B70] hover:text-white font-bold text-xs flex items-center justify-center gap-1 transition-all"
                        >
                            <Edit2 size={14} /> Sửa
                        </Link>
                        <button
                            onClick={() => onDelete(product.id, product.name)}
                            className="p-2 rounded-lg bg-red-50 text-red-600 hover:bg-red-600 hover:text-white transition-all"
                            title="Xóa khỏi hệ thống"
                        >
                            <Trash2 size={16} />
                        </button>
                    </div>
                ) : (
                    <button
                        onClick={() => onAddToCart(product)}
                        disabled={isOutOfStock}
                        className={`w-full py-2.5 rounded-[8px] font-bold text-xs sm:text-sm text-white flex items-center justify-center gap-2 transition-all shadow-sm ${isOutOfStock
                            ? 'bg-slate-300 cursor-not-allowed'
                            : 'bg-[#0866e8] hover:bg-[#173B70] active:scale-95 cursor-pointer'
                            }`}
                    >
                        <ShoppingCart size={16} />
                        {isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
                    </button>
                )}
            </div>
        </div>
    );
}

function LoadingSpinner({ message }: { message: string }) {
    return (
        <div className="py-20 text-center text-[#173B70]">
            <div className="inline-block animate-spin rounded-full h-10 w-10 border-4 border-[#173B70] border-t-transparent mb-3" />
            <p className="font-semibold text-slate-600">{message}</p>
        </div>
    );
}

function ErrorMessage({ message }: { message: string }) {
    return (
        <div className="bg-red-50 text-red-700 p-6 rounded-2xl text-center border border-red-200">
            <p className="font-bold mb-1">Không thể tải danh sách sản phẩm</p>
            <p className="text-sm">{message}</p>
        </div>
    );
}

function EmptyState() {
    return (
        <div className="bg-slate-50 rounded-2xl p-12 text-center text-slate-600 border border-slate-200">
            <h3 className="text-lg font-bold mb-2">Chưa có sản phẩm nào trong hệ thống</h3>
            <p className="text-xs text-slate-500">
                Chỉ hiển thị các sản phẩm được Quản trị viên (Admin) tạo mới hoặc cập nhật.
            </p>
        </div>
    );
}
