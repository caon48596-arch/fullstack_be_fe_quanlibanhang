/**
 * styles/constants.ts
 * 
 * Quản lý tập trung các quy chuẩn className giao diện (Design System Tokens).
 * Chuẩn Clean Code: Tên biến tự giải thích (Intention-Revealing Names),
 * tránh viết tắt khó hiểu (btn, cls) và tránh xung đột với HTML/Next.js tags.
 */

/* ──────────────────────────────────────────────
 *  1. Bảng màu thương hiệu IPWORLD (Brand Colors)
 * ────────────────────────────────────────────── */
export const brandColors = {
    primary:   '#173B70',
    hover:     '#0866e8',
    danger:    '#d70018',
    lightBg:   '#f8fafc',
} as const;

/* ──────────────────────────────────────────────
 *  2. Layout Xác thực (Login / Register / Verify OTP)
 * ────────────────────────────────────────────── */
export const authLayoutStyles = {
    /** Khung ngoài cùng: toàn màn hình và căn giữa */
    page: 'flex-1 flex items-center justify-center py-10 px-4',

    /** Khung chứa cặp [Logo thương hiệu – Form đăng nhập] */
    container: 'w-full max-w-4xl flex flex-col md:flex-row items-center justify-center gap-10',

    /** Cột bọc logo bên trái */
    logoWrapper: 'flex flex-col items-center justify-center',

    /** Ảnh logo thương hiệu */
    logoImage: 'w-48 sm:w-64 md:w-80 object-contain drop-shadow-[0_10px_20px_rgba(23,59,112,0.5)]',

    /** Chữ thương hiệu bên dưới logo */
    logoText: 'text-3xl font-black text-white mt-4 tracking-wider',

    /** Khung thẻ trắng (Card) chứa form nhập liệu */
    card: [
        'bg-white rounded-2xl p-8 sm:p-10',
        'w-full max-w-md',
        'border-2 border-blue-900/10',
        'shadow-[5px_5px_15px_#173B70]',
    ].join(' '),

    /** Tiêu đề chính của Card */
    cardTitle: 'text-xl sm:text-2xl font-black text-[#173B70] text-center mb-6 tracking-wide',
} as const;

/* ──────────────────────────────────────────────
 *  3. Các phần tử Form nhập liệu (Form Elements)
 * ────────────────────────────────────────────── */
export const formStyles = {
    /** Nhãn trường thông thường */
    label: 'block text-xs font-bold text-black mb-1',

    /** Nhãn trường có kèm icon Lucide */
    labelWithIcon: 'text-xs font-bold text-black mb-1 flex items-center gap-1.5',

    /** Ô nhập liệu chuẩn thương hiệu (chỉnh sửa được) */
    input: [
        'w-full px-3.5 py-2.5 text-sm',
        'border-2 border-[#173B70] rounded-lg',
        'outline-none focus:border-[#0866e8]',
        'text-black transition-colors',
    ].join(' '),

    /** Ô nhập liệu chỉ đọc (Read Only / Disabled) */
    inputReadOnly: [
        'w-full px-3.5 py-2.5 text-sm',
        'border-2 border-slate-200 bg-slate-50 rounded-lg',
        'text-black font-semibold',
        'cursor-not-allowed outline-none',
    ].join(' '),

    /** Ô nhập liệu trong Modal cài đặt người dùng */
    inputSettings: [
        'w-full px-3 py-2 text-sm',
        'border-2 border-slate-200 rounded-xl',
        'outline-none focus:border-[#173B70]',
        'text-black',
    ].join(' '),

    /** Ô nhập liệu khi có lỗi */
    inputError: [
        'w-full px-3.5 py-2.5 text-sm',
        'border-2 border-red-500 rounded-lg bg-red-50/30',
        'outline-none focus:border-red-600',
        'text-black transition-colors',
    ].join(' '),
} as const;

/* ──────────────────────────────────────────────
 *  4. Nút bấm thao tác (Buttons)
 * ────────────────────────────────────────────── */
