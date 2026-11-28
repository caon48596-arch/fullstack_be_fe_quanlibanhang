import type { Metadata } from 'next';
import CategoryProductPage from '@/components/CategoryProductPage';
import { fetchInitialCategoryData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'Điện thoại chính hãng | IPWORLD Store',
    description: 'Mua điện thoại iPhone, Samsung Galaxy chính hãng giá tốt, bảo hành đầy đủ tại IPWORLD.',
};

const ALIASES = ['Điện thoại', 'Điện Thoại', 'Smartphone', 'iPhone'];

export default async function DienThoaiPage() {
    const initial = await fetchInitialCategoryData('Điện thoại', ALIASES, 12);

    return (
        <CategoryProductPage
            categoryName="Điện thoại"
            categorySlug="dien-thoai"
            categoryAliases={ALIASES}
            bannerImage="/HINH_TRANG_CHU/dienthoai.jpg"
            videoUrl="/HINH_DIEN_THOAI/iPhone 16 Reveal (4K).mp4"
            initialCategoryId={initial.categoryId}
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
        />
    );
}
