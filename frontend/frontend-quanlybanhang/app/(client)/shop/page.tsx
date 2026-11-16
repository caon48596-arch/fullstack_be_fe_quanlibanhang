"use client";

import React, { useEffect, useState } from 'react';
import { productService, Product } from '@/util/productService';
import { useCart } from '@/context/CartContext';
import { getValidMediaUrl } from '@/util/fetchClient';

export default function ShopPage() {
    const [products, setProducts] = useState<Product[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const { addToCart } = useCart();

    useEffect(() => {
        const loadProducts = async () => {
            try {
                const data = await productService.getAllProducts();
                setProducts(data);
            } catch (err) {
                console.error("[ShopPage] Lỗi lấy sản phẩm từ database:", err);
            } finally {
                setIsLoading(false);
            }
        };
        loadProducts();
    }, []);

    const handleAddToCart = (product: Product) => {
        const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
        if (!token) {
            alert("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!");
            return;
        }

        const rawImageUrl = product.imageUrl || product.image_url || '/HINH_TRANG_CHU/logo2.jpg';
        addToCart({
            id: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            imageUrl: getValidMediaUrl(rawImageUrl)
        });
        alert(`Đã thêm "${product.name}" vào giỏ hàng!`);
    };

    return (
        <div className="bg-gray-50 min-h-screen py-12">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                {/* Phần Header của trang mua sắm */}
                <div className="text-center mb-12">
                    <h1 className="text-4xl font-extrabold text-gray-900 mb-4">
                        Cửa Hàng Công Nghệ
                    </h1>
                    <p className="text-lg text-gray-600 max-w-2xl mx-auto">
                        Khám phá những thiết bị công nghệ chính hãng từ hệ thống cơ sở dữ liệu IPWORLD.
                    </p>
                </div>

                {isLoading ? (
                    <div className="text-center py-16 text-gray-500">
                        <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
                        <p>Đang tải danh sách sản phẩm từ database...</p>
                    </div>
                ) : products.length === 0 ? (
                    <div className="text-center py-16 text-gray-500 bg-white rounded-2xl border border-gray-100 shadow-sm">
                        <p className="text-base font-semibold">Chưa có sản phẩm nào trong cơ sở dữ liệu.</p>
                    </div>
                ) : (
                    /* Lưới sản phẩm (Grid) */
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
                        {products.map((product) => {
                            const rawImageUrl = product.imageUrl || product.image_url || '/HINH_TRANG_CHU/logo2.jpg';
                            const displayImage = getValidMediaUrl(rawImageUrl);

                            return (
                                <div
                                    key={product.id}
                                    className="bg-white rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 overflow-hidden flex flex-col group border border-gray-100"
                                >
                                    {/* Ảnh sản phẩm */}
                                    <div className="h-48 overflow-hidden relative bg-gray-100 flex items-center justify-center">
                                        <img
                                            src={displayImage}
                                            alt={product.name}
                                            className="w-full h-full object-contain p-2 group-hover:scale-105 transition-transform duration-500"
                                        />
                                    </div>

                                    {/* Thông tin sản phẩm */}
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
                )}
            </div>
        </div>
    );
}