export const buttonStyles = {
    /** Nút chính (Submit action) trải rộng 100% */
    primary: [
        'w-full py-2.5 rounded-lg',
        'font-black text-sm text-white',
        'shadow-md transition-all mt-2 cursor-pointer',
    ].join(' '),

    /** Trạng thái kích hoạt (Active / Hover) */
    primaryActive: 'bg-[#173B70] hover:bg-[#0866e8] active:scale-[0.98]',

    /** Trạng thái vô hiệu hóa (Disabled / Loading) */
    primaryDisabled: 'bg-slate-400 cursor-not-allowed',

    /** Nút phụ gửi lại OTP bên trong ô nhập */
    otpAction: [
        'px-3.5 py-2.5 rounded-lg',
        'text-xs font-bold text-white',
        'transition-all whitespace-nowrap',
        'flex items-center gap-1 cursor-pointer',
    ].join(' '),

    /** Nút phụ viền (Outline button) */
    outline: [
        'px-4 py-2 rounded-lg border-2 border-[#173B70]',
        'text-[#173B70] font-bold text-xs',
        'hover:bg-[#173B70] hover:text-white transition-colors cursor-pointer',
    ].join(' '),

    /** Nút nguy hiểm (Danger / Delete) */
    danger: [
        'px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700',
        'text-white font-bold text-xs shadow-sm transition-colors cursor-pointer',
    ].join(' '),
} as const;

/* ──────────────────────────────────────────────
 *  5. Hộp thông báo (Alerts)
 * ────────────────────────────────────────────── */
export const alertStyles = {
    error:   'p-3 rounded-lg mb-4 text-xs font-bold bg-red-100 text-red-700 border border-red-200',
    success: 'p-3 rounded-lg mb-4 text-xs font-bold bg-green-100 text-green-700 border border-green-200',
    info:    'p-3 rounded-lg mb-4 text-xs font-bold bg-blue-100 text-[#173B70] border border-blue-200',
    warning: 'p-3 rounded-lg mb-4 text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200',
} as const;

/* ──────────────────────────────────────────────
 *  6. Đường dẫn điều hướng (Links)
 * ────────────────────────────────────────────── */
export const linkStyles = {
    /** Link nhấn mạnh (Đăng ký ngay / Đăng nhập) */
    accent:    'font-bold text-[#173B70] hover:text-[#0866e8] hover:underline cursor-pointer',

    /** Link làm mờ (Về trang chủ) */
    muted:     'font-bold text-slate-500 hover:text-[#173B70] hover:underline cursor-pointer',

    /** Link dưới footer form */
    footerNav: 'text-xs font-bold text-[#173B70] hover:text-[#0866e8] hover:underline cursor-pointer',
} as const;

/* ──────────────────────────────────────────────
 *  7. Thanh điều hướng đầu trang (Navbar)
 * ────────────────────────────────────────────── */
