"use client";

import React from 'react';
import CategoryProductPage from '@/components/CategoryProductPage';

export default function LaptopPage() {
    return (
        <CategoryProductPage
            categoryName="Laptop"
            categorySlug="laptop"
            categoryAliases={['Laptop', 'MacBook', 'Máy tính xách tay', 'Notebook']}
            bannerImage="/HINH_TRANG_CHU/laptop.jog.png"
            videoUrl="/HINH_LAPTOP/New MacBook Pro with PROLED XDR Display and Touch- The worlds best laptop display.mp4"
        />
    );
}
