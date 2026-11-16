"use client";

import React from 'react';
import CategoryProductPage from '@/components/CategoryProductPage';

export default function MayTinhBangPage() {
    return (
        <CategoryProductPage
            categoryName="Máy tính bảng"
            categorySlug="may-tinh-bang"
            categoryAliases={['Máy tính bảng', 'Máy Tính Bảng', 'iPad', 'Tablet', 'Tab']}
            bannerImage="/HINH_TRANG_CHU/maytinhbang.jpg"
            videoUrl="/HINH_MAY_TINH_BANG/The all-new iPad Pro  TV Ad  Thinpossible.mp4"
        />
    );
}
