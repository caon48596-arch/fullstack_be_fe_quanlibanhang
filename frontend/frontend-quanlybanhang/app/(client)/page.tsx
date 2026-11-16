"use client";

import React, { useEffect, useState, useMemo } from 'react';
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
    TrendingUp,
    Plus,
} from 'lucide-react';
import { useCart } from '@/context/CartContext';
import { fetchClient, getUserRoleFromToken, getValidMediaUrl } from '@/util/fetchClient';
import { productService, Product } from '@/util/productService';

interface Category {
    id: number;
    name: string;
}

const DEFAULT_CATEGORIES: Category[] = [
    { id: 1, name: 'Điện thoại' },
    { id: 2, name: 'Máy tính bảng' },
    { id: 3, name: 'Phụ kiện' },
    { id: 4, name: 'Laptop' },
    { id: 5, name: 'Đồng hồ thông minh' },
    { id: 6, name: 'PC' },
    { id: 7, name: 'Màn hình' },
];

export default function HomePage() {
    const [allProducts, setAllProducts] = useState<Product[]>([]);
    const [categories, setCategories] = useState<Category[]>(DEFAULT_CATEGORIES);
    const [selectedCategoryId, setSelectedCategoryId] = useState<number | 'all'>('all');
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const { addToCart } = useCart();
    const [userRole, setUserRole] = useState<string | null>(null);

    // Xác định quyền Admin
    useEffect(() => {
        setUserRole(getUserRoleFromToken());
    }, []);

    // Tải danh mục từ hệ thống
    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const data = await fetchClient('/categories');
                if (Array.isArray(data) && data.length > 0) {
                    setCategories(data);
                }
            } catch (err) {
                console.warn("[HomePage] Dùng danh mục mặc định:", err);
            }
        };
        fetchCategories();
    }, []);

    // Tải TOÀN BỘ sản phẩm của tất cả các mặt hàng
    const loadProducts = async () => {
        setIsLoading(true);
        setError(null);
        try {
            const data = await productService.getAllProducts();
            setAllProducts(data);
        } catch (err: any) {
            console.error("[HomePage] Lỗi tải sản phẩm:", err);
            setError(err.message || 'Không thể kết nối đến máy chủ để tải sản phẩm.');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadProducts();

        // Lắng nghe sự kiện khi có bất kỳ sản phẩm nào được THÊM, SỬA, hoặc XÓA từ các trang con
        const unsubscribe = productService.subscribe(() => {
            loadProducts();
        });

        return () => {
            unsubscribe();
        };
    }, []);

    // Hàm kiểm tra sản phẩm có thuộc danh mục được chọn hay không
    const matchCategory = (prod: Product, targetCatId: number): boolean => {
        if (prod.category?.id && prod.category.id === targetCatId) {
            return true;
        }

        const targetCat = categories.find(c => c.id === targetCatId);
        const targetName = (targetCat?.name || '').toLowerCase().trim();
        const prodCatName = (prod.category?.name || '').toLowerCase().trim();

        if (prodCatName && (prodCatName.includes(targetName) || targetName.includes(prodCatName))) {
            return true;
        }

        const pName = (prod.name || '').toLowerCase();
        const pDesc = (prod.description || '').toLowerCase();

        if (targetName.includes('điện thoại') || targetName.includes('dien thoai')) {
            return pName.includes('iphone') || pName.includes('điện thoại') || pDesc.includes('smartphone');
        }
        if (targetName.includes('laptop')) {
            return pName.includes('macbook') || pName.includes('laptop') || pName.includes('rog');
        }
        if (targetName.includes('pc')) {
            return pName.includes('pc') || pName.includes('gaming') || pName.includes('core i') || pName.includes('ryzen');
        }
        if (targetName.includes('màn hình') || targetName.includes('man hinh')) {
            return pName.includes('màn hình') || pName.includes('ultrasharp') || pName.includes('monitor');
        }
        if (targetName.includes('máy tính bảng') || targetName.includes('may tinh bang')) {
            return pName.includes('ipad') || pName.includes('tab') || pName.includes('máy tính bảng');
        }
        if (targetName.includes('phụ kiện') || targetName.includes('phu kien')) {
            return pName.includes('tai nghe') || pName.includes('chuột') || pName.includes('airpods') || pName.includes('phụ kiện');
        }

        return false;
    };

    // Danh sách sản phẩm hiển thị theo tab bộ lọc
    const displayedProducts = useMemo(() => {
        if (selectedCategoryId === 'all') {
            return allProducts;
        }
        return allProducts.filter(p => matchCategory(p, selectedCategoryId));
    }, [allProducts, selectedCategoryId, categories]);

    // Sản phẩm nổi bật: lấy trực tiếp từ allProducts (khi xóa thì mục nổi bật cũng mất theo)
    const featuredProducts = useMemo(() => {
        if (allProducts.length === 0) return [];
        return allProducts.slice(0, 4);
    }, [allProducts]);

    // Xử lý thêm vào giỏ hàng
    const handleAddToCart = (product: Product) => {
        const fallbackImages = [
            '/HINH_TRANG_CHU/IP16PRM.JPG',
            '/HINH_TRANG_CHU/MACBM2.JPG',
            '/HINH_TRANG_CHU/ipad.jpg',
            '/HINH_TRANG_CHU/TAINGHE.JPG',
            '/HINH_TRANG_CHU/CHUOT.JPG'
        ];
        const rawImageUrl = product.imageUrl && product.imageUrl.trim() !== ''
            ? product.imageUrl
            : (product.image_url || product.image || product.img || fallbackImages[product.id % fallbackImages.length]);

        addToCart({
            id: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            imageUrl: getValidMediaUrl(rawImageUrl)
        });
        alert(`Đã thêm "${product.name}" vào giỏ hàng thành công!`);
    };

    // Xử lý xóa sản phẩm: đồng bộ xóa ngay lập tức và giảm tổng sản phẩm
    const handleDeleteProduct = async (id: number, name: string) => {
        if (!confirm(`Bạn có chắc muốn xóa sản phẩm "${name}" khỏi toàn bộ hệ thống?`)) return;
        try {
            await productService.deleteProduct(id, name);
            setAllProducts(prev => prev.filter(p => p.id !== id));
            alert("Đã xóa sản phẩm thành công! Tổng sản phẩm trang chủ đã được cập nhật.");
        } catch (err: any) {
            console.error("Lỗi xóa sản phẩm:", err);
            alert("Lỗi khi xóa sản phẩm: " + err.message);
        }
    };

    const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'ADMIN';

    // Danh mục điều hướng trang chủ
    const staticNavCategories = [
        { name: 'Điện Thoại', href: '/dien-thoai', icon: '/HINH_TRANG_CHU/dienthoai.jpg', fallbackIcon: <Smartphone size={18} /> },
        { name: 'Laptop', href: '/laptop', icon: '/HINH_TRANG_CHU/laptop.jog.png', fallbackIcon: <Laptop size={18} /> },
        { name: 'PC', href: '/pc', icon: '/HINH_TRANG_CHU/pc.jpg', fallbackIcon: <Cpu size={18} /> },
        { name: 'Màn Hình', href: '/man-hinh', icon: '/HINH_TRANG_CHU/manhinh.png', fallbackIcon: <Monitor size={18} /> },
        { name: 'Máy Tính Bảng', href: '/may-tinh-bang', icon: '/HINH_TRANG_CHU/maytinhbang.jpg', fallbackIcon: <Tablet size={18} /> },
        { name: 'Phụ Kiện', href: '/phu-kien', icon: '/HINH_TRANG_CHU/phukien.jpg', fallbackIcon: <Headphones size={18} /> },
    ];

    const selectedCategoryName = selectedCategoryId === 'all'
        ? null
        : categories.find(c => c.id === selectedCategoryId)?.name || 'Danh mục';

    return (
        <div className="w-full flex flex-col gap-8">

            {/* ================= HERO SECTION: DANH MỤC TRÁI & KHUNG VIDEO PHẢI ================= */}
            <section className="w-full flex flex-col lg:flex-row gap-6 items-stretch justify-center">

                {/* DANH MỤC BÊN TRÁI */}
                <div className="w-full lg:w-64 xl:w-72 shrink-0 bg-white rounded-[20px] shadow-[4px_4px_15px_rgba(23,59,112,0.15)] p-4 flex flex-col justify-between border-2 border-[#173B70]/20">
                    <div>
                        <div className="flex items-center pb-3 mb-3 border-b-2 border-[#173B70]">
                            <h2 className="text-sm font-black text-[#173B70] tracking-wide flex items-center gap-2 whitespace-nowrap">
                                <Sparkles size={16} className="text-amber-500 shrink-0" />
                                <span>DANH MỤC SẢN PHẨM</span>
                            </h2>
                        </div>

                        <div className="flex flex-col gap-2">
                            {staticNavCategories.map((item, idx) => (
                                <Link
                                    key={idx}
                                    href={item.href}
                                    className="flex items-center px-3.5 py-2.5 rounded-[12px] text-xs font-bold bg-[#173B70] text-white hover:bg-[#0866e8] transition-all duration-200 shadow-xs hover:translate-x-1 group whitespace-nowrap"
                                >
                                    <div className="flex items-center gap-2.5 min-w-0">
                                        <img
                                            src={item.icon}
                                            alt={item.name}
                                            className="w-4 h-4 object-contain rounded bg-white/10 p-0.5 shrink-0"
                                            onError={(e) => {
                                                (e.target as HTMLElement).style.display = 'none';
                                            }}
                                        />
                                        <span className="font-semibold text-xs tracking-wide whitespace-nowrap truncate">
                                            {item.name}
                                        </span>
                                    </div>
                                </Link>
                            ))}
                        </div>
                    </div>
                </div>

                {/* KHUNG VIDEO BÊN PHẢI */}
                <div className="flex-1 bg-white rounded-[20px] shadow-[4px_4px_15px_rgba(23,59,112,0.15)] p-3 sm:p-4 flex flex-col justify-center border-4 border-[#173B70] overflow-hidden">
                    <div className="w-full relative rounded-[14px] overflow-hidden bg-black flex items-center justify-center max-h-[320px]">
                        <video
                            autoPlay
                            loop
                            muted
                            controls
                            playsInline
                            poster="/HINH_TRANG_CHU/ipwworlkd.png"
                            className="w-full h-full max-h-[320px] object-cover"
                        >
                            <source src="/HINH_TRANG_CHU/gemini_generated_video_6ee14454.mp4" type="video/mp4" />
                            <source src="/HINH_TRANG_CHU/videointro.mp4" type="video/mp4" />
                            <source src="/HINH_DIEN_THOAI/iPhone 16 Reveal (4K).mp4" type="video/mp4" />
                            Trình duyệt của bạn không hỗ trợ thẻ video.
                        </video>
                    </div>
                </div>
            </section>

            {/* ================= 1. SẢN PHẨM NỔI BẬT ================= */}
            {featuredProducts.length > 0 && (
                <section className="w-full bg-[#173B70] rounded-[20px] shadow-[5px_5px_15px_rgba(23,59,112,0.2)] p-6 sm:p-8 border border-blue-900/40">
                    <div className="flex items-center justify-between pb-3 mb-6 border-b border-white/20">
                        <div className="flex items-center gap-3">
                            <span className="w-3 h-8 bg-amber-400 rounded-full inline-block"></span>
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
                        {featuredProducts.map((product) => {
                            const rawImg = product.imageUrl || product.image_url || '/HINH_TRANG_CHU/logo2.jpg';
                            const mediaUrl = getValidMediaUrl(rawImg);
                            const discountBadge = product.discount || 'Hot';

                            return (
                                <div
                                    key={product.id}
                                    className="bg-white rounded-[16px] border border-slate-100 p-4 pt-5 shadow-md hover:shadow-2xl hover:-translate-y-1.5 transition-all duration-300 flex flex-col justify-between group relative mt-3"
                                >
                                    <span className="absolute -top-3 left-4 bg-[#d70018] text-white text-[11px] font-black px-3 py-0.5 rounded-full shadow-md z-10 border-2 border-white">
                                        {discountBadge}
                                    </span>

                                    <div className="flex justify-end items-center mb-1">
                                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700">
                                            Còn {product.stock !== undefined ? product.stock : 20}
                                        </span>
                                    </div>

                                    <div className="w-full h-44 sm:h-48 overflow-hidden rounded-xl bg-slate-50 flex items-center justify-center p-2 mb-3">
                                        <img
                                            src={mediaUrl}
                                            alt={product.name}
                                            className="w-full h-full object-contain group-hover:scale-108 transition-transform duration-300"
                                            onError={(e) => {
                                                (e.target as HTMLImageElement).src = '/HINH_TRANG_CHU/ipwworlkd.png';
                                            }}
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
                                        {!isAdmin ? (
                                            <button
                                                onClick={() => handleAddToCart(product)}
                                                className="w-full py-2.5 rounded-[8px] font-bold text-xs sm:text-sm text-white flex items-center justify-center gap-2 bg-[#0866e8] hover:bg-[#173B70] active:scale-98 transition-all shadow-sm"
                                            >
                                                <ShoppingCart size={16} />
                                                Thêm vào giỏ
                                            </button>
                                        ) : (
                                            <div className="w-full flex gap-2">
                                                <Link
                                                    href={`/products/edit/${product.id}`}
                                                    className="flex-1 py-2 rounded-lg bg-blue-50 text-[#173B70] hover:bg-[#173B70] hover:text-white font-bold text-xs flex items-center justify-center gap-1 transition-all"
                                                >
                                                    <Edit2 size={14} /> Sửa
                                                </Link>
                                                <button
                                                    onClick={() => handleDeleteProduct(product.id, product.name)}
                                                    className="p-2 rounded-lg bg-red-50 text-red-600 hover:bg-red-600 hover:text-white transition-all"
                                                    title="Xóa khỏi hệ thống"
                                                >
                                                    <Trash2 size={16} />
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            )}

            {/* ================= 2. TỔNG SẢN PHẨM / TẤT CẢ SẢN PHẨM ================= */}
            <section className="w-full bg-white rounded-[20px] shadow-[5px_5px_15px_rgba(23,59,112,0.12)] p-5 sm:p-7 border border-blue-900/10">
                <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 pb-4 mb-6 border-b-2 border-[#173B70]">
                    <div className="flex items-center gap-3">
                        <span className="w-3 h-8 bg-[#0866e8] rounded-full inline-block"></span>
                        <div className="flex flex-wrap items-center gap-2.5">
                            <h2 className="text-xl sm:text-2xl font-black text-[#173B70] tracking-wide">
                                Tất Cả Sản Phẩm
                            </h2>
                            {selectedCategoryName && (
                                <span className="text-xs font-extrabold px-3 py-1 rounded-full bg-amber-100 text-amber-900 border border-amber-300">
                                    Đang xem: {selectedCategoryName} ({displayedProducts.length})
                                </span>
                            )}
                            <span className="text-xs font-bold px-3 py-1 rounded-full bg-blue-100 text-[#173B70] border border-blue-200" title="Tổng toàn bộ sản phẩm trên hệ thống">
                                Tổng cộng: {allProducts.length} sản phẩm
                            </span>
                        </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                        {/* Admin Action Buttons */}
                        {isAdmin && (
                            <div className="flex items-center gap-2 mr-2">
                                <Link
                                    href="/admin#daily-revenue"
                                    className="bg-emerald-600 hover:bg-emerald-700 text-white px-3.5 py-1.5 rounded-full font-bold text-xs transition-all shadow-sm flex items-center gap-1.5 whitespace-nowrap"
                                >
                                    <TrendingUp size={14} /> Coi Doanh Thu Theo Ngày
                                </Link>
                                <Link
                                    href="/products/create"
                                    className="bg-[#173B70] hover:bg-[#0866e8] text-white px-3.5 py-1.5 rounded-full font-bold text-xs transition-all shadow-sm flex items-center gap-1.5 whitespace-nowrap"
                                >
                                    <Plus size={14} /> Thêm Sản Phẩm
                                </Link>
                            </div>
                        )}

                        {/* Bộ lọc danh mục nhanh - Nút Tất cả luôn giữ tổng số lượng allProducts */}
                        <div className="flex items-center gap-1.5 flex-wrap">
                            <button
                                onClick={() => setSelectedCategoryId('all')}
                                className={`text-xs font-bold px-3.5 py-1.5 rounded-full transition-all ${selectedCategoryId === 'all'
                                    ? 'bg-[#173B70] text-white shadow-sm'
                                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                                    }`}
                            >
                                Tất cả ({allProducts.length})
                            </button>
                            {categories.map((c) => {
                                const catCount = allProducts.filter(p => matchCategory(p, c.id)).length;
                                return (
                                    <button
                                        key={c.id}
                                        onClick={() => setSelectedCategoryId(c.id)}
                                        className={`text-xs font-bold px-3.5 py-1.5 rounded-full transition-all ${selectedCategoryId === c.id
                                            ? 'bg-[#0866e8] text-white shadow-sm'
                                            : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                                            }`}
                                    >
                                        {c.name} ({catCount})
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                </div>

                {isLoading ? (
                    <div className="py-20 text-center text-[#173B70]">
                        <div className="inline-block animate-spin rounded-full h-10 w-10 border-4 border-[#173B70] border-t-transparent mb-3"></div>
                        <p className="font-semibold text-slate-600">Đang tải toàn bộ sản phẩm từ hệ thống...</p>
                    </div>
                ) : error && allProducts.length === 0 ? (
                    <div className="bg-red-50 text-red-700 p-6 rounded-2xl text-center border border-red-200">
                        <p className="font-bold mb-1">Không thể tải danh sách sản phẩm</p>
                        <p className="text-sm">{error}</p>
                    </div>
                ) : displayedProducts.length === 0 ? (
                    <div className="bg-slate-50 rounded-2xl p-12 text-center text-slate-600 border border-slate-200">
                        <h3 className="text-lg font-bold mb-2">Chưa có sản phẩm nào trong danh mục này</h3>
                        <p className="text-xs text-slate-500">Hãy thêm sản phẩm mới hoặc chọn tab "Tất cả" để xem các mặt hàng khác.</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                        {displayedProducts.map((product, index) => {
                            const fallbackImages = [
                                '/HINH_TRANG_CHU/IP16PRM.JPG',
                                '/HINH_TRANG_CHU/MACBM2.JPG',
                                '/HINH_TRANG_CHU/TAINGHE.JPG',
                                '/HINH_TRANG_CHU/CHUOT.JPG',
                                '/HINH_TRANG_CHU/ipad.jpg',
                                '/HINH_TRANG_CHU/donggho.jpg',
                                '/HINH_TRANG_CHU/loajbl.jpg',
                                '/HINH_TRANG_CHU/camera.jpg'
                            ];
                            const rawImageUrl = product.imageUrl && product.imageUrl.trim() !== ''
                                ? product.imageUrl
                                : (product.image_url || product.image || product.img || fallbackImages[product.id % fallbackImages.length]);
                            const mediaUrl = getValidMediaUrl(rawImageUrl);

                            const discountRates = ['Giảm 35%', 'Giảm 22%', 'Giảm 37%', 'Giảm 29%', 'Giảm 18%', 'Giảm 15%'];
                            const discountLabel = product.discount || discountRates[index % discountRates.length];

                            return (
                                <div
                                    key={product.id}
                                    className="bg-white rounded-[16px] border border-slate-200 p-4 pt-5 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between group relative mt-3"
                                >
                                    {/* Nhãn giảm giá */}
                                    <span className="absolute -top-3 left-4 bg-[#d70018] text-white text-[11px] font-black px-3 py-0.5 rounded-full shadow-md z-10 border-2 border-white">
                                        {discountLabel}
                                    </span>

                                    {/* Tồn kho & Danh mục tag */}
                                    <div className="flex justify-between items-center mb-1">
                                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-blue-50 text-[#173B70] truncate max-w-[120px]">
                                            {product.category?.name || 'Công nghệ'}
                                        </span>
                                        {product.stock !== undefined && (
                                            <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md ${product.stock > 0 ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-600'
                                                }`}>
                                                {product.stock > 0 ? `Còn ${product.stock}` : 'Hết hàng'}
                                            </span>
                                        )}
                                    </div>

                                    {/* Hình ảnh */}
                                    <div className="w-full h-44 sm:h-48 overflow-hidden rounded-xl bg-slate-50 flex items-center justify-center p-2 mb-3">
                                        <img
                                            src={mediaUrl}
                                            alt={product.name}
                                            className="w-full h-full object-contain group-hover:scale-108 transition-transform duration-300"
                                            onError={(e) => {
                                                (e.target as HTMLImageElement).src = '/HINH_TRANG_CHU/ipwworlkd.png';
                                            }}
                                        />
                                    </div>

                                    {/* Tên & Đánh giá */}
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

                                    {/* Nút hành động */}
                                    <div className="mt-3 pt-3 border-t border-slate-100 flex items-center gap-2">
                                        {!isAdmin ? (
                                            <button
                                                onClick={() => handleAddToCart(product)}
                                                disabled={product.stock !== undefined && product.stock <= 0}
                                                className={`w-full py-2.5 rounded-[8px] font-bold text-xs sm:text-sm text-white flex items-center justify-center gap-2 transition-all shadow-sm ${product.stock !== undefined && product.stock <= 0
                                                    ? 'bg-slate-300 cursor-not-allowed'
                                                    : 'bg-[#0866e8] hover:bg-[#173B70] active:scale-98'
                                                    }`}
                                            >
                                                <ShoppingCart size={16} />
                                                Thêm vào giỏ
                                            </button>
                                        ) : (
                                            <div className="w-full flex gap-2">
                                                <Link
                                                    href={`/products/edit/${product.id}`}
                                                    className="flex-1 py-2 rounded-lg bg-blue-50 text-[#173B70] hover:bg-[#173B70] hover:text-white font-bold text-xs flex items-center justify-center gap-1 transition-all"
                                                >
                                                    <Edit2 size={14} /> Sửa
                                                </Link>
                                                <button
                                                    onClick={() => handleDeleteProduct(product.id, product.name)}
                                                    className="p-2 rounded-lg bg-red-50 text-red-600 hover:bg-red-600 hover:text-white transition-all"
                                                    title="Xóa khỏi hệ thống"
                                                >
                                                    <Trash2 size={16} />
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </section>
        </div>
    );
}
