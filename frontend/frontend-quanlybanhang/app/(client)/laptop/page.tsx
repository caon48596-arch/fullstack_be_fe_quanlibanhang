import type { Metadata } from 'next';
import CategoryProductPage from '@/components/CategoryProductPage';
import { fetchInitialCategoryData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'Laptop chính hãng | IPWORLD Store',
    description: 'Mua Laptop MacBook, Gaming ROG, Acer Nitro, Lenovo LOQ chính hãng giá tốt tại IPWORLD.',
};

const ALIASES = ['Laptop', 'MacBook', 'Máy tính xách tay', 'Notebook'];

export default async function LaptopPage() {
    const initial = await fetchInitialCategoryData('Laptop', ALIASES, 12);

    return (
        <CategoryProductPage
            categoryName="Laptop"
            categorySlug="laptop"
            categoryAliases={ALIASES}
            bannerImage="/HINH_TRANG_CHU/laptop.jog.png"
            videoUrl="/HINH_LAPTOP/New MacBook Pro with PROLED XDR Display and Touch- The worlds best laptop display.mp4"
            initialCategoryId={initial.categoryId}
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
        />
    );
}
