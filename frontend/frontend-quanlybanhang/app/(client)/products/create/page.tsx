"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { fetchClient, getUserRoleFromToken, getValidMediaUrl } from '@/util/fetchClient';
import { productService } from '@/util/productService';
import { ArrowLeft, TrendingUp, Image as ImageIcon, Video, Package, Check, Sparkles } from 'lucide-react';

interface ProductFormData {
    name: string;
    price: number | '';
    stock: number | '';
    description: string;
    imageUrl: string;
    videoUrl: string;
    category: {
        id: number | '';
    };
}

interface Category {
    id: number;
    name: string;
}

export default function CreateProductPage() {
    const router = useRouter();

    const [formData, setFormData] = useState<ProductFormData>({
        name: '',
        price: '',
        stock: 20,
        description: '',
        imageUrl: '',
        videoUrl: '',
        category: {
            id: ''
        }
    });

    const [categories, setCategories] = useState<Category[]>([]);
    const [isSaving, setIsSaving] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [isCheckingRole, setIsCheckingRole] = useState<boolean>(true);
    const [isAdmin, setIsAdmin] = useState<boolean>(false);

    useEffect(() => {
        const role = getUserRoleFromToken();
        setIsAdmin(role === 'ROLE_ADMIN' || role === 'ADMIN');
        setIsCheckingRole(false);

        const fetchCategories = async () => {
            try {
                const data = await fetchClient('/categories');
                setCategories(Array.isArray(data) ? data : []);
            } catch (err) {
                console.error("Lỗi lấy danh mục:", err);
            }
        };
        fetchCategories();
    }, []);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        if (name === 'categoryId') {
            setFormData(prev => ({
                ...prev,
                category: {
                    id: value === '' ? '' : Number(value)
                }
            }));
            return;
        }
        if (name === 'price' || name === 'stock') {
            setFormData(prev => ({
                ...prev,
                [name]: value === '' ? '' : Number(value)
            }));
            return;
        }
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSelectSampleImage = (url: string) => {
        setFormData(prev => ({ ...prev, imageUrl: url }));
    };

    const handleSelectSampleVideo = (url: string) => {
        setFormData(prev => ({ ...prev, videoUrl: url }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSaving(true);
        setError(null);

        try {
            const categoryObj = categories.find(c => c.id === Number(formData.category.id));
            const payload = {
                name: formData.name,
                price: Number(formData.price) || 0,
                stock: Number(formData.stock) || 0,
                description: formData.description,
                imageUrl: formData.imageUrl,
                videoUrl: formData.videoUrl,
                category: formData.category.id ? { id: Number(formData.category.id), name: categoryObj?.name || 'Khác' } : null
            };

            await productService.createProduct(payload);

            alert("Thêm sản phẩm thành công! Sản phẩm đã xuất hiện trên Trang chủ và danh mục tương ứng.");
            router.push('/');
        } catch (err: any) {
            console.error("[CreateProduct] Lỗi thêm mới:", err);
            setError(err.message || 'Lỗi khi thêm mới sản phẩm.');
        } finally {
            setIsSaving(false);
        }
    };

    if (isCheckingRole) {
        return (
            <div className="flex items-center justify-center min-h-[50vh] text-slate-500 font-medium text-sm">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#173B70] mr-3"></div>
                Đang xác thực quyền truy cập...
            </div>
        );
    }

    if (!isAdmin) {
        return (
            <div className="max-w-xl mx-auto my-12 bg-white rounded-2xl p-8 border-2 border-red-200 text-center shadow-lg">
                <p className="text-red-600 font-black text-lg mb-2">Quyền Truy Cập Bị Từ Chối</p>
                <p className="text-slate-600 text-xs sm:text-sm mb-6">
                    Bạn cần đăng nhập bằng tài khoản Quản trị viên (Admin) để thực hiện chức năng thêm sản phẩm.
                </p>
                <Link
                    href="/login"
                    className="inline-block px-5 py-2.5 rounded-xl bg-[#173B70] hover:bg-[#0866e8] text-white font-bold text-xs"
                >
                    Đăng nhập tài khoản Admin
                </Link>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto px-4 py-6 w-full">
            {/* Thanh điều hướng đầu trang */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-6 bg-white rounded-2xl p-4 shadow-sm border border-slate-200">
                <div className="flex items-center gap-3">
                    <Link
                        href="/products"
                        className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
                        title="Quay lại danh sách sản phẩm"
                    >
                        <ArrowLeft size={18} />
                    </Link>
                    <div>
                        <h1 className="text-lg sm:text-xl font-black text-[#173B70]">Thêm Sản Phẩm Mới</h1>
                        <p className="text-xs text-slate-500 font-medium">Bổ sung hình ảnh, video, tên sản phẩm và giá cả</p>
                    </div>
                </div>

                {/* Nút trở về coi doanh thu chi tiết theo ngày */}
                <Link
                    href="/admin#daily-revenue"
                    className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-sm transition-all whitespace-nowrap"
                >
                    <TrendingUp size={16} /> Coi doanh thu theo ngày
                </Link>
            </div>

            {/* Form Thêm Sản Phẩm */}
            <div className="bg-white rounded-2xl shadow-md border border-blue-900/10 p-5 sm:p-7">
                {error && (
                    <div className="p-3 mb-5 rounded-xl text-xs font-bold bg-red-100 text-red-700 border border-red-200">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    {/* Tên sản phẩm & Danh mục */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-bold text-[#173B70] mb-1">
                                Tên sản phẩm <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="text"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                required
                                placeholder="Ví dụ: iPhone 16 Pro Max 256GB"
                                className="w-full px-3 py-2 text-xs sm:text-sm border border-slate-300 rounded-xl outline-none focus:border-[#173B70] focus:ring-1 focus:ring-[#173B70] transition-colors"
                            />
                        </div>

                        <div>
                            <label className="block text-xs font-bold text-[#173B70] mb-1">
                                Danh mục <span className="text-red-500">*</span>
                            </label>
                            <select
                                name="categoryId"
                                value={formData.category.id}
                                onChange={handleChange}
                                required
                                className="w-full px-3 py-2 text-xs sm:text-sm border border-slate-300 rounded-xl outline-none focus:border-[#173B70] focus:ring-1 focus:ring-[#173B70] bg-white transition-colors"
                            >
                                <option value="">-- Chọn danh mục sản phẩm --</option>
                                {categories.map(cat => (
                                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Giá bán & Số lượng kho */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-bold text-[#173B70] mb-1">
                                Giá cả (VNĐ) <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="number"
                                name="price"
                                min="0"
                                value={formData.price}
                                onChange={handleChange}
                                required
                                placeholder="Ví dụ: 34990000"
                                className="w-full px-3 py-2 text-xs sm:text-sm border border-slate-300 rounded-xl outline-none focus:border-[#173B70] focus:ring-1 focus:ring-[#173B70] transition-colors"
                            />
                        </div>

                        <div>
                            <label className="block text-xs font-bold text-[#173B70] mb-1">
                                Số lượng tồn kho <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="number"
                                name="stock"
                                min="0"
                                value={formData.stock}
                                onChange={handleChange}
                                required
                                placeholder="Ví dụ: 50"
                                className="w-full px-3 py-2 text-xs sm:text-sm border border-slate-300 rounded-xl outline-none focus:border-[#173B70] focus:ring-1 focus:ring-[#173B70] transition-colors"
                            />
                        </div>
                    </div>

                    {/* Hình ảnh sản phẩm (Image URL) */}
                    <div>
                        <label className="block text-xs font-bold text-[#173B70] mb-1 flex items-center gap-1.5">
                            <ImageIcon size={15} /> Đường dẫn hình ảnh (Image URL)
                        </label>
                        <input
                            type="text"
                            name="imageUrl"
                            value={formData.imageUrl}
                            onChange={handleChange}
                            placeholder="Nhập link ảnh (https://... hoặc /HINH_DIEN_THOAI/ip16pro.jpg)"
                            className="w-full px-3 py-2 text-xs sm:text-sm border border-slate-300 rounded-xl outline-none focus:border-[#173B70] focus:ring-1 focus:ring-[#173B70] transition-colors"
                        />
                        {/* Gợi ý chọn ảnh mẫu */}
                        <div className="flex flex-wrap items-center gap-1.5 mt-2">
                            <span className="text-[11px] font-semibold text-slate-500">Mẫu có sẵn:</span>
                            {[
                                { label: 'iPhone 16 Pro', url: '/HINH_DIEN_THOAI/ip16pro.jpg' },
                                { label: 'MacBook M2', url: '/HINH_LAPTOP/MACBM2.JPG' },
                                { label: 'PC RTX 4070', url: '/HINH_PC/PCGamingRTX4070.jpg' },
                                { label: 'iPad Pro', url: '/HINH_MAY_TINH_BANG/ipad.jpg' },
                                { label: 'Tai nghe', url: '/HINH_PHU_KIEN/TAINGHE.JPG' }
                            ].map((sample, idx) => (
                                <button
                                    key={idx}
                                    type="button"
                                    onClick={() => handleSelectSampleImage(sample.url)}
                                    className="px-2 py-0.5 rounded-md text-[11px] font-bold bg-slate-100 hover:bg-blue-100 text-slate-700 hover:text-[#173B70] transition-colors cursor-pointer"
                                >
                                    {sample.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Video sản phẩm (Video URL) */}
                    <div>
                        <label className="block text-xs font-bold text-[#173B70] mb-1 flex items-center gap-1.5">
                            <Video size={15} /> Đường dẫn video giới thiệu (Video URL)
                        </label>
                        <input
                            type="text"
                            name="videoUrl"
                            value={formData.videoUrl}
                            onChange={handleChange}
                            placeholder="Nhập link video MP4 (https://... hoặc /HINH_TRANG_CHU/...)"
                            className="w-full px-3 py-2 text-xs sm:text-sm border border-slate-300 rounded-xl outline-none focus:border-[#173B70] focus:ring-1 focus:ring-[#173B70] transition-colors"
                        />
                        {/* Gợi ý chọn video mẫu */}
                        <div className="flex flex-wrap items-center gap-1.5 mt-2">
                            <span className="text-[11px] font-semibold text-slate-500">Video mẫu:</span>
                            {[
                                { label: 'Quảng cáo Apple', url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4' },
                                { label: 'Review Công nghệ', url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' }
                            ].map((sample, idx) => (
                                <button
                                    key={idx}
                                    type="button"
                                    onClick={() => handleSelectSampleVideo(sample.url)}
                                    className="px-2 py-0.5 rounded-md text-[11px] font-bold bg-slate-100 hover:bg-blue-100 text-slate-700 hover:text-[#173B70] transition-colors cursor-pointer"
                                >
                                    {sample.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Khung Xem Trước (Live Preview) Hình Ảnh & Video */}
                    {(formData.imageUrl || formData.videoUrl) && (
                        <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200">
                            <p className="text-xs font-bold text-slate-600 mb-3 flex items-center gap-1.5">
                                <Sparkles size={14} className="text-amber-500" /> Xem trước Media sản phẩm:
                            </p>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                {formData.imageUrl && (
                                    <div className="bg-white rounded-xl p-3 border border-slate-200 flex flex-col items-center">
                                        <span className="text-[11px] font-bold text-slate-500 mb-2">Ảnh đại diện</span>
                                        <div className="w-full h-40 bg-slate-100 rounded-lg overflow-hidden flex items-center justify-center">
                                            <img
                                                src={getValidMediaUrl(formData.imageUrl)}
                                                alt="Preview"
                                                className="w-full h-full object-contain"
                                                onError={(e) => {
                                                    (e.target as HTMLImageElement).src = '/HINH_TRANG_CHU/ipwworlkd.png';
                                                }}
                                            />
                                        </div>
                                    </div>
                                )}
                                {formData.videoUrl && (
                                    <div className="bg-white rounded-xl p-3 border border-slate-200 flex flex-col items-center">
                                        <span className="text-[11px] font-bold text-slate-500 mb-2">Video minh họa</span>
                                        <div className="w-full h-40 bg-black rounded-lg overflow-hidden flex items-center justify-center">
                                            <video
                                                src={formData.videoUrl}
                                                controls
                                                className="w-full h-full object-contain"
                                            />
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Mô tả sản phẩm */}
                    <div>
                        <label className="block text-xs font-bold text-[#173B70] mb-1">Mô tả sản phẩm</label>
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            rows={3}
                            placeholder="Nhập thông tin mô tả chi tiết sản phẩm, chính sách bảo hành..."
                            className="w-full px-3 py-2 text-xs sm:text-sm border border-slate-300 rounded-xl outline-none focus:border-[#173B70] focus:ring-1 focus:ring-[#173B70] resize-none transition-colors"
                        />
                    </div>

                    {/* Thao tác lưu & Trở về doanh thu */}
                    <div className="pt-4 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3">
                        <Link
                            href="/admin#daily-revenue"
                            className="text-xs font-bold text-emerald-700 hover:text-emerald-800 hover:underline flex items-center gap-1.5"
                        >
                            <TrendingUp size={15} /> Trở về coi doanh thu chi tiết theo ngày
                        </Link>

                        <div className="flex items-center gap-2 w-full sm:w-auto">
                            <button
                                type="button"
                                onClick={() => router.back()}
                                className="flex-1 sm:flex-none px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors"
                            >
                                Hủy
                            </button>
                            <button
                                type="submit"
                                disabled={isSaving}
                                className="flex-1 sm:flex-none px-6 py-2 rounded-xl text-xs sm:text-sm font-bold text-white bg-[#173B70] hover:bg-[#0866e8] transition-all disabled:opacity-50 shadow-md flex items-center justify-center gap-1.5"
                            >
                                <Check size={16} /> {isSaving ? 'Đang lưu...' : 'Thêm Sản Phẩm'}
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}
