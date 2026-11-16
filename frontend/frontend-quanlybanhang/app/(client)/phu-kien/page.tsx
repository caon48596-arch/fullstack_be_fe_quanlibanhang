"use client";

import React from 'react';
import CategoryProductPage from '@/components/CategoryProductPage';

export default function PhuKienPage() {
    return (
        <CategoryProductPage
            categoryName="Phụ kiện"
            categorySlug="phu-kien"
            categoryAliases={['Phụ kiện', 'Phụ Kiện', 'Accessories', 'Tai nghe', 'Chuột', 'Bàn phím', 'Loa', 'Đồng hồ', 'Đồng hồ thông minh']}
            bannerImage="/HINH_TRANG_CHU/phukien.jpg"
            videoUrl="/HINH_PHU_KIEN/JBL - Go 4 - Ultra-Portable Bluetooth Speaker.mp4"
        />
    );
}
