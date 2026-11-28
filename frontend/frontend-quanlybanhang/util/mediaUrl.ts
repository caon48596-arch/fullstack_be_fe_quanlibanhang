/**
 * Bộ định tuyến URL Media tập trung (Cloudflare R2 CDN / Dev Local Fallback).
 * Toàn bộ ảnh sản phẩm & video 4K đã được chuyển ra khỏi thư mục `public/` của Next.js.
 */

const MEDIA_CDN_URL = (process.env.NEXT_PUBLIC_MEDIA_CDN_URL || '').replace(/\/+$/, '');
const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api').replace(/\/api\/?$/, '');

export const DEFAULT_LOGO_PATH = '/HINH_TRANG_CHU/logo2.jpg';
export const DEFAULT_FALLBACK_IMAGE_PATH = '/HINH_TRANG_CHU/ipwworlkd.png';

function encodePathSegments(relativePath: string): string {
    return relativePath
        .split('/')
        .filter(Boolean)
        .map(seg => encodeURIComponent(decodeURIComponent(seg)))
        .join('/');
}

/**
 * Chuẩn hóa đường dẫn media:
 * - URL đầy đủ (https://..., data:...) -> giữ nguyên
 * - Đường dẫn `/HINH_*` -> ánh xạ sang Cloudflare R2 CDN nếu đã cấu hình,
 *   hoặc `/api/media/local/HINH_*` (streaming từ `media-assets/` ngoài `public/` ở môi trường dev).
 */
export function resolveMediaUrl(url: string | undefined | null, fallbackPath: string = ''): string {
    const candidate = (url && url.trim() !== '') ? url.trim() : fallbackPath;
    if (!candidate) return '';

    if (candidate.startsWith('data:') || candidate.startsWith('blob:')) return candidate;
    if (candidate.startsWith('//')) return `https:${candidate}`;
    if (candidate.startsWith('http://') || candidate.startsWith('https://')) return candidate;

    // Tài nguyên ảnh/video catalog đã tách khỏi public/
    if (candidate.startsWith('/HINH_') || candidate.startsWith('HINH_')) {
        const cleanRelative = candidate.replace(/^\/+/, '');
        const encodedRelative = encodePathSegments(cleanRelative);

        // 1. Ưu tiên Cloudflare R2 Public CDN URL / Custom Domain
        if (MEDIA_CDN_URL) {
            return `${MEDIA_CDN_URL}/${encodedRelative}`;
        }

        // 2. Fallback cho môi trường dev local: streaming từ thư mục ngoài `media-assets/`
        return `/api/media/local/${encodedRelative}`;
    }

    // Các đường dẫn API upload hoặc asset tương đối khác
    if (candidate.startsWith('/api/')) {
        return candidate;
    }

    if (candidate.startsWith('/')) {
        // Icon tĩnh nhỏ còn lại trong public/ (icon.png, favicon.ico, *.svg)
        if (/^\/(icon\.png|favicon\.ico|[^/]+\.svg)$/i.test(candidate)) {
            return candidate;
        }
        return `${API_BASE_URL}${candidate}`;
    }

    return `https://${candidate}`;
}
