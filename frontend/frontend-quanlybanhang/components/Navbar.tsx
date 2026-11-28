"use client";

import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import {
    Search, ShoppingCart, User, X,
    Plus, Minus, Settings, LogOut,
    Package, LogIn, UserPlus, Menu,
} from 'lucide-react';
import { useCart } from '@/context/CartContext';
import { getUserRoleFromToken, getUserNameFromToken } from '@/util/fetchClient';
import { resolveMediaUrl } from '@/util/mediaUrl';
import {
    navbarStyles,
    modalStyles,
    formStyles,
} from '@/styles/constants';

const cx = (...parts: string[]) => parts.filter(Boolean).join(' ');

export default function Navbar() {
    const router = useRouter();
    const pathname = usePathname();

    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const { cartItems, cartCount, cartTotal, removeFromCart, updateQuantity } = useCart();
    const [isCartOpen, setIsCartOpen] = useState(false);
    const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const [settingsForm, setSettingsForm] = useState({ dob: '', phone: '', email: '' });
    const [isUpdatingSettings, setIsUpdatingSettings] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [userRole, setUserRole] = useState<string | null>(null);
    const [userName, setUserName] = useState<string | null>(null);

    const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'ADMIN';
    const apiBase = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';

    useEffect(() => {
        try {
            const token = typeof window !== 'undefined'
                ? (sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken'))
                : null;
            setIsLoggedIn(!!token);
            if (token) {
                setUserRole(getUserRoleFromToken());
                setUserName(getUserNameFromToken());
            } else {
                setUserRole(null);
                setUserName(null);
            }
        } catch (err) {
            console.error('[Navbar] Lỗi đọc token:', err);
            setIsLoggedIn(false);
            setUserRole(null);
            setUserName(null);
        }
    }, [pathname]);

    useEffect(() => {
        setIsCartOpen(false);
        setIsAccountMenuOpen(false);
        setIsMobileMenuOpen(false);
    }, [pathname]);

    const closeAllMenus = () => {
        setIsCartOpen(false);
        setIsAccountMenuOpen(false);
        setIsMobileMenuOpen(false);
    };

    const handleLogout = async () => {
        try {
            const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
            // Thu hồi Access Token trên Redis Blacklist & xóa HttpOnly Refresh Cookie phía Backend
            await fetch(`${apiBase}/users/logout`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
            }).catch(() => {});
        } finally {
            sessionStorage.removeItem('accessToken');
            localStorage.removeItem('accessToken');
            setIsLoggedIn(false);
            setUserRole(null);
            setUserName(null);
            router.push('/login');
        }
    };

    const handleSearchSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (searchTerm.trim()) {
            router.push(`/?search=${encodeURIComponent(searchTerm.trim())}`);
        }
    };

    const handleOpenSettings = async () => {
        setIsSettingsOpen(true);
        try {
            const token = sessionStorage.getItem('accessToken');
            const res = await fetch(`${apiBase}/users/profile`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {},
            });
            if (res.ok) {
                const { data } = await res.json();
                if (data) {
                    setSettingsForm({
                        email: data.email || '',
                        phone: data.phone || '',
                        dob: data.dob || '',
                    });
                }
            }
        } catch (err) {
            console.error('Lỗi lấy profile:', err);
        }
    };

    const handleSettingsUpdate = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsUpdatingSettings(true);
        try {
            const token = sessionStorage.getItem('accessToken');
            const res = await fetch(`${apiBase}/users/update`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
                body: JSON.stringify({
                    email: userName,
                    dob: settingsForm.dob,
                    phone: settingsForm.phone,
                    ...(settingsForm.email ? { email: settingsForm.email } : {}),
                }),
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Lỗi cập nhật');
            alert('Cập nhật thành công!');
            setIsSettingsOpen(false);
            // Đăng xuất để cấp lại JWT mới nếu người dùng thay đổi email đăng nhập
            if (settingsForm.email && settingsForm.email !== userName) handleLogout();
        } catch (error: any) {
            alert(error.message);
        } finally {
            setIsUpdatingSettings(false);
        }
    };

    const navPillClass = (isActive: boolean) =>
        cx(navbarStyles.navPill, isActive ? navbarStyles.navPillActive : navbarStyles.navPillDefault);

    const accountDropdownVisibility = isAccountMenuOpen
        ? 'opacity-100 visible'
        : 'opacity-0 invisible group-hover:opacity-100 group-hover:visible';

    const displayName = isLoggedIn
        ? (userName ? userName.split('@')[0] : 'Tài khoản')
        : 'Tài khoản';

    return (
        <header className={navbarStyles.header}>
            <div className={navbarStyles.bar}>
                <div className={navbarStyles.leftGroup}>
                    <Link href="/" title="Về trang chủ IPWORLD" className={navbarStyles.logoLink}>
                        <img
                            src={resolveMediaUrl('/HINH_TRANG_CHU/logo2.jpg')}
                            alt="IPWORLD Logo"
                            className={navbarStyles.logoImg}
                            onError={(e) => { (e.target as HTMLImageElement).src = resolveMediaUrl('/HINH_TRANG_CHU/ip.jpg'); }}
                        />
                        <h1
                            className={navbarStyles.logoTitle}
                            style={{ textShadow: '2px 3px 5px #A3C1AD' }}
                        >
                            IPWORLD
                        </h1>
                    </Link>

                    {isAdmin && (
                        <nav className={navbarStyles.adminNav}>
                            <Link href="/products/create" className={navPillClass(pathname === '/products/create')}>
                                + Thêm SP
                            </Link>
                            <Link href="/admin" className={navPillClass(pathname === '/admin')}>
                                Quản trị Admin
                            </Link>
                        </nav>
                    )}
                </div>

                <form onSubmit={handleSearchSubmit} className={navbarStyles.searchForm}>
                    <input
                        type="text"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        placeholder="Tìm kiếm sản phẩm..."
                        className={navbarStyles.searchInput}
                    />
                    <button type="submit" className={navbarStyles.searchBtn} title="Tìm kiếm">
                        <Search size={16} />
                    </button>
                </form>

                <div className={navbarStyles.rightGroup}>
                    {!isAdmin && (
                        <div className="relative">
                            <button
                                onClick={() => setIsCartOpen(!isCartOpen)}
                                className={navbarStyles.cartBtn}
                                title="Giỏ hàng"
                            >
                                <ShoppingCart size={18} />
                                {cartCount > 0 && (
                                    <span className={navbarStyles.cartBadge}>{cartCount}</span>
                                )}
                            </button>

                            {isCartOpen && (
                                <div className={navbarStyles.cartPopup}>
                                    <div className="p-4 bg-[#173B70] text-white flex justify-between items-center">
                                        <h3 className="font-bold text-base flex items-center gap-2">
                                            <ShoppingCart size={18} /> Giỏ Hàng Của Bạn
                                        </h3>
                                        <span className="text-xs font-semibold bg-white text-[#173B70] px-2.5 py-0.5 rounded-full">
                                            {cartCount} món
                                        </span>
                                    </div>

                                    <div className="max-h-72 overflow-y-auto p-3 divide-y divide-slate-100">
                                        {cartItems.length === 0 ? (
                                            <div className="text-center text-slate-500 py-10 text-sm">
                                                <p>Giỏ hàng đang trống.</p>
                                                <Link
                                                    href="/products"
                                                    onClick={() => setIsCartOpen(false)}
                                                    className="inline-block mt-3 text-xs text-[#0866e8] font-bold hover:underline"
                                                >
                                                    Mua sắm ngay →
                                                </Link>
                                            </div>
                                        ) : (
                                            cartItems.map((item) => (
                                                <div key={item.id} className="flex items-center gap-3 py-3 first:pt-1 last:pb-1">
                                                    <div className="w-16 h-16 bg-slate-50 border border-slate-200 rounded-xl overflow-hidden shrink-0 p-1 flex items-center justify-center">
                                                        {item.imageUrl ? (
                                                            <img
                                                                src={resolveMediaUrl(item.imageUrl)}
                                                                alt={item.name}
                                                                className="w-full h-full object-contain"
                                                                onError={(e) => { (e.target as HTMLImageElement).src = resolveMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png'); }}
                                                            />
                                                        ) : (
                                                            <Package size={24} className="text-slate-400" />
                                                        )}
                                                    </div>

                                                    <div className="flex-1 min-w-0 pr-1">
                                                        <p className="text-xs sm:text-sm font-bold text-slate-800 truncate" title={item.name}>
                                                            {item.name}
                                                        </p>
                                                        <p className="text-xs font-bold text-[#d70018] my-1">
                                                            {new Intl.NumberFormat('vi-VN').format(item.price)}đ
                                                        </p>

                                                        <div className="flex items-center bg-slate-100 rounded-full border border-slate-200 w-fit">
                                                            <button
                                                                onClick={(e) => { e.stopPropagation(); updateQuantity(item.id, item.quantity - 1); }}
                                                                disabled={item.quantity <= 1}
                                                                className="px-2 py-0.5 text-slate-600 hover:text-black hover:bg-slate-200 rounded-l-full disabled:opacity-40 transition-colors"
                                                            >
                                                                <Minus size={11} />
                                                            </button>
                                                            <span className="text-xs font-bold px-2 min-w-5 text-center text-slate-800">
                                                                {item.quantity}
                                                            </span>
                                                            <button
                                                                onClick={(e) => { e.stopPropagation(); updateQuantity(item.id, item.quantity + 1); }}
                                                                className="px-2 py-0.5 text-slate-600 hover:text-black hover:bg-slate-200 rounded-r-full transition-colors"
                                                            >
                                                                <Plus size={11} />
                                                            </button>
                                                        </div>
                                                    </div>

                                                    <button
                                                        onClick={() => removeFromCart(item.id)}
                                                        className="text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-full p-1.5 transition-all"
                                                        title="Xóa món"
                                                    >
                                                        <X size={16} />
                                                    </button>
                                                </div>
                                            ))
                                        )}
                                    </div>

                                    {cartItems.length > 0 && (
                                        <div className="p-4 border-t border-slate-100 bg-slate-50">
                                            <div className="flex justify-between items-center mb-3">
                                                <span className="text-xs font-semibold text-slate-600">Tổng thanh toán:</span>
                                                <span className="text-base font-extrabold text-[#d70018]">
                                                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(cartTotal)}
                                                </span>
                                            </div>
                                            <Link
                                                href="/orders"
                                                onClick={() => setIsCartOpen(false)}
                                                className="block w-full text-center bg-[#173B70] hover:bg-[#0866e8] text-white font-bold text-sm py-2.5 rounded-full transition-all shadow-md"
                                            >
                                                Tiến Hành Đặt Hàng
                                            </Link>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )}

                    <div className="relative group">
                        <button
                            onClick={() => setIsAccountMenuOpen(!isAccountMenuOpen)}
                            className={navbarStyles.accountBtn}
                            title={isLoggedIn ? (userName || 'Tài khoản') : 'Đăng nhập / Đăng ký'}
                        >
                            <User size={16} />
                            <span className="max-w-20 sm:max-w-28 truncate">{displayName}</span>
                        </button>

                        <div className={cx(navbarStyles.accountDropdown, accountDropdownVisibility)}>
                            {isLoggedIn ? (
                                <>
                                    <div className="px-4 py-2.5 border-b border-slate-100 bg-slate-50/80 rounded-t-2xl">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tài khoản</p>
                                        <p className="text-xs font-bold text-[#173B70] truncate mt-0.5">{userName}</p>
                                        <span className="inline-block mt-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-[#173B70]">
                                            {isAdmin ? 'Quản trị viên (Admin)' : 'Thành viên IPWORLD'}
                                        </span>
                                    </div>

                                    <div className="py-1">
                                        <Link
                                            href="/my-orders"
                                            onClick={() => setIsAccountMenuOpen(false)}
                                            className="flex items-center gap-2.5 px-4 py-2 text-xs font-semibold text-slate-700 hover:text-[#173B70] hover:bg-blue-50 transition-colors"
                                        >
                                            <Package size={15} className="text-[#173B70]" /> Đơn hàng của tôi
                                        </Link>
                                        <button
                                            onClick={() => { setIsAccountMenuOpen(false); handleOpenSettings(); }}
                                            className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-semibold text-slate-700 hover:text-[#173B70] hover:bg-blue-50 transition-colors text-left cursor-pointer"
                                        >
                                            <Settings size={15} className="text-[#173B70]" /> Cài đặt thông tin
                                        </button>
                                        <div className="my-1 border-t border-slate-100" />
                                        <button
                                            onClick={() => { setIsAccountMenuOpen(false); handleLogout(); }}
                                            className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-bold text-red-600 hover:bg-red-50 transition-colors text-left cursor-pointer"
                                        >
                                            <LogOut size={15} /> Đăng xuất
                                        </button>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div className="px-4 py-3 border-b border-slate-100 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-t-2xl">
                                        <p className="text-xs font-extrabold text-[#173B70] flex items-center gap-1.5">
                                            <User size={15} /> Mục Tài Khoản
                                        </p>
                                        <p className="text-[11px] text-slate-500 mt-1">
                                            Đăng nhập hoặc đăng ký tài khoản để mua sắm dễ dàng hơn!
                                        </p>
                                    </div>

                                    <div className="p-3 space-y-2">
                                        <Link
                                            href="/login"
                                            onClick={() => setIsAccountMenuOpen(false)}
                                            className="flex items-center justify-center gap-2 w-full py-2 px-3 text-xs font-bold text-white bg-[#173B70] hover:bg-[#0866e8] rounded-xl shadow-sm transition-all"
                                        >
                                            <LogIn size={14} /> Đăng nhập
                                        </Link>
                                        <Link
                                            href="/register"
                                            onClick={() => setIsAccountMenuOpen(false)}
                                            className="flex items-center justify-center gap-2 w-full py-2 px-3 text-xs font-bold text-[#173B70] bg-blue-50 hover:bg-blue-100 border border-[#173B70]/30 rounded-xl transition-all"
                                        >
                                            <UserPlus size={14} /> Đăng ký thành viên
                                        </Link>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>

                    <button
                        onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                        className={navbarStyles.hamburger}
                        title="Menu"
                    >
                        <Menu size={18} />
                    </button>
                </div>
            </div>

            {isMobileMenuOpen && (
                <div className={navbarStyles.mobilePanel}>
                    <form
                        onSubmit={handleSearchSubmit}
                        className="flex sm:hidden items-center border-2 border-[#173B70] rounded-2xl overflow-hidden h-8 bg-white mb-3"
                    >
                        <input
                            type="text"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            placeholder="Tìm kiếm sản phẩm..."
                            className="w-full px-3 text-xs text-slate-800 placeholder-slate-400 outline-none border-none bg-transparent"
                        />
                        <button type="submit" className="bg-[#173B70] text-white h-full px-3 flex items-center justify-center">
                            <Search size={14} />
                        </button>
                    </form>

                    <div className="grid grid-cols-2 gap-2 pb-3 mb-3 border-b border-slate-200 text-xs font-bold text-[#173B70]">
                        <Link href="/pc" onClick={() => setIsMobileMenuOpen(false)} className="p-2 rounded-xl bg-blue-50 hover:bg-blue-100 flex items-center gap-1.5">
                            🖥️ PC
                        </Link>
                        <Link href="/laptop" onClick={() => setIsMobileMenuOpen(false)} className="p-2 rounded-xl bg-blue-50 hover:bg-blue-100 flex items-center gap-1.5">
                            💻 Laptop
                        </Link>
                        <Link href="/phu-kien" onClick={() => setIsMobileMenuOpen(false)} className="p-2 rounded-xl bg-blue-50 hover:bg-blue-100 flex items-center gap-1.5">
                            🎧 Phụ kiện
                        </Link>
                        <Link href="/man-hinh" onClick={() => setIsMobileMenuOpen(false)} className="p-2 rounded-xl bg-blue-50 hover:bg-blue-100 flex items-center gap-1.5">
                            🖥️ Màn hình
                        </Link>
                        <Link href="/may-tinh-bang" onClick={() => setIsMobileMenuOpen(false)} className="p-2 rounded-xl bg-blue-50 hover:bg-blue-100 flex items-center gap-1.5">
                            📱 Máy tính bảng
                        </Link>
                        <Link href="/dien-thoai" onClick={() => setIsMobileMenuOpen(false)} className="p-2 rounded-xl bg-blue-50 hover:bg-blue-100 flex items-center gap-1.5">
                            📱 Điện thoại
                        </Link>
                    </div>

                    {isAdmin && (
                        <nav className="flex flex-col gap-1 text-sm font-bold text-[#173B70]">
                            <Link href="/products/create" onClick={() => setIsMobileMenuOpen(false)} className="px-3 py-2 rounded-xl hover:bg-blue-50 transition-colors">
                                ➕ Thêm sản phẩm mới
                            </Link>
                            <Link href="/admin" onClick={() => setIsMobileMenuOpen(false)} className="px-3 py-2 rounded-xl hover:bg-blue-50 transition-colors">
                                🛡️ Quản trị Admin
                            </Link>
                        </nav>
                    )}
                </div>
            )}

            {(isCartOpen || isAccountMenuOpen || isMobileMenuOpen) && (
                <div className={navbarStyles.overlay} onClick={closeAllMenus} />
            )}

            {isSettingsOpen && (
                <div className={modalStyles.backdrop}>
                    <div className={modalStyles.card}>
                        <div className={modalStyles.header}>
                            <h3 className={modalStyles.headerTitle}>Cài Đặt Tài Khoản</h3>
                            <button onClick={() => setIsSettingsOpen(false)} className={modalStyles.closeBtn}>
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSettingsUpdate} className={modalStyles.body}>
                            <div>
                                <label className={formStyles.label}>Email mới</label>
                                <input
                                    type="email"
                                    value={settingsForm.email}
                                    onChange={(e) => setSettingsForm({ ...settingsForm, email: e.target.value })}
                                    placeholder={userName || 'Nhập email...'}
                                    className={formStyles.inputSettings}
                                />
                            </div>
                            <div>
                                <label className={formStyles.label}>Ngày sinh</label>
                                <input
                                    type="date"
                                    value={settingsForm.dob}
                                    onChange={(e) => setSettingsForm({ ...settingsForm, dob: e.target.value })}
                                    className={formStyles.inputSettings}
                                />
                            </div>
                            <div>
                                <label className={formStyles.label}>Số điện thoại</label>
                                <input
                                    type="tel"
                                    value={settingsForm.phone}
                                    onChange={(e) => setSettingsForm({ ...settingsForm, phone: e.target.value })}
                                    placeholder="0987654321"
                                    className={formStyles.inputSettings}
                                />
                            </div>

                            <div className={modalStyles.footer}>
                                <button type="button" onClick={() => setIsSettingsOpen(false)} className={modalStyles.cancelBtn}>
                                    Đóng
                                </button>
                                <button
                                    type="submit"
                                    disabled={isUpdatingSettings}
                                    className={cx(modalStyles.saveBtn, isUpdatingSettings ? 'bg-slate-400' : 'bg-[#173B70] hover:bg-[#0866e8]')}
                                >
                                    {isUpdatingSettings ? 'Đang lưu...' : 'Lưu Thay Đổi'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </header>
    );
}
