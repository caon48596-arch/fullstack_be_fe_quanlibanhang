"use client";

import React from 'react';
import CategoryProductPage from '@/components/CategoryProductPage';

export default function DienThoaiPage() {
    return (
        <CategoryProductPage
            categoryName="Điện thoại"
            categorySlug="dien-thoai"
            categoryAliases={['Điện thoại', 'Điện Thoại', 'Smartphone', 'iPhone']}
            bannerImage="/HINH_TRANG_CHU/dienthoai.jpg"
            videoUrl="/HINH_DIEN_THOAI/iPhone 16 Reveal (4K).mp4"
        />
    );
}
