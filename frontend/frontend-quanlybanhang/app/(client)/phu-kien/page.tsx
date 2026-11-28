import type { Metadata } from 'next';
import CategoryProductPage from '@/components/CategoryProductPage';
import { fetchInitialCategoryData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'Phụ kiện công nghệ chính hãng | IPWORLD Store',
    description: 'Tai nghe, chuột, loa JBL, sạc dự phòng, cáp sạc và đồng hồ thông minh chính hãng tại IPWORLD.',
};

const ALIASES = ['Phụ kiện', 'Phụ Kiện', 'Accessories', 'Tai nghe', 'Chuột', 'Bàn phím', 'Loa', 'Đồng hồ', 'Đồng hồ thông minh'];

export default async function PhuKienPage() {
    const initial = await fetchInitialCategoryData('Phụ kiện', ALIASES, 12);

    return (
        <CategoryProductPage
            categoryName="Phụ kiện"
            categorySlug="phu-kien"
            categoryAliases={ALIASES}
            bannerImage="/HINH_TRANG_CHU/phukien.jpg"
            videoUrl="/HINH_PHU_KIEN/JBL - Go 4 - Ultra-Portable Bluetooth Speaker.mp4"
            initialCategoryId={initial.categoryId}
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
        />
    );
}
