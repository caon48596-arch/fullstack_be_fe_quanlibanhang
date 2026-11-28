import type { Metadata } from 'next';
import HomeClient from '@/components/HomeClient';
import { fetchInitialHomeData } from '@/util/serverCatalog';

export const revalidate = 60;

export const metadata: Metadata = {
    title: 'IPWORLD - Trang chủ | Mua sắm thiết bị công nghệ chính hãng',
    description: 'Khám phá Điện thoại, Laptop, PC Gaming, Màn hình, Máy tính bảng và Phụ kiện công nghệ chính hãng với giá ưu đãi tại IPWORLD.',
};

export default async function HomePage() {
    const initial = await fetchInitialHomeData(12);

    return (
        <HomeClient
            initialProducts={initial.products}
            initialTotalElements={initial.totalElements}
            initialTotalPages={initial.totalPages}
            initialCategories={initial.categories}
        />
    );
}
