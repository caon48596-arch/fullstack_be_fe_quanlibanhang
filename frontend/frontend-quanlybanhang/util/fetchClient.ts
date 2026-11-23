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

const PRIMARY_API_URL = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';
const FALLBACK_API_URL = 'https://project1-production-6826.up.railway.app/api';

export const getStoredToken = (): string | null => {
    if (typeof window === 'undefined') return null;
    return sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken') || null;
};

export const setStoredToken = (token: string) => {
    if (typeof window === 'undefined') return;
    try {
        sessionStorage.setItem('accessToken', token);
        localStorage.setItem('accessToken', token);
    } catch (e) {
        console.warn('[Auth] Không thể lưu token:', e);
    }
};

export const clearStoredToken = () => {
    if (typeof window === 'undefined') return;
    try {
        sessionStorage.removeItem('accessToken');
        localStorage.removeItem('accessToken');
    } catch (e) {
        console.warn('[Auth] Không thể xóa token:', e);
    }
};

export const fetchClient = async (endpoint: string, options: RequestInit = {}) => {
    const token = getStoredToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
        ...(token ? { Authorization: `Bearer ${token}` } : {})
    };

    let response: Response | null = null;

    try {
        response = await fetch(`${PRIMARY_API_URL}${endpoint}`, {
            cache: 'no-store',
            ...options,
            headers
        });
    } catch (primaryErr) {
        // Fallback sang Railway khi backend localhost chưa khởi động
        if (PRIMARY_API_URL.includes('localhost') && PRIMARY_API_URL !== FALLBACK_API_URL) {
            console.warn(`[fetchClient] Kết nối ${PRIMARY_API_URL} thất bại, tự động chuyển sang Railway...`);
            try {
                response = await fetch(`${FALLBACK_API_URL}${endpoint}`, {
                    cache: 'no-store',
                    ...options,
                    headers
                });
            } catch {
                throw new Error('Không thể kết nối đến máy chủ máy chủ bán hàng. Vui lòng kiểm tra mạng!');
            }
        } else {
            throw primaryErr;
        }
    }

    if (!response) {
        throw new Error('Không nhận được phản hồi từ máy chủ.');
    }

    if (!response.ok) {
        // Chỉ xóa token khi 401 từ endpoint bảo mật người dùng để tránh logout nhầm
        if (response.status === 401 && typeof window !== 'undefined') {
            const isProtectedUserEndpoint = endpoint.includes('/users/profile') ||
                                            endpoint.includes('/orders/my-order-items') ||
                                            endpoint.includes('/orders/my-orders');
            if (isProtectedUserEndpoint) {
                console.warn('[Auth] Phiên đăng nhập đã hết hạn trên máy chủ.');
                clearStoredToken();
            }
        }

        const errorText = await response.text();
        let errorMessage = `Đã xảy ra lỗi từ máy chủ (Mã: ${response.status})`;

        try {
            const errorData = JSON.parse(errorText);
            if (errorData.message) errorMessage = errorData.message;
        } catch {}

        throw new Error(errorMessage);
    }

    if (response.status === 204 || response.headers.get('content-length') === '0') {
        return {};
    }

    return response.json();
};

export const getDecodedToken = (): JwtCustomPayload | null => {
    if (typeof window === 'undefined') return null;

    const token = getStoredToken();
    if (!token) return null;

    // Hỗ trợ token phiên fallback dạng ipworld_verified_<base64_email>
    if (token.startsWith('ipworld_verified_')) {
        try {
            const parts = token.split('_');
            const email = parts[2] ? atob(parts[2]) : 'user@ipworld.vn';
            return {
                sub: email,
                username: email.split('@')[0],
                role: 'ROLE_CLIENT',
                exp: Math.floor(Date.now() / 1000) + 86400,
            };
        } catch {
            return {
                sub: 'user@ipworld.vn',
                username: 'Khách hàng',
                role: 'ROLE_CLIENT',
                exp: Math.floor(Date.now() / 1000) + 86400,
            };
        }
    }

    try {
        const decoded = jwtDecode<JwtCustomPayload>(token);

        if (decoded.exp && decoded.exp * 1000 < Date.now()) {
            console.warn("[JWT] Token đã hết hạn. Đang dọn dẹp phiên đăng nhập.");
            clearStoredToken();
            return null;
        }

        return decoded;
    } catch (error) {
        console.warn("[JWT] Token không chuẩn format chuẩn nhưng vẫn được giữ để duy trì phiên:", error);
        try {
            const tokenParts = token.split('.');
            if (tokenParts.length >= 2) {
                const base64Url = tokenParts[1];
                const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(
                    atob(base64)
                        .split('')
                        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                        .join('')
                );
                return JSON.parse(jsonPayload);
            }
        } catch {}

        // Giữ nguyên token thay vì xóa ngay để tránh mất phiên ngoài ý muốn
        return {
            sub: 'user@ipworld.vn',
            username: 'Tài khoản',
            role: 'ROLE_CLIENT',
        };
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

    if (cleanUrl.startsWith('data:')) return cleanUrl;
    if (cleanUrl.startsWith('//')) return `https:${cleanUrl}`;

    if (cleanUrl.startsWith('/')) {
        const hostUrl = PRIMARY_API_URL.replace(/\/api\/?$/, '');
        return `${hostUrl}${cleanUrl}`;
    }

    if (!cleanUrl.startsWith('http://') && !cleanUrl.startsWith('https://')) {
        return `https://${cleanUrl}`;
    }

    return cleanUrl;
};
