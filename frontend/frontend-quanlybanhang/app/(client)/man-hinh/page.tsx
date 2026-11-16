"use client";

import React from 'react';
import CategoryProductPage from '@/components/CategoryProductPage';

export default function ManHinhPage() {
    return (
        <CategoryProductPage
            categoryName="Màn hình"
            categorySlug="man-hinh"
            categoryAliases={['Màn hình', 'Màn Hình', 'Monitor', 'Màn']}
            bannerImage="/HINH_TRANG_CHU/manhinh.png"
            videoUrl="/HINH_MAN_HINH/PRO MP242A E2 & MP271A E2 & MP275 E2 Series120Hz Home Office Monitors in Various Sizes - MSI.mp4"
        />
    );
}