export const navbarStyles = {
    /** Khung header ngoài cùng */
    header: 'w-full max-w-[1400px] mx-auto select-none',

    /** Thanh navbar chính bo tròn đổ bóng */
    bar: [
        'bg-white rounded-2xl px-4 md:px-8 py-3',
        'flex items-center justify-between',
        'shadow-[5px_5px_15px_#173B70]',
        'transition-all relative',
    ].join(' '),

    /** Nhóm logo và menu admin bên trái */
    leftGroup: 'flex items-center gap-4 lg:gap-6',

    /** Khung liên kết logo */
    logoLink: [
        'flex items-center gap-3 group',
        'cursor-pointer transition-transform duration-200',
        'active:scale-95',
    ].join(' '),

    /** Ảnh đại diện logo */
    logoImg: [
        'w-12 h-12 md:w-14 md:h-14',
        'rounded-2xl object-cover',
        'border-2 border-[#173B70]/20 shadow-sm',
        'transition-transform group-hover:scale-105',
    ].join(' '),

    /** Tiêu đề chữ IPWORLD */
    logoTitle: [
        'text-xl md:text-2xl font-black',
        'text-[#173B70] tracking-wide',
        'group-hover:opacity-90 transition-opacity',
    ].join(' '),

    /** Danh mục quản trị viên desktop */
    adminNav: 'hidden md:flex items-center gap-1.5 lg:gap-2 text-xs md:text-sm font-bold text-[#173B70]',

    /** Nút điều hướng hình viên thuốc (Pill) */
    navPill: 'px-2.5 py-1 md:px-3 md:py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer whitespace-nowrap',
    navPillActive:   'bg-[#173B70] text-white shadow-sm',
    navPillDefault:  'hover:bg-blue-50 text-[#173B70]',

    /** Khung tìm kiếm sản phẩm (thu nhỏ gọn gàng) */
    searchForm: [
        'hidden sm:flex items-center',
        'border-2 border-[#173B70] rounded-2xl overflow-hidden',
        'h-8 md:h-9 bg-white shadow-inner',
        'w-36 md:w-52 shrink-0 mx-2 lg:mx-3',
    ].join(' '),

    searchInput: [
        'w-full px-4 text-xs md:text-sm',
        'text-black placeholder-slate-400',
        'outline-none border-none bg-transparent',
    ].join(' '),

    searchBtn: [
        'bg-[#173B70] hover:bg-[#0866e8]',
        'text-white h-full px-3.5',
        'flex items-center justify-center',
        'transition-colors shrink-0 cursor-pointer',
    ].join(' '),

    /** Nhóm giỏ hàng, người dùng bên phải */
    rightGroup: 'flex items-center gap-2.5 sm:gap-3',

    /** Nút giỏ hàng */
    cartBtn: [
        'flex items-center justify-center',
        'w-11 h-8 rounded-2xl',
        'bg-[#173B70] hover:bg-[#0866e8]',
        'text-white shadow-sm transition-all',
        'relative cursor-pointer',
    ].join(' '),

    /** Huy hiệu số lượng trên giỏ hàng */
    cartBadge: [
        'absolute -top-1.5 -right-1.5',
        'bg-red-600 text-white text-[11px] font-black',
        'h-5 w-5 rounded-full',
        'flex items-center justify-center',
        'border-2 border-white shadow',
    ].join(' '),

    /** Hộp xem nhanh giỏ hàng thả xuống (Popup) */
    cartPopup: [
        'absolute right-0 mt-3 w-80 sm:w-96',
        'bg-white rounded-2xl overflow-hidden z-50',
        'border border-slate-200',
        'shadow-[5px_5px_20px_rgba(23,59,112,0.4)]',
        'animate-in fade-in zoom-in-95 duration-150',
    ].join(' '),

    /** Nút tài khoản người dùng */
    accountBtn: [
        'flex items-center gap-1.5',
        'px-3.5 py-1.5 rounded-2xl',
        'bg-[#173B70] hover:bg-[#0866e8]',
        'text-white text-xs sm:text-sm font-bold',
        'shadow-sm transition-all cursor-pointer',
    ].join(' '),

    /** Menu thả xuống của tài khoản */
    accountDropdown: [
        'absolute right-0 mt-2 w-64',
        'bg-white border border-slate-200 rounded-2xl',
        'shadow-[0_10px_30px_rgba(23,59,112,0.25)]',
        'py-2 transition-all z-50',
    ].join(' '),

    /** Nút menu trên điện thoại (Hamburger button) */
    hamburger: [
        'lg:hidden flex items-center justify-center',
        'w-9 h-8 rounded-xl',
        'bg-slate-100 hover:bg-slate-200',
        'text-[#173B70] transition-colors cursor-pointer',
    ].join(' '),

    /** Bảng menu mở rộng trên điện thoại */
    mobilePanel: [
        'lg:hidden mt-2 p-4',
        'bg-white rounded-2xl',
        'border border-slate-200',
        'shadow-[0_8px_20px_rgba(23,59,112,0.25)]',
        'animate-in fade-in slide-in-from-top-2 duration-150',
    ].join(' '),

    /** Lớp phủ trong suốt đóng menu khi nhấp ngoài */
    overlay: 'fixed inset-0 z-40 bg-transparent',
} as const;

/* ──────────────────────────────────────────────
 *  8. Hộp thoại nổi (Modal Dialogs)
 * ────────────────────────────────────────────── */
