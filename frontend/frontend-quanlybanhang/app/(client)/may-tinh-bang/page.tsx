import type { Metadata } from 'next';
import CategoryProductPage from '@/components/CategoryProductPage';
import { fetchInitialCategoryData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'Máy tính bảng & iPad | IPWORLD Store',
    description: 'Mua iPad Pro, iPad Air, iPad Mini và máy tính bảng chính hãng tại IPWORLD.',
};

const ALIASES = ['Máy tính bảng', 'Máy Tính Bảng', 'iPad', 'Tablet', 'Tab'];

export default async function MayTinhBangPage() {
    const initial = await fetchInitialCategoryData('Máy tính bảng', ALIASES, 12);

    return (
        <CategoryProductPage
            categoryName="Máy tính bảng"
            categorySlug="may-tinh-bang"
            categoryAliases={ALIASES}
            bannerImage="/HINH_TRANG_CHU/maytinhbang.jpg"
            videoUrl="/HINH_MAY_TINH_BANG/The all-new iPad Pro  TV Ad  Thinpossible.mp4"
            initialCategoryId={initial.categoryId}
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
        />
    );
}
