import type { Metadata } from 'next';
import ShopClient from '@/components/ShopClient';
import { fetchInitialHomeData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'Cửa hàng công nghệ | IPWORLD Store',
    description: 'Toàn bộ sản phẩm công nghệ chính hãng tại IPWORLD - Tìm kiếm, lọc giá và đặt mua nhanh chóng.',
};

export default async function ShopPage() {
    const initial = await fetchInitialHomeData(12);

    return (
        <ShopClient
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
        />
    );
}