export const modalStyles = {
    /** Phông nền làm tối và làm mờ */
    backdrop: 'fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs',

    /** Khung thẻ Modal */
    card: [
        'bg-white rounded-2xl overflow-hidden relative',
        'w-full max-w-md',
        'border border-slate-200',
        'shadow-[5px_5px_25px_#173B70]',
    ].join(' '),

    header:      'p-4 bg-[#173B70] text-white flex justify-between items-center',
    headerTitle: 'font-bold text-base',
    closeBtn:    'text-white/80 hover:text-white transition-colors cursor-pointer',

    body: 'p-6 space-y-4',

    footer:     'pt-3 flex gap-3',
    cancelBtn:  'flex-1 py-2 text-xs font-bold rounded-full text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer',
    saveBtn:    'flex-1 py-2 text-xs font-bold rounded-full text-white shadow-md transition-all cursor-pointer',
} as const;

/* ──────────────────────────────────────────────
 *  9. Chân trang (Footer)
 * ────────────────────────────────────────────── */
export const footerStyles = {
    /** Wrapper toàn chân trang */
    wrapper: 'w-[98%] max-w-[1400px] mx-auto my-6 bg-[#173B70] text-white p-6 md:p-10 rounded-[20px] shadow-[5px_5px_15px_#173B70] box-border border border-blue-900/40',

    /** Bố cục 3 cột responsive */
    grid: 'grid grid-cols-1 md:grid-cols-3 gap-8',

    /** Tiêu đề cột */
    columnTitle: 'text-xl font-bold mb-3 pb-2 border-b-2 border-white tracking-wide',

    /** Mô tả thương hiệu */
    description: 'text-sm leading-relaxed text-blue-100 mb-2',

    /** Cam kết chất lượng */
    commitment: 'text-sm font-semibold text-emerald-300',

    /** Danh sách mục hỗ trợ / liên hệ */
    list: 'space-y-3 text-sm text-blue-100',

    /** Mục trong danh sách hỗ trợ */
    listItem: 'flex items-center gap-2 hover:text-white transition-colors cursor-pointer',

    /** Icon hình ảnh nhỏ */
    icon: 'w-6 h-6 object-contain flex-shrink-0',

    /** Thanh bản quyền dưới đáy */
    bottomBar: 'mt-8 pt-4 border-t border-blue-400/40 text-center text-xs text-blue-200',
} as const;

/* ──────────────────────────────────────────────
 *  10. Thẻ sản phẩm (Product Card Styles)
 * ────────────────────────────────────────────── */
export const productCardStyles = {
    card: [
        'bg-white rounded-[20px] p-4 flex flex-col justify-between',
        'border border-blue-900/10 shadow-[5px_5px_15px_#173B70]',
        'hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 group',
    ].join(' '),

    imageContainer: 'w-full h-44 overflow-hidden rounded-xl bg-slate-50 flex items-center justify-center p-2 mb-3',
    image: 'w-full h-full object-contain group-hover:scale-105 transition-transform duration-300',
    title: 'font-bold text-sm text-[#173B70] line-clamp-2 min-h-[38px] group-hover:text-[#0866e8] transition-colors',
    price: 'text-[#d70018] font-black text-lg my-1',
    description: 'text-xs text-slate-500 line-clamp-2 min-h-[2rem]',
    addToCartBtn: 'w-full py-2.5 rounded-[8px] bg-[#0866e8] hover:bg-[#173B70] text-white font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-sm cursor-pointer',
} as const;

/* ──────────────────────────────────────────────
 *  Backward Compatibility Aliases (Tương thích ngược)
 *  Giúp code cũ chưa cập nhật import vẫn hoạt động an toàn
 * ────────────────────────────────────────────── */
export const BRAND       = brandColors;
export const authLayout  = authLayoutStyles;
export const form        = formStyles;
export const btn         = buttonStyles;
export const alert       = alertStyles;
export const link        = linkStyles;
export const navbar      = navbarStyles;
export const modal       = modalStyles;
export const footer      = footerStyles;
