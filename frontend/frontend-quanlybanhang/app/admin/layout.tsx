"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { LayoutDashboard, Package, ShoppingCart, LogOut, ShieldAlert, TrendingUp } from "lucide-react";
import { getUserRoleFromToken } from "../../util/fetchClient";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isAuthorized, setIsAuthorized] = useState<boolean | null>(null);

  useEffect(() => {
    // Kiểm tra quyền Admin từ Token JWT
    const token = typeof window !== 'undefined'
      ? (sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken'))
      : null;
    const role = getUserRoleFromToken();
    if (role === 'ROLE_ADMIN' || role === 'ADMIN') {
      setIsAuthorized(true);
    } else {
      setIsAuthorized(false);
      // Chuyển hướng sau 1.5 giây: nếu chưa đăng nhập -> /login, nếu đã là người dùng thường -> / (không đá ra login)
      const timer = setTimeout(() => {
        if (!token) {
          router.push('/login?redirect=/admin');
        } else {
          router.push('/');
        }
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [router]);

  // Trạng thái đang kiểm tra phiên đăng nhập
  if (isAuthorized === null) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-slate-900 text-white">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-amber-400 mb-4"></div>
        <p className="text-slate-300">Đang xác thực quyền truy cập quản trị...</p>
      </div>
    );
  }

  // Không có quyền truy cập
  if (!isAuthorized) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-slate-900 text-white p-4">
        <ShieldAlert size={56} className="text-red-500 mb-4" />
        <h2 className="text-2xl font-bold mb-2">Truy cập bị từ chối</h2>
        <p className="text-slate-400 mb-6 text-center max-w-md">
          Bạn cần đăng nhập bằng tài khoản Quản trị viên (Admin) để xem trang này. Đang chuyển hướng về trang đăng nhập...
        </p>
        <Link
          href="/login"
          className="bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold px-6 py-2.5 rounded-lg transition-colors"
        >
          Đăng nhập ngay
        </Link>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-100 w-full">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col">
        <div className="p-6 border-b border-slate-800">
          <h2 className="text-2xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-400 to-amber-200">
            IPWORLD Admin
          </h2>
        </div>
        <nav className="flex-1 p-4 space-y-2">
          <Link href="/admin" className="flex items-center space-x-3 p-3 rounded-lg hover:bg-slate-800 hover:text-white transition-colors">
            <LayoutDashboard size={20} /> <span>Dashboard</span>
          </Link>
          <Link href="/admin#daily-revenue" className="flex items-center space-x-3 p-3 rounded-lg hover:bg-slate-800 hover:text-white transition-colors">
            <TrendingUp size={20} /> <span>Doanh thu theo ngày</span>
          </Link>
          <Link href="/admin/products" className="flex items-center space-x-3 p-3 rounded-lg hover:bg-slate-800 hover:text-white transition-colors">
            <Package size={20} /> <span>Sản phẩm</span>
          </Link>
          <Link href="/admin/orders" className="flex items-center space-x-3 p-3 rounded-lg hover:bg-slate-800 hover:text-white transition-colors">
            <ShoppingCart size={20} /> <span>Đơn hàng & Khách hàng</span>
          </Link>
          <Link href="/admin/coupons" className="flex items-center space-x-3 p-3 rounded-lg hover:bg-slate-800 hover:text-white transition-colors">
            <Package size={20} /> <span>Mã giảm giá</span>
          </Link>
        </nav>
        <div className="p-4 border-t border-slate-800">
          <Link href="/" className="flex items-center space-x-3 p-3 rounded-lg hover:bg-red-500/20 text-red-400 transition-colors">
            <LogOut size={20} /> <span>Về trang chủ</span>
          </Link>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <header className="h-16 bg-white shadow-sm flex items-center justify-between px-8">
          <h1 className="text-xl font-semibold text-gray-800">Quản trị viên</h1>
          <div className="flex items-center space-x-4">
            <div className="w-10 h-10 rounded-full bg-amber-500 flex items-center justify-center font-bold text-white shadow-md">
              A
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 p-8 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
