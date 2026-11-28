import type { Metadata } from 'next';
import CategoryProductPage from '@/components/CategoryProductPage';
import { fetchInitialCategoryData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'PC Gaming & Văn phòng | IPWORLD Store',
    description: 'Mua PC Gaming RTX, PC Văn phòng cấu hình mạnh giá tốt tại IPWORLD.',
};

const ALIASES = ['PC', 'Máy tính để bàn', 'Desktop', 'PC Gaming'];

export default async function PcPage() {
    const initial = await fetchInitialCategoryData('PC', ALIASES, 12);

    return (
        <CategoryProductPage
            categoryName="PC"
            categorySlug="pc"
            categoryAliases={ALIASES}
            bannerImage="/HINH_TRANG_CHU/pc.jpg"
            videoUrl="/HINH_PC/Dream PC build - RTX 5080 Noctua + ASUS ProArt Z890 + Core Ultra 9 285K - For Creators and Gaming (online-video-cutter.com).mp4"
            initialCategoryId={initial.categoryId}
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
        />
    );
}
