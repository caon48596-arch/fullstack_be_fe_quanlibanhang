import type { Metadata } from 'next';
import CategoryProductPage from '@/components/CategoryProductPage';
import { fetchInitialCategoryData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'Màn hình máy tính | IPWORLD Store',
    description: 'Màn hình Dell, ASUS, MSI, HP tần số quét cao, chuẩn màu chính hãng tại IPWORLD.',
};

const ALIASES = ['Màn hình', 'Màn Hình', 'Monitor', 'Màn'];

export default async function ManHinhPage() {
    const initial = await fetchInitialCategoryData('Màn hình', ALIASES, 12);

    return (
        <CategoryProductPage
            categoryName="Màn hình"
            categorySlug="man-hinh"
            categoryAliases={ALIASES}
            bannerImage="/HINH_TRANG_CHU/manhinh.png"
            videoUrl="/HINH_MAN_HINH/PRO MP242A E2 & MP271A E2 & MP275 E2 Series120Hz Home Office Monitors in Various Sizes - MSI.mp4"
            initialCategoryId={initial.categoryId}
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
        />
    );
}
