"use client";

import React, { useEffect, useState, useCallback, useRef } from 'react';
import { productService, Product } from '@/util/productService';
import { useCart } from '@/context/CartContext';
import { getValidMediaUrl } from '@/util/fetchClient';
import { ChevronLeft, ChevronRight, Search } from 'lucide-react';

interface ShopClientProps {
    initialProducts: Product[];
    initialTotalElements: number;
    initialTotalPages: number;
}

const PAGE_SIZE = 12;

export default function ShopClient({
    initialProducts,
    initialTotalElements,
    initialTotalPages,
}: ShopClientProps) {
    const [products, setProducts] = useState<Product[]>(initialProducts);
    const [totalElements, setTotalElements] = useState<number>(initialTotalElements);
    const [totalPages, setTotalPages] = useState<number>(Math.max(1, initialTotalPages));
    const [page, setPage] = useState<number>(0);
    const [keyword, setKeyword] = useState<string>('');
    const [debouncedKeyword, setDebouncedKeyword] = useState<string>('');
    const [sortBy, setSortBy] = useState<'newest' | 'price-asc' | 'price-desc' | 'name-asc'>('newest');
    const [isLoading, setIsLoading] = useState<boolean>(initialProducts.length === 0);

    const hasInitialSsrData = useRef<boolean>(initialProducts.length > 0);
    const { addToCart } = useCart();

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedKeyword(keyword.trim());
            setPage(0);
        }, 250);
        return () => clearTimeout(timer);
    }, [keyword]);

    const loadProducts = useCallback(async () => {
        setIsLoading(true);
        try {
            const paged = await productService.getPagedProducts({
                page,
                size: PAGE_SIZE,
                keyword: debouncedKeyword || undefined,
                sort: sortBy,
            });
            setProducts(paged.content);
            setTotalElements(paged.totalElements);
            setTotalPages(Math.max(1, paged.totalPages));
        } catch (err) {
            console.error('[ShopClient] Lỗi lấy sản phẩm từ database:', err);
        } finally {
            setIsLoading(false);
        }
    }, [page, debouncedKeyword, sortBy]);

    useEffect(() => {
        if (hasInitialSsrData.current && page === 0 && !debouncedKeyword && sortBy === 'newest') {
            hasInitialSsrData.current = false;
            return;
        }
        loadProducts();
    }, [page, debouncedKeyword, sortBy, loadProducts]);

    const handleAddToCart = (product: Product) => {
        const rawImageUrl = product.imageUrl || product.image_url || '/HINH_TRANG_CHU/logo2.jpg';
        addToCart({
            id: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            imageUrl: getValidMediaUrl(rawImageUrl),
        });
        alert(`Đã thêm "${product.name}" vào giỏ hàng!`);
    };

    return (
        <div className="bg-gray-50 min-h-screen py-12">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="text-center mb-10">
                    <h1 className="text-4xl font-extrabold text-gray-900 mb-3">
                        Cửa Hàng Công Nghệ
                    </h1>
                    <p className="text-lg text-gray-600 max-w-2xl mx-auto">
                        Khám phá {totalElements} thiết bị công nghệ chính hãng từ hệ thống IPWORLD.
                    </p>
                </div>

                <div className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100 mb-8 flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
                    <div className="relative flex-1">
                        <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
                        <input
                            type="text"
                            value={keyword}
                            onChange={(e) => setKeyword(e.target.value)}
                            placeholder="Tìm kiếm theo tên sản phẩm..."
                            className="w-full pl-9 pr-3 py-2 rounded-xl border border-gray-300 text-sm text-gray-800 focus:outline-none focus:border-blue-600"
                        />
                    </div>
                    <select
                        value={sortBy}
                        onChange={(e) => {
                            setSortBy(e.target.value as any);
                            setPage(0);
                        }}
                        className="px-3.5 py-2 rounded-xl border border-gray-300 text-sm font-semibold text-gray-700 bg-white focus:outline-none focus:border-blue-600"
                    >
                        <option value="newest">Mới nhất</option>
                        <option value="price-asc">Giá tăng dần</option>
                        <option value="price-desc">Giá giảm dần</option>
                        <option value="name-asc">Tên A-Z</option>
                    </select>
                </div>

                {isLoading ? (
                    <div className="text-center py-16 text-gray-500">
                        <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
                        <p>Đang tải danh sách sản phẩm...</p>
                    </div>
                ) : products.length === 0 ? (
                    <div className="text-center py-16 text-gray-500 bg-white rounded-2xl border border-gray-100 shadow-sm">
                        <p className="text-base font-semibold">Không tìm thấy sản phẩm phù hợp.</p>
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
                            {products.map((product) => {
                                const rawImageUrl = product.imageUrl || product.image_url || '/HINH_TRANG_CHU/logo2.jpg';
                                const displayImage = getValidMediaUrl(rawImageUrl);

                                return (
                                    <div
                                        key={product.id}
                                        className="bg-white rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 overflow-hidden flex flex-col group border border-gray-100"
                                    >
                                        <div className="h-48 overflow-hidden relative bg-gray-100 flex items-center justify-center">
                                            <img
                                                src={displayImage}
                                                alt={product.name}
                                                className="w-full h-full object-contain p-2 group-hover:scale-105 transition-transform duration-500"
                                            />
                                        </div>

                                        <div className="p-5 flex flex-col flex-grow">
                                            <div className="text-xs text-blue-600 font-bold mb-1">
                                                {product.category?.name || 'Sản phẩm'}
                                            </div>
                                            <h3 className="text-lg font-bold text-gray-900 line-clamp-1 mb-1" title={product.name}>
                                                {product.name}
                                            </h3>
                                            <p className="text-sm text-gray-500 line-clamp-2 mb-4 flex-grow">
                                                {product.description || 'Sản phẩm công nghệ chính hãng'}
                                            </p>

                                            <div className="mt-auto">
                                                <p className="text-xl font-black text-blue-600 mb-4">
                                                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
                                                </p>
                                                <button
                                                    onClick={() => handleAddToCart(product)}
                                                    className="w-full bg-gray-900 hover:bg-blue-600 text-white font-medium py-2.5 rounded-xl transition-colors cursor-pointer"
                                                >
                                                    Thêm vào giỏ
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>

                        {totalPages > 1 && (
                            <div className="mt-10 flex items-center justify-center gap-3">
                                <button
                                    onClick={() => setPage(prev => Math.max(0, prev - 1))}
                                    disabled={page <= 0}
                                    className="inline-flex items-center gap-1 px-4 py-2 rounded-xl text-xs font-bold border border-gray-300 bg-white text-gray-800 hover:bg-gray-100 disabled:opacity-40 cursor-pointer"
                                >
                                    <ChevronLeft size={15} /> Trang trước
                                </button>
                                <span className="text-xs font-bold text-gray-600 px-3 py-1.5 rounded-lg bg-white border border-gray-200">
                                    Trang {page + 1} / {totalPages}
                                </span>
                                <button
                                    onClick={() => setPage(prev => Math.min(totalPages - 1, prev + 1))}
                                    disabled={page >= totalPages - 1}
                                    className="inline-flex items-center gap-1 px-4 py-2 rounded-xl text-xs font-bold border border-gray-300 bg-white text-gray-800 hover:bg-gray-100 disabled:opacity-40 cursor-pointer"
                                >
                                    Trang sau <ChevronRight size={15} />
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}
