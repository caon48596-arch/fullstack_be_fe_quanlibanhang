"use client";

import { DollarSign, Package, ShoppingBag, Users, Calendar, Plus, ArrowUpRight, Search } from "lucide-react";
import { useState, useEffect } from "react";
import Link from "next/link";
import { fetchClient } from "../../util/fetchClient";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from "recharts";

export default function AdminDashboard() {
  const [dashboardData, setDashboardData] = useState({
    totalRevenue: 0,
    newOrders: 0,
    totalProducts: 0,
    totalCustomers: 0,
    dailyRevenue: [] as any[],
    recentOrders: [] as any[]
  });
  const [isLoading, setIsLoading] = useState(true);
  const [dateSearch, setDateSearch] = useState("");

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await fetchClient("/dashboard/stats");
        if (response && response.data) {
          setDashboardData({
            totalRevenue: response.data.totalRevenue || 0,
            newOrders: response.data.newOrders || 0,
            totalProducts: response.data.totalProducts || 0,
            totalCustomers: response.data.totalCustomers || 0,
            dailyRevenue: response.data.dailyRevenue || [],
            recentOrders: response.data.recentOrders || []
          });
        }
      } catch (error) {
        console.error("Lỗi khi tải thống kê:", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchStats();
  }, []);

  const stats = [
    { name: "Tổng Doanh Thu", value: `${dashboardData.totalRevenue.toLocaleString()} ₫`, icon: DollarSign, color: "text-green-600", bg: "bg-green-100" },
    { name: "Tổng Số Đơn", value: dashboardData.newOrders.toString(), icon: ShoppingBag, color: "text-blue-600", bg: "bg-blue-100" },
    { name: "Sản Phẩm", value: dashboardData.totalProducts.toString(), icon: Package, color: "text-amber-600", bg: "bg-amber-100" },
    { name: "Khách Hàng", value: dashboardData.totalCustomers.toString(), icon: Users, color: "text-purple-600", bg: "bg-purple-100" },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-700"></div>
      </div>
    );
  }

  const chartData = [...dashboardData.dailyRevenue]
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .map((item: any) => ({
      name: new Date(item.date).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      Revenue: item.revenue
    }));

  const dailyList = dashboardData.dailyRevenue || [];
  const totalDays = dailyList.length;
  const avgDailyRevenue = totalDays > 0 ? dashboardData.totalRevenue / totalDays : 0;
  const maxDay = dailyList.reduce((max: any, cur: any) => (!max || cur.revenue > max.revenue ? cur : max), null);

  const filteredDailyRevenue = dailyList.filter((item: any) => {
    if (!dateSearch.trim()) return true;
    const formatted = new Date(item.date).toLocaleDateString('vi-VN');
    return formatted.includes(dateSearch.trim()) || String(item.date).includes(dateSearch.trim());
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-bold text-gray-800">Tổng quan hệ thống</h2>
          <p className="text-xs text-gray-500 mt-0.5">Theo dõi doanh thu chi tiết theo ngày và quản trị bán hàng</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href="/admin/products/create"
            className="flex items-center gap-1.5 bg-amber-600 hover:bg-amber-700 text-white px-3.5 py-2 rounded-lg font-bold text-xs transition-colors shadow-sm"
          >
            <Plus size={16} /> Thêm Sản Phẩm
          </Link>
          <Link
            href="/admin/products"
            className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-900 text-white px-3.5 py-2 rounded-lg font-bold text-xs transition-colors shadow-sm"
          >
            <Package size={16} /> Danh Sách SP
          </Link>
          <a
            href="#daily-revenue"
            className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white px-3.5 py-2 rounded-lg font-bold text-xs transition-colors shadow-sm"
          >
            <Calendar size={16} /> Xem Doanh Thu Ngày
          </a>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        {stats.map((stat, idx) => {
          const Icon = stat.icon;
          return (
            <div key={idx} className="bg-white rounded-xl shadow-sm p-5 flex items-center space-x-4 border border-gray-100 transition-all hover:shadow-md">
              <div className={`p-3.5 rounded-full shrink-0 ${stat.bg} ${stat.color}`}>
                <Icon size={22} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold text-gray-500 truncate">{stat.name}</p>
                <p className="text-lg sm:text-xl font-bold text-gray-900 truncate mt-0.5">{stat.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 sm:p-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-4">
          <div>
            <h3 className="text-base sm:text-lg font-bold text-gray-800">Biểu đồ doanh số theo ngày</h3>
            <p className="text-xs text-gray-500">Biểu diễn trực quan tốc độ tăng trưởng doanh số</p>
          </div>
          <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
            Tổng {chartData.length} mốc thời gian
          </span>
        </div>
        <div className="h-[300px] sm:h-[340px] w-full">
          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 20, right: 20, left: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 11 }} dy={10} />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#64748b', fontSize: 11 }}
                  tickFormatter={(value) => `${(value / 1000000).toFixed(1)}M`}
                />
                <Tooltip
                  cursor={{ fill: '#f8fafc' }}
                  formatter={(value) => [`${Number(value ?? 0).toLocaleString()} ₫`, 'Doanh thu'] as [string, string]}
                  contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)', fontSize: '12px' }}
                />
                <Bar dataKey="Revenue" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={45} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400 text-sm">
              Chưa có dữ liệu doanh thu
            </div>
          )}
        </div>
      </div>

      <div id="daily-revenue" className="bg-white rounded-xl shadow-sm border-2 border-emerald-500/20 p-5 sm:p-6 scroll-mt-6">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 pb-4 mb-4 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-100 text-emerald-700">
              <Calendar size={20} />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-black text-gray-800 flex items-center gap-2">
                <span>Bảng Doanh Thu Chi Tiết Theo Ngày</span>
                <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                  Chi tiết
                </span>
              </h3>
              <p className="text-xs text-gray-500">Xem số tiền bán được và số lượng đơn hàng theo từng ngày cụ thể</p>
            </div>
          </div>

          <div className="relative w-full md:w-64">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              value={dateSearch}
              onChange={(e) => setDateSearch(e.target.value)}
              placeholder="Lọc theo ngày (dd/mm/yyyy)..."
              className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-5">
          <div className="bg-slate-50 border border-slate-200/80 rounded-xl p-3.5">
            <p className="text-xs font-semibold text-slate-500">Doanh thu trung bình/ngày</p>
            <p className="text-base sm:text-lg font-black text-slate-800 mt-1">
              {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(avgDailyRevenue)}
            </p>
          </div>
          <div className="bg-emerald-50 border border-emerald-200/80 rounded-xl p-3.5">
            <p className="text-xs font-semibold text-emerald-700">Ngày cao điểm nhất</p>
            <p className="text-base sm:text-lg font-black text-emerald-800 mt-1">
              {maxDay ? `${new Date(maxDay.date).toLocaleDateString('vi-VN')} (${new Intl.NumberFormat('vi-VN').format(maxDay.revenue)}đ)` : "Chưa có"}
            </p>
          </div>
          <div className="bg-blue-50 border border-blue-200/80 rounded-xl p-3.5">
            <p className="text-xs font-semibold text-blue-700">Tổng số ngày ghi nhận</p>
            <p className="text-base sm:text-lg font-black text-blue-800 mt-1">
              {totalDays} ngày
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[600px]">
            <thead>
              <tr className="border-b border-gray-200 text-gray-500 text-xs uppercase bg-gray-50/70">
                <th className="py-2.5 px-3 font-semibold">STT</th>
                <th className="py-2.5 px-3 font-semibold">Ngày ghi nhận</th>
                <th className="py-2.5 px-3 font-semibold">Doanh thu (VNĐ)</th>
                <th className="py-2.5 px-3 font-semibold">Số đơn hàng</th>
                <th className="py-2.5 px-3 font-semibold">Tỷ trọng</th>
                <th className="py-2.5 px-3 font-semibold">Đánh giá</th>
              </tr>
            </thead>
            <tbody className="text-xs divide-y divide-gray-100">
              {filteredDailyRevenue.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-gray-400">
                    Không tìm thấy dữ liệu doanh thu ngày phù hợp
                  </td>
                </tr>
              ) : (
                filteredDailyRevenue.map((item: any, idx: number) => {
                  const itemRevenue = item.revenue || 0;
                  const ratio = dashboardData.totalRevenue > 0
                    ? ((itemRevenue / dashboardData.totalRevenue) * 100).toFixed(1)
                    : "0";
                  const orderCount = item.orderCount !== undefined ? item.orderCount : "-";
                  const isHigh = maxDay && item.date === maxDay.date;

                  return (
                    <tr key={idx} className="hover:bg-gray-50 transition-colors">
                      <td className="py-3 px-3 text-gray-500 font-medium">{idx + 1}</td>
                      <td className="py-3 px-3 font-bold text-gray-800">
                        {new Date(item.date).toLocaleDateString('vi-VN', {
                          weekday: 'short',
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit'
                        })}
                      </td>
                      <td className="py-3 px-3 font-black text-emerald-700">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(itemRevenue)}
                      </td>
                      <td className="py-3 px-3 text-gray-700 font-semibold">
                        {orderCount} đơn
                      </td>
                      <td className="py-3 px-3 text-gray-600 font-medium">
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-2 bg-gray-100 rounded-full overflow-hidden">
                            <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${Math.min(Number(ratio), 100)}%` }} />
                          </div>
                          <span>{ratio}%</span>
                        </div>
                      </td>
                      <td className="py-3 px-3">
                        {isHigh ? (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200">
                            ⭐ Kỷ lục
                          </span>
                        ) : itemRevenue >= avgDailyRevenue ? (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                            Cao
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-200">
                            Ổn định
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 sm:p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-base sm:text-lg font-bold text-gray-800">Đơn hàng gần đây</h3>
          <Link href="/admin/orders" className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1">
            Xem tất cả <ArrowUpRight size={13} />
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[600px]">
            <thead>
              <tr className="border-b border-gray-100 text-gray-500 text-xs">
                <th className="pb-3 font-semibold">Mã ĐH</th>
                <th className="pb-3 font-semibold">Khách hàng</th>
                <th className="pb-3 font-semibold">Ngày đặt</th>
                <th className="pb-3 font-semibold">Tổng tiền</th>
                <th className="pb-3 font-semibold">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="text-xs">
              {dashboardData.recentOrders.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-gray-500">Chưa có đơn hàng nào</td>
                </tr>
              ) : (
                dashboardData.recentOrders.map((order: any, idx: number) => (
                  <tr key={order.id || idx} className="border-b border-gray-50 last:border-0 hover:bg-gray-50 transition-colors">
                    <td className="py-3 text-gray-900 font-medium">#{order.id}</td>
                    <td className="py-3 text-gray-600 truncate max-w-[150px]">
                      {order.user?.username || order.user?.email || "Khách vô danh"}
                    </td>
                    <td className="py-3 text-gray-500">
                      {new Date(order.orderDate).toLocaleString('vi-VN')}
                    </td>
                    <td className="py-3 text-emerald-700 font-bold">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.totalAmount || 0)}
                    </td>
                    <td className="py-3">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        order.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-700' :
                        order.status === 'CANCELLED' ? 'bg-red-100 text-red-700' :
                        'bg-amber-100 text-amber-700'
                      }`}>
                        {order.status === 'APPROVED' ? 'Đã Duyệt' : order.status === 'CANCELLED' ? 'Đã Hủy' : 'Chờ Duyệt'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
