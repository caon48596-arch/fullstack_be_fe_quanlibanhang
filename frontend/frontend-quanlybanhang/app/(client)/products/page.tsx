"use client";

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { getUserRoleFromToken, getValidMediaUrl } from '@/util/fetchClient';
import { productService, Product } from '@/util/productService';
import { ShoppingCart, Edit2, Trash2, Plus } from 'lucide-react';
import { useCart } from '@/context/CartContext';

export default function ProductsPage() {
    const router = useRouter();
    const [products, setProducts] = useState<Product[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [userRole, setUserRole] = useState<string | null>(null);
    const { addToCart } = useCart();

    const loadProducts = async () => {
        try {
            const data = await productService.getAllProducts();
            setProducts(data);
        } catch (err: any) {
            console.error("[Products] Lỗi tải dữ liệu:", err);
            setError(err.message || 'Không thể tải danh sách sản phẩm.');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        const role = getUserRoleFromToken();
        setUserRole(role);
        loadProducts();

        const unsubscribe = productService.subscribe(() => {
            loadProducts();
        });

        return () => {
            unsubscribe();
        };
    }, []);

    const handleEdit = (id: number) => {
        router.push(`/products/edit/${id}`);
    };

    const handleDelete = async (id: number, name: string) => {
        const role = getUserRoleFromToken();
        if (role !== 'ROLE_ADMIN' && role !== 'ADMIN') {
            alert("Cấm tuyệt đối: Chỉ có Quản trị viên (Admin) mới có quyền xóa sản phẩm!");
            return;
        }
        if (window.confirm(`Bạn có chắc chắn muốn xóa sản phẩm "${name}" khỏi toàn bộ hệ thống không?`)) {
            try {
                await productService.deleteProduct(id, name);
                setProducts(prev => prev.filter(p => p.id !== id));
                alert("Đã xóa sản phẩm thành công! Trang chủ và hệ thống đã được đồng bộ.");
            } catch (err: any) {
                console.error("[Products] Lỗi khi xóa:", err);
                alert(err.message || 'Lỗi hệ thống khi xóa sản phẩm.');
            }
        }
    };

    const handleAddToCart = (product: Product) => {
        const rawImageUrl = product.imageUrl && product.imageUrl.trim() !== '' 
            ? product.imageUrl 
            : (product.image_url || product.image || product.img || '/HINH_TRANG_CHU/logo2.jpg');
        
        addToCart({
            id: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            imageUrl: getValidMediaUrl(rawImageUrl)
        });
        alert(`Đã thêm "${product.name}" vào giỏ hàng!`);
    };

    const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'ADMIN';

    if (isLoading) {
        return (
            <div className="text-center py-24 text-white">
                <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-white border-t-transparent mb-4"></div>
                <p className="font-semibold text-lg text-blue-100">Đang tải danh sách sản phẩm...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="max-w-4xl mx-auto p-6 mt-8 bg-red-500/90 text-white rounded-[20px] shadow-[5px_5px_15px_#173B70]">
                <p className="font-bold text-lg mb-1">Lỗi truy xuất</p>
                <p className="text-sm">{error}</p>
            </div>
        );
    }

    return (
        <div className="w-full flex flex-col gap-6">
            <div className="bg-white rounded-[20px] shadow-[5px_5px_15px_#173B70] px-6 py-4 flex flex-col sm:flex-row justify-between items-center gap-4 border border-blue-900/10">
                <div className="flex items-center gap-3">
                    <span className="w-3 h-8 bg-[#173B70] rounded-full inline-block"></span>
                    <div>
                        <h1 className="text-xl sm:text-2xl font-black text-[#173B70] tracking-wide">
                            Tất Cả Sản Phẩm
                        </h1>
                        <p className="text-xs text-slate-500 font-medium">Hiện có {products.length} sản phẩm chính hãng</p>
                    </div>
                </div>

                {isAdmin && (
                    <div className="flex items-center gap-2">
                        <Link
                            href="/products/create"
                            className="bg-[#173B70] hover:bg-[#0866e8] text-white px-4 py-2 rounded-full font-bold text-xs transition-all shadow-md flex items-center gap-1.5 whitespace-nowrap"
                        >
                            <Plus size={15} /> Thêm Sản Phẩm Mới
                        </Link>
                    </div>
                )}
            </div>

            <div className="bg-[#173B70] rounded-[20px] shadow-[5px_5px_15px_#173B70] p-6 border border-blue-900/30">
                {products.length === 0 ? (
                    <div className="bg-white rounded-[16px] py-16 text-center text-slate-600">
                        <p className="font-bold text-base mb-1">Chưa có sản phẩm nào trong hệ thống</p>
                        <p className="text-xs text-slate-400">Hãy thêm mới từ trang quản trị</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5">
                        {products.map((product, index) => {
                            const fallbackImages = [
                                '/HINH_TRANG_CHU/IP16PRM.JPG',
                                '/HINH_TRANG_CHU/MACBM2.JPG',
                                '/HINH_TRANG_CHU/TAINGHE.JPG',
                                '/HINH_TRANG_CHU/CHUOT.JPG',
                                '/HINH_TRANG_CHU/ipad.jpg',
                                '/HINH_TRANG_CHU/donggho.jpg'
                            ];
                            const rawImageUrl = product.imageUrl && product.imageUrl.trim() !== '' 
                                ? product.imageUrl 
                                : (product.image_url || product.image || product.img || fallbackImages[product.id % fallbackImages.length]);
                            const mediaUrl = getValidMediaUrl(rawImageUrl);

                            const discounts = ['Giảm 35%', 'Giảm 25%', 'Giảm 20%', 'Giảm 15%', 'Giảm 10%'];
                            const discount = discounts[index % discounts.length];

                            return (
                                <div 
                                    key={product.id} 
                                    className="bg-white rounded-[16px] p-4 shadow-md hover:shadow-2xl hover:-translate-y-1.5 transition-all duration-300 flex flex-col justify-between group"
                                >
                                    <div>
                                        <div className="flex justify-between items-center mb-2">
                                            <span className="bg-[#d70018] text-white text-[10px] font-black px-2 py-0.5 rounded-full">
                                                {discount}
                                            </span>
                                            {product.stock !== undefined && (
                                                <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md">
                                                    Còn {product.stock}
                                                </span>
                                            )}
                                        </div>

                                        <div className="w-full h-44 overflow-hidden rounded-xl bg-slate-50 flex items-center justify-center p-2 mb-3">
                                            <img
                                                src={mediaUrl}
                                                alt={product.name}
                                                className="w-full h-full object-contain group-hover:scale-108 transition-transform duration-300"
                                                onError={(e) => {
                                                    (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png');
                                                }}
                                            />
                                        </div>

                                        <h3 className="font-bold text-xs sm:text-sm text-[#173B70] line-clamp-2 min-h-[36px] group-hover:text-[#0866e8] transition-colors" title={product.name}>
                                            {product.name}
                                        </h3>

                                        <div className="flex items-center gap-1 my-1 text-xs text-amber-500 font-bold">
                                            <span>★★★★★</span>
                                            <span className="text-slate-400 font-normal text-[11px]">(5.0)</span>
                                        </div>

                                        <p className="text-[#d70018] font-black text-base sm:text-lg my-1">
                                            {new Intl.NumberFormat('vi-VN').format(product.price)}đ
                                        </p>
                                        
                                        <p className="text-xs text-slate-500 line-clamp-2 min-h-[2rem]">
                                            {product.description || 'Chính hãng 100%, bảo hành 12 tháng.'}
                                        </p>
                                    </div>

                                    <div className="mt-4 pt-3 border-t border-slate-100">
                                        {!isAdmin ? (
                                            <button
                                                onClick={() => handleAddToCart(product)}
                                                className="w-full py-2.5 rounded-[8px] bg-[#0866e8] hover:bg-[#173B70] text-white font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-sm"
                                            >
                                                <ShoppingCart size={15} /> Thêm vào giỏ
                                            </button>
                                        ) : (
                                            <div className="flex gap-2">
                                                <button
                                                    onClick={() => handleEdit(product.id)}
                                                    className="flex-1 bg-blue-50 hover:bg-[#173B70] hover:text-white text-[#173B70] py-2 rounded-lg font-bold text-xs transition-colors flex items-center justify-center gap-1"
                                                >
                                                    <Edit2 size={13} /> Sửa
                                                </button>
                                                <button
                                                    onClick={() => handleDelete(product.id, product.name)}
                                                    className="p-2 bg-red-50 hover:bg-red-600 hover:text-white text-red-600 rounded-lg transition-colors"
                                                    title="Xóa"
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
            </div>
        </div>
    );
}
