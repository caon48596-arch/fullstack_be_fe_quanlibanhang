"use client";

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCart } from '@/context/CartContext';
import { ShoppingBag, ArrowLeft, Trash2, Plus, Minus, Tag, ShieldCheck, FileText, CheckCircle2, Printer, ExternalLink, LogIn } from 'lucide-react';
import { fetchClient, getUserNameFromToken, getValidMediaUrl } from '@/util/fetchClient';

interface InvoiceData {
    orderId: number;
    orderDate: string;
    email: string;
    items: {
        id: number;
        name: string;
        price: number;
        quantity: number;
    }[];
    subtotal: number;
    discountAmount: number;
    couponCode: string | null;
    totalAmount: number;
}

export default function CheckoutPage() {
    const router = useRouter();
    const { cartItems, cartCount, cartTotal, removeFromCart, updateQuantity, clearCart } = useCart();
    const [isProcessing, setIsProcessing] = useState(false);
    const [couponCode, setCouponCode] = useState('');
    const [appliedCoupon, setAppliedCoupon] = useState<any>(null);
    const [couponMessage, setCouponMessage] = useState({ text: '', isError: false });
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [createdInvoice, setCreatedInvoice] = useState<InvoiceData | null>(null);
    // Idempotency key ổn định cho cùng một phiên thanh toán để tránh tạo đơn trùng nếu double-click / retry
    const idempotencyKeyRef = useRef<string>('');

    useEffect(() => {
        // Tạo mới idempotency key mỗi khi giỏ hàng hoặc coupon thay đổi
        idempotencyKeyRef.current = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
            ? crypto.randomUUID()
            : `order-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    }, [cartItems, appliedCoupon]);

    useEffect(() => {
        const token = typeof window !== 'undefined'
            ? (sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken'))
            : null;
        setIsLoggedIn(!!token);
    }, []);

    const handleApplyCoupon = async () => {
        if (!couponCode.trim()) return;
        setCouponMessage({ text: 'Đang kiểm tra...', isError: false });
        try {
            const res = await fetchClient(`/coupons/validate/${couponCode}`);
            if (res && res.id) {
                setAppliedCoupon(res);
                setCouponMessage({ text: 'Áp dụng mã giảm giá thành công!', isError: false });
            } else {
                setAppliedCoupon(null);
                setCouponMessage({ text: 'Mã giảm giá không hợp lệ hoặc đã hết lượt dùng.', isError: true });
            }
        } catch {
            setAppliedCoupon(null);
            setCouponMessage({ text: 'Mã giảm giá không hợp lệ hoặc đã hết lượt dùng.', isError: true });
        }
    };

    const handleCheckout = async () => {
        if (cartItems.length === 0) return;

        const token = typeof window !== 'undefined'
            ? (sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken'))
            : null;
        if (!token) {
            alert('Quý khách vui lòng đăng nhập để tiến hành thanh toán và tạo hóa đơn!');
            router.push('/login?redirect=/orders');
            return;
        }

        setIsProcessing(true);
        try {
            const items = cartItems.map(item => ({
                product: { id: item.id },
                quantity: item.quantity,
            }));

            const email = getUserNameFromToken() || 'Khách hàng IPWORLD';
            const idempotencyKey = idempotencyKeyRef.current || `order-${Date.now()}`;

            const res = await fetchClient('/orders/CreateAndSum', {
                method: 'POST',
                headers: {
                    'Idempotency-Key': idempotencyKey,
                },
                body: JSON.stringify({
                    items: items,
                    couponCode: appliedCoupon ? appliedCoupon.code : null,
                    email: email,
                    idempotencyKey,
                })
            });

            const savedOrder = res?.data || res;
            const resolvedOrderId = savedOrder?.id ?? Date.now();
            const resolvedTotal = typeof savedOrder?.totalAmount === 'number' ? savedOrder.totalAmount : finalTotal;

            const now = new Date();
            const formattedDateTime = now.toLocaleString('vi-VN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
            });

            const invoiceInfo: InvoiceData = {
                orderId: resolvedOrderId,
                orderDate: formattedDateTime,
                email: email,
                items: cartItems.map(i => ({ id: i.id, name: i.name, price: i.price, quantity: i.quantity })),
                subtotal: cartTotal,
                discountAmount: appliedCoupon ? appliedCoupon.discountAmount : 0,
                couponCode: appliedCoupon ? appliedCoupon.code : null,
                totalAmount: resolvedTotal
            };

            setCreatedInvoice(invoiceInfo);
            clearCart();
        } catch (error: any) {
            console.error('Lỗi thanh toán:', error);
            alert(error.message || 'Thanh toán thất bại. Vui lòng thử lại.');
        } finally {
            setIsProcessing(false);
        }
    };

    const finalTotal = appliedCoupon ? Math.max(0, cartTotal - appliedCoupon.discountAmount) : cartTotal;

    if (cartItems.length === 0 && !createdInvoice) {
        return (
            <div className="flex-1 flex flex-col items-center justify-center py-20 px-4 text-black">
                <div className="bg-white p-10 rounded-[20px] shadow-[5px_5px_15px_#173B70] text-center max-w-md w-full border border-blue-900/10">
                    <img 
                        src={getValidMediaUrl('/HINH_TRANG_CHU/giohang.jpg')} 
                        alt="Giỏ hàng trống" 
                        className="w-24 h-24 object-contain mx-auto mb-4 rounded-xl"
                        onError={(e) => {
                            (e.target as HTMLElement).style.display = 'none';
                        }}
                    />
                    <h2 className="text-2xl font-black text-black mb-2">Giỏ Hàng Trống</h2>
                    <p className="text-black text-sm mb-6">Bạn chưa có sản phẩm nào trong giỏ hàng.</p>
                    <Link
                        href="/"
                        className="bg-[#173B70] hover:bg-[#0866e8] text-white font-bold py-3 px-8 rounded-full transition-all inline-flex items-center gap-2 shadow-md cursor-pointer"
                    >
                        <ArrowLeft size={18} />
                        Khám phá sản phẩm ngay
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full flex flex-col gap-6 py-4 text-black">
            <div className="bg-white rounded-[20px] shadow-[5px_5px_15px_#173B70] px-6 py-4 flex items-center justify-between border border-blue-900/10">
                <div className="flex items-center gap-3">
                    <Link href="/" className="text-black hover:text-[#173B70] transition-colors p-1" title="Về trang chủ">
                        <ArrowLeft size={22} />
                    </Link>
                    <span className="w-3 h-8 bg-[#173B70] rounded-full inline-block"></span>
                    <h1 className="text-xl sm:text-2xl font-black text-black tracking-wide">
                        Giỏ Hàng Của Bạn ({cartCount} món)
                    </h1>
                </div>

                {cartItems.length > 0 && (
                    <button 
                        onClick={clearCart}
                        className="text-xs font-bold text-red-600 hover:text-red-800 hover:underline cursor-pointer"
                    >
                        Xóa tất cả
                    </button>
                )}
            </div>

            {!isLoggedIn && cartItems.length > 0 && (
                <div className="bg-amber-50 border-2 border-amber-300 rounded-[18px] p-4 flex flex-col sm:flex-row items-center justify-between gap-3 shadow-sm">
                    <div className="flex items-center gap-3 text-black">
                        <div className="p-2 bg-amber-100 rounded-full text-amber-800 shrink-0">
                            <LogIn size={20} />
                        </div>
                        <div>
                            <p className="text-sm font-bold text-black">Quý khách chưa đăng nhập tài khoản</p>
                            <p className="text-xs text-black">
                                Vui lòng đăng nhập để tiến hành thanh toán và xuất hóa đơn theo ngày giờ hiện tại.
                            </p>
                        </div>
                    </div>
                    <Link
                        href="/login?redirect=/orders"
                        className="px-5 py-2 rounded-xl bg-[#173B70] hover:bg-[#0866e8] text-white text-xs font-bold transition-all shadow whitespace-nowrap cursor-pointer"
                    >
                        Đăng nhập ngay
                    </Link>
                </div>
            )}

            <div className="flex flex-col lg:flex-row gap-6">
                <div className="flex-1 space-y-4">
                    {cartItems.map((item) => (
                        <div 
                            key={item.id} 
                            className="bg-white p-4 sm:p-5 rounded-[18px] shadow-[4px_4px_12px_rgba(23,59,112,0.2)] border border-slate-100 flex items-center gap-4 sm:gap-6 hover:shadow-lg transition-all"
                        >
                            <div className="w-20 h-20 sm:w-24 sm:h-24 bg-slate-50 rounded-xl flex-shrink-0 flex items-center justify-center overflow-hidden border border-slate-200 p-1">
                                {item.imageUrl ? (
                                    <img 
                                        src={getValidMediaUrl(item.imageUrl)} 
                                        alt={item.name} 
                                        className="w-full h-full object-contain" 
                                        onError={(e) => {
                                            (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png');
                                        }}
                                    />
                                ) : (
                                    <ShoppingBag className="w-8 h-8 text-slate-400" />
                                )}
                            </div>

                            <div className="flex-1 min-w-0">
                                <h3 className="font-bold text-sm sm:text-base text-black mb-1 truncate" title={item.name}>
                                    {item.name}
                                </h3>
                                <p className="text-[#d70018] font-black text-base">
                                    {new Intl.NumberFormat('vi-VN').format(item.price)}đ
                                </p>
                            </div>

                            <div className="flex items-center gap-3 sm:gap-4">
                                <div className="flex items-center bg-slate-100 border border-slate-300 rounded-full overflow-hidden">
                                    <button 
                                        onClick={() => updateQuantity(item.id, item.quantity - 1)}
                                        disabled={item.quantity <= 1}
                                        className="p-1.5 sm:p-2 text-black hover:bg-slate-200 disabled:opacity-40 transition-colors cursor-pointer"
                                        title="Giảm số lượng"
                                    >
                                        <Minus size={14} />
                                    </button>
                                    <div className="w-8 sm:w-10 text-center font-bold text-black text-xs sm:text-sm">
                                        {item.quantity}
                                    </div>
                                    <button 
                                        onClick={() => updateQuantity(item.id, item.quantity + 1)}
                                        className="p-1.5 sm:p-2 text-black hover:bg-slate-200 transition-colors cursor-pointer"
                                        title="Tăng số lượng"
                                    >
                                        <Plus size={14} />
                                    </button>
                                </div>
                                <button
                                    onClick={() => removeFromCart(item.id)}
                                    className="p-2 sm:p-2.5 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-full transition-all cursor-pointer"
                                    title="Xóa sản phẩm khỏi giỏ hàng"
                                >
                                    <Trash2 size={18} />
                                </button>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="w-full lg:w-[400px]">
                    <div className="bg-white p-6 rounded-[20px] shadow-[5px_5px_15px_#173B70] border border-blue-900/10 sticky top-24">
                        <h2 className="text-lg font-black text-black mb-4 pb-3 border-b-2 border-[#173B70] tracking-wide">
                            TỔNG QUAN ĐƠN HÀNG
                        </h2>

                        <div className="space-y-3 text-xs sm:text-sm text-black mb-5 pb-5 border-b border-dashed border-slate-300">
                            <div className="flex justify-between">
                                <span className="text-black font-semibold">Số lượng sản phẩm:</span>
                                <span className="font-bold text-black">{cartCount}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-black font-semibold">Tạm tính:</span>
                                <span className="font-bold text-black">
                                    {new Intl.NumberFormat('vi-VN').format(cartTotal)}đ
                                </span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-black font-semibold">Phí vận chuyển:</span>
                                <span className="font-bold text-emerald-700">Miễn phí toàn quốc</span>
                            </div>
                            {appliedCoupon && (
                                <div className="flex justify-between text-emerald-700 mt-2 font-bold">
                                    <span>Giảm giá ({appliedCoupon.code}):</span>
                                    <span>
                                        - {new Intl.NumberFormat('vi-VN').format(appliedCoupon.discountAmount)}đ
                                    </span>
                                </div>
                            )}
                        </div>

                        <div className="mb-5">
                            <label className="block text-xs font-bold text-black mb-1.5 flex items-center gap-1">
                                <Tag size={14} /> Mã Giảm Giá
                            </label>
                            <div className="flex gap-2">
                                <input
                                    type="text"
                                    value={couponCode}
                                    onChange={(e) => setCouponCode(e.target.value)}
                                    placeholder="Nhập mã ưu đãi..."
                                    className="flex-1 px-3 py-2 text-xs sm:text-sm border-2 border-[#173B70] rounded-xl outline-none focus:border-[#0866e8] uppercase text-black font-bold"
                                    disabled={appliedCoupon != null}
                                />
                                {appliedCoupon ? (
                                    <button
                                        onClick={() => {
                                            setAppliedCoupon(null);
                                            setCouponCode('');
                                            setCouponMessage({ text: '', isError: false });
                                        }}
                                        className="px-3 py-2 bg-red-100 text-red-700 font-bold text-xs rounded-xl hover:bg-red-200 transition-colors cursor-pointer"
                                    >
                                        Hủy
                                    </button>
                                ) : (
                                    <button
                                        onClick={handleApplyCoupon}
                                        className="px-4 py-2 bg-[#173B70] hover:bg-[#0866e8] text-white font-bold text-xs rounded-xl transition-colors cursor-pointer"
                                    >
                                        Áp dụng
                                    </button>
                                )}
                            </div>
                            {couponMessage.text && (
                                <p className={`text-xs mt-1.5 font-bold ${couponMessage.isError ? 'text-red-600' : 'text-emerald-700'}`}>
                                    {couponMessage.text}
                                </p>
                            )}
                        </div>

                        <div className="flex justify-between items-end mb-6 pt-2">
                            <span className="text-sm font-bold text-black">Thành tiền:</span>
                            <span className="text-2xl font-black text-[#d70018]">
                                {new Intl.NumberFormat('vi-VN').format(finalTotal)}đ
                            </span>
                        </div>

                        {isLoggedIn ? (
                            <button
                                onClick={handleCheckout}
                                disabled={isProcessing || cartItems.length === 0}
                                className={`w-full py-3.5 rounded-full font-black text-sm text-white transition-all shadow-md cursor-pointer ${
                                    isProcessing 
                                    ? 'bg-slate-400 cursor-not-allowed' 
                                    : 'bg-[#173B70] hover:bg-[#0866e8] active:scale-98'
                                }`}
                            >
                                {isProcessing ? 'ĐANG KHỞI TẠO HÓA ĐƠN...' : 'XÁC NHẬN THANH TOÁN & TẠO HÓA ĐƠN'}
                            </button>
                        ) : (
                            <Link
                                href="/login?redirect=/orders"
                                className="block w-full py-3.5 rounded-full font-black text-sm text-center text-white bg-[#173B70] hover:bg-[#0866e8] transition-all shadow-md cursor-pointer"
                            >
                                ĐĂNG NHẬP ĐỂ THANH TOÁN
                            </Link>
                        )}

                        <div className="mt-4 flex items-center justify-center gap-2 text-[11px] text-black font-semibold">
                            <ShieldCheck size={14} className="text-emerald-700" /> Cam kết bảo mật thông tin & Hóa đơn VAT chuẩn 100%
                        </div>
                    </div>
                </div>
            </div>

            {createdInvoice && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
                    <div className="bg-white rounded-2xl w-full max-w-2xl border-2 border-[#173B70] shadow-2xl overflow-hidden max-h-[90vh] flex flex-col text-black">
                        <div className="bg-[#173B70] text-white p-5 flex items-center justify-between">
                            <div className="flex items-center gap-3">
                                <div className="p-2 bg-white/10 rounded-xl">
                                    <FileText size={24} className="text-amber-300" />
                                </div>
                                <div>
                                    <h2 className="text-lg font-black tracking-wide">HÓA ĐƠN BÁN HÀNG IPWORLD</h2>
                                    <p className="text-xs text-blue-100">Hệ thống Công nghệ & Thiết bị Chính Hãng</p>
                                </div>
                            </div>
                            <span className="bg-emerald-500 text-white text-xs font-black px-3 py-1 rounded-full flex items-center gap-1">
                                <CheckCircle2 size={14} /> Thành công
                            </span>
                        </div>

                        <div className="p-6 overflow-y-auto space-y-5 text-black">
                            <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                                <div>
                                    <span className="text-black font-semibold">Mã hóa đơn:</span>
                                    <p className="text-sm font-black text-[#173B70]">#HD-{createdInvoice.orderId}</p>
                                </div>
                                <div>
                                    <span className="text-black font-semibold">Thời gian tạo hóa đơn:</span>
                                    <p className="text-sm font-black text-black">{createdInvoice.orderDate}</p>
                                </div>
                                <div>
                                    <span className="text-black font-semibold">Khách hàng:</span>
                                    <p className="text-sm font-bold text-black truncate">{createdInvoice.email}</p>
                                </div>
                                <div>
                                    <span className="text-black font-semibold">Trạng thái:</span>
                                    <p className="text-sm font-bold text-emerald-700">Đã tiếp nhận (Chờ duyệt)</p>
                                </div>
                            </div>

                            <div>
                                <h3 className="text-xs font-black text-black uppercase tracking-wider mb-2">Chi tiết sản phẩm</h3>
                                <div className="border border-slate-200 rounded-xl overflow-hidden">
                                    <table className="w-full text-left text-xs border-collapse">
                                        <thead className="bg-slate-100 border-b border-slate-200 text-black font-bold">
                                            <tr>
                                                <th className="py-2.5 px-3">Sản phẩm</th>
                                                <th className="py-2.5 px-3 text-center">SL</th>
                                                <th className="py-2.5 px-3 text-right">Đơn giá</th>
                                                <th className="py-2.5 px-3 text-right">Thành tiền</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-100">
                                            {createdInvoice.items.map((item, idx) => (
                                                <tr key={idx} className="hover:bg-slate-50/50">
                                                    <td className="py-2 px-3 font-bold text-black">{item.name}</td>
                                                    <td className="py-2 px-3 text-center text-black font-semibold">{item.quantity}</td>
                                                    <td className="py-2 px-3 text-right text-black font-medium">
                                                        {new Intl.NumberFormat('vi-VN').format(item.price)}đ
                                                    </td>
                                                    <td className="py-2 px-3 text-right font-black text-[#173B70]">
                                                        {new Intl.NumberFormat('vi-VN').format(item.price * item.quantity)}đ
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-2 text-xs">
                                <div className="flex justify-between text-black">
                                    <span className="font-semibold">Tạm tính:</span>
                                    <span className="font-bold">{new Intl.NumberFormat('vi-VN').format(createdInvoice.subtotal)}đ</span>
                                </div>
                                {createdInvoice.discountAmount > 0 && (
                                    <div className="flex justify-between text-emerald-700 font-bold">
                                        <span>Giảm giá ({createdInvoice.couponCode}):</span>
                                        <span>- {new Intl.NumberFormat('vi-VN').format(createdInvoice.discountAmount)}đ</span>
                                    </div>
                                )}
                                <div className="flex justify-between text-black">
                                    <span className="font-semibold">Phí giao hàng:</span>
                                    <span className="font-bold text-emerald-700">Miễn phí</span>
                                </div>
                                <div className="border-t border-slate-200 pt-2 flex justify-between items-center text-sm">
                                    <span className="font-black text-black">TỔNG THANH TOÁN:</span>
                                    <span className="text-lg font-black text-[#d70018]">
                                        {new Intl.NumberFormat('vi-VN').format(createdInvoice.totalAmount)}đ
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div className="bg-slate-100 p-4 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3">
                            <button
                                onClick={() => window.print()}
                                className="px-4 py-2 rounded-xl border border-slate-300 bg-white hover:bg-slate-200 text-black text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer"
                            >
                                <Printer size={15} /> In hóa đơn
                            </button>

                            <div className="flex gap-2">
                                <Link
                                    href="/my-orders"
                                    className="px-4 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-black text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer"
                                >
                                    <ExternalLink size={15} /> Đơn hàng của tôi
                                </Link>
                                <button
                                    onClick={() => {
                                        setCreatedInvoice(null);
                                        router.push('/');
                                    }}
                                    className="px-5 py-2 rounded-xl bg-[#173B70] hover:bg-[#0866e8] text-white text-xs font-bold transition-all shadow-sm cursor-pointer"
                                >
                                    Tiếp tục mua sắm
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
