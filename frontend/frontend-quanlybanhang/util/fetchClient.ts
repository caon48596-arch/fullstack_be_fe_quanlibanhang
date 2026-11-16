import { jwtDecode } from 'jwt-decode';

export interface JwtCustomPayload {
    sub?: string;
    username?: string;
    role?: string;
    roles?: string[];
    authorities?: string | string[];
    exp?: number;
    iat?: number;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';

export const fetchClient = async (endpoint: string, options: RequestInit = {}) => {
    // 1. Lấy token từ sessionStorage
    const token = typeof window !== 'undefined' ? sessionStorage.getItem('accessToken') : null;

    // 2. Gắn token vào Header (nếu có)
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
        ...(token ? { Authorization: `Bearer ${token}` } : {})
    };

    // 3. Gọi sang Backend
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        cache: 'no-store',
        ...options,
        headers
    });

    // 4. Xử lý lỗi nếu Backend trả về mã lỗi (như 401, 403, 404, 500)
    if (!response.ok) {
        // Tự động dọn dẹp token nếu Backend báo 401 Unauthorized (token hết hạn hoặc không hợp lệ)
        if (response.status === 401 && typeof window !== 'undefined') {
            sessionStorage.removeItem('accessToken');
        }

        // Cố gắng đọc message lỗi từ Backend trả về
        const errorText = await response.text();
        let errorMessage = `Đã xảy ra lỗi từ máy chủ (Mã: ${response.status})`;

        try {
            const errorData = JSON.parse(errorText);
            if (errorData.message) errorMessage = errorData.message;
        } catch {
            // Nếu không parse được JSON thì giữ nguyên câu báo lỗi mặc định
        }

        throw new Error(errorMessage);
    }

    // 5. Trả về data cho component dùng
    // Xử lý trường hợp API Xóa (DELETE) trả về rỗng (204 No Content)
    if (response.status === 204 || response.headers.get('content-length') === '0') {
        return {};
    }

    return response.json();
};

/**
 * Giải mã an toàn JWT và tự động kiểm tra thời hạn hết hạn (exp).
 */
export const getDecodedToken = (): JwtCustomPayload | null => {
    if (typeof window === 'undefined') return null;

    const token = sessionStorage.getItem('accessToken');
    if (!token) return null;

    try {
        const decoded = jwtDecode<JwtCustomPayload>(token);

        // Kiểm tra thời hạn hết hạn của Token (exp tính bằng giây, Date.now() tính bằng mili-giây)
        if (decoded.exp && decoded.exp * 1000 < Date.now()) {
            console.warn("[JWT] Token đã hết hạn. Đang dọn dẹp phiên đăng nhập.");
            sessionStorage.removeItem('accessToken');
            return null;
        }

        return decoded;
    } catch (error) {
        console.error("[JWT] Lỗi giải mã token:", error);
        sessionStorage.removeItem('accessToken');
        return null;
    }
};

export const getUserRoleFromToken = (): string | null => {
    const decoded = getDecodedToken();
    if (!decoded) return null;

    if (typeof decoded.role === 'string') return decoded.role;
    if (typeof decoded.authorities === 'string') return decoded.authorities;
    if (Array.isArray(decoded.authorities) && decoded.authorities.length > 0) return decoded.authorities[0];
    if (Array.isArray(decoded.roles) && decoded.roles.length > 0) return decoded.roles[0];
    return null;
};

export const getUserNameFromToken = (): string | null => {
    const decoded = getDecodedToken();
    if (!decoded) return null;
    return decoded.sub || decoded.username || null;
};

export const getValidMediaUrl = (url: string | undefined): string => {
    if (!url || url.trim() === '') return '';
    const cleanUrl = url.trim();

    // Nếu là base64 thì giữ nguyên
    if (cleanUrl.startsWith('data:')) return cleanUrl;

    // Nếu bắt đầu bằng // (ví dụ //imgur.com/...)
    if (cleanUrl.startsWith('//')) return `https:${cleanUrl}`;

    // Nếu là đường dẫn tương đối từ backend
    if (cleanUrl.startsWith('/')) {
        const hostUrl = API_BASE_URL.replace(/\/api\/?$/, '');
        return `${hostUrl}${cleanUrl}`;
    }

    // Nếu không có http/https, thêm https mặc định
    if (!cleanUrl.startsWith('http://') && !cleanUrl.startsWith('https://')) {
        return `https://${cleanUrl}`;
    }

    return cleanUrl;
};
