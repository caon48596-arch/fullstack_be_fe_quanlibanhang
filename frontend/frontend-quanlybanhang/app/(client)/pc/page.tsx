"use client";

import React from 'react';
import CategoryProductPage from '@/components/CategoryProductPage';

export default function PcPage() {
    return (
        <CategoryProductPage
            categoryName="PC"
            categorySlug="pc"
            categoryAliases={['PC', 'Máy tính để bàn', 'Desktop', 'PC Gaming']}
            bannerImage="/HINH_TRANG_CHU/pc.jpg"
            videoUrl="/HINH_PC/Dream PC build - RTX 5080 Noctua + ASUS ProArt Z890 + Core Ultra 9 285K - For Creators and Gaming (online-video-cutter.com).mp4"
        />
    );
}
