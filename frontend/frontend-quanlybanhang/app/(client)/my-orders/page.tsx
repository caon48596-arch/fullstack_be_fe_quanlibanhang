"use client";

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { ShoppingBag, ArrowLeft, Clock, CheckCircle, XCircle, FileText } from 'lucide-react';
import { fetchClient, getUserNameFromToken, getValidMediaUrl } from '@/util/fetchClient';

export default function MyOrdersPage() {
    const [orders, setOrders] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const loadOrders = async () => {
            const email = getUserNameFromToken();
            if (!email) {
                setError("Vui lòng đăng nhập để xem danh sách hóa đơn.");
                setIsLoading(false);
                return;
            }
            try {
                const res = await fetchClient(`/orders/my-order-items?email=${encodeURIComponent(email)}`);
                setOrders(Array.isArray(res) ? res : []);
            } catch (err: any) {
                setError(err.message || "Không thể tải danh sách hóa đơn.");
            } finally {
                setIsLoading(false);
            }
        };
        loadOrders();
    }, []);

    const getStatusInfo = (status: string) => {
        switch (status) {
            case 'PAID':
                return { text: 'Đã Thanh Toán', color: 'text-blue-800 bg-blue-100 border-blue-300', icon: <CheckCircle size={16} className="text-blue-700" /> };
            case 'APPROVED':
            case 'CONFIRMED':
                return { text: 'Đã Xác Nhận', color: 'text-emerald-800 bg-emerald-100 border-emerald-300', icon: <CheckCircle size={16} className="text-emerald-700" /> };
            case 'SHIPPING':
            case 'SHIPPED':
                return { text: 'Đang Giao Hàng', color: 'text-indigo-800 bg-indigo-100 border-indigo-300', icon: <Clock size={16} className="text-indigo-700" /> };
            case 'COMPLETED':
                return { text: 'Hoàn Thành', color: 'text-emerald-900 bg-emerald-200 border-emerald-400', icon: <CheckCircle size={16} className="text-emerald-800" /> };
            case 'EXPIRED':
                return { text: 'Hết Hạn TT', color: 'text-slate-700 bg-slate-200 border-slate-300', icon: <XCircle size={16} className="text-slate-600" /> };
            case 'CANCELLED':
                return { text: 'Đã Hủy', color: 'text-red-800 bg-red-100 border-red-300', icon: <XCircle size={16} className="text-red-700" /> };
            default:
                return { text: 'Chờ Duyệt', color: 'text-amber-800 bg-amber-100 border-amber-300', icon: <Clock size={16} className="text-amber-700" /> };
        }
    };

    if (isLoading) {
        return (
            <div className="min-h-[50vh] flex flex-col items-center justify-center p-4 text-black font-semibold text-sm">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#173B70] mb-3"></div>
                Đang tải danh sách hóa đơn...
            </div>
        );
    }
    
    if (error) {
        return (
            <div className="min-h-[50vh] flex flex-col items-center justify-center p-4 text-black">
                <p className="text-red-600 font-bold mb-4">{error}</p>
                <Link href="/login?redirect=/my-orders" className="bg-[#173B70] hover:bg-[#0866e8] text-white px-6 py-2 rounded-full transition-colors font-bold text-sm cursor-pointer shadow">
                    Đăng nhập ngay
                </Link>
            </div>
        );
    }

    return (
        <div className="max-w-[1000px] mx-auto px-4 py-8 min-h-[calc(100vh-80px)] text-black">
            <div className="flex items-center justify-between mb-8 pb-4 border-b-2 border-[#173B70]">
                <div className="flex items-center gap-3">
                    <Link href="/" className="text-black hover:text-[#173B70] transition-colors p-1" title="Về trang chủ">
                        <ArrowLeft size={24} />
                    </Link>
                    <h1 className="text-2xl sm:text-3xl font-black text-black tracking-tight">
                        Lịch Sử Hóa Đơn & Đơn Hàng
                    </h1>
                </div>
                <div className="flex items-center gap-2 text-xs font-bold text-black bg-blue-50 border border-blue-200 px-3.5 py-1.5 rounded-full">
                    <FileText size={15} className="text-[#173B70]" />
                    <span>IPWORLD Store</span>
                </div>
            </div>

            {orders.length === 0 ? (
                <div className="bg-white p-10 rounded-3xl shadow-[5px_5px_15px_#173B70] border border-blue-900/10 text-center max-w-md mx-auto text-black">
                    <div className="w-24 h-24 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-6 border border-slate-200">
                        <ShoppingBag className="w-10 h-10 text-slate-400" />
                    </div>
                    <h2 className="text-2xl font-bold text-black mb-2">Chưa Có Hóa Đơn Nào</h2>
                    <p className="text-black text-sm mb-8">Bạn chưa thực hiện bất kỳ giao dịch mua hàng nào.</p>
                    <Link href="/" className="bg-[#173B70] hover:bg-[#0866e8] text-white font-bold py-3 px-8 rounded-full transition-all shadow-md cursor-pointer">
                        Mua sắm ngay
                    </Link>
                </div>
            ) : (
                <div className="grid gap-4">
                    {orders.map((item, idx) => {
                        const statusData = getStatusInfo(item.status || 'PENDING');
                        const product = item.product || {};
                        const order = item.order || {};
                        const itemTotal = (item.price || item.Price || product.price || 0) * (item.quantity || 1);

                        return (
                            <div key={item.id || idx} className="bg-white p-5 sm:p-6 rounded-2xl border-2 border-slate-100 shadow-[4px_4px_12px_rgba(23,59,112,0.15)] hover:shadow-lg transition-all flex flex-col md:flex-row items-start md:items-center justify-between gap-5 text-black">
                                <div className="flex items-start gap-4 flex-1">
                                    <div className="w-16 h-16 sm:w-20 sm:h-20 bg-slate-50 rounded-xl flex items-center justify-center border border-slate-200 shrink-0 overflow-hidden p-1">
                                        {product.imageUrl || product.image_url ? (
                                            <img 
                                                src={getValidMediaUrl(product.imageUrl || product.image_url)} 
                                                alt={product.name} 
                                                className="w-full h-full object-contain"
                                                onError={(e) => { (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png'); }}
                                            />
                                        ) : (
                                            <ShoppingBag className="w-6 h-6 text-[#173B70]" />
                                        )}
                                    </div>
                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-[11px] font-black uppercase tracking-wider bg-slate-100 text-black px-2 py-0.5 rounded">
                                                Mã HĐ: #HD-{order.id || item.id}
                                            </span>
                                        </div>
                                        <h3 className="font-bold text-base sm:text-lg text-black mb-1 line-clamp-1" title={product.name}>
                                            {product.name || `Sản phẩm theo hóa đơn #${order.id}`}
                                        </h3>
                                        <p className="text-xs sm:text-sm text-black flex items-center gap-2 mb-1">
                                            <span className="font-semibold text-black">Số lượng:</span> {item.quantity || 1}
                                        </p>
                                        <p className="text-xs sm:text-sm text-black flex items-center gap-1.5 font-medium">
                                            <Clock size={14} className="text-[#173B70]" />
                                            <span>Ngày giờ tạo hóa đơn:</span>
                                            <strong className="text-black">
                                                {order.orderDate ? new Date(order.orderDate).toLocaleString('vi-VN') : 'Hiện tại'}
                                            </strong>
                                        </p>
                                    </div>
                                </div>

                                <div className="flex flex-col items-end gap-3 w-full md:w-auto border-t md:border-t-0 pt-3 md:pt-0 border-slate-200">
                                    <p className="text-xl sm:text-2xl text-[#d70018] font-black tracking-tight">
                                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(itemTotal)}
                                    </p>
                                    <div className={`flex items-center gap-1.5 px-3 py-1 rounded-full font-bold text-xs border ${statusData.color}`}>
                                        {statusData.icon}
                                        {statusData.text}
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
