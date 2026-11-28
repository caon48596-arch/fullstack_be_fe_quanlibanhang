import { jwtDecode } from 'jwt-decode';
import { resolveMediaUrl } from './mediaUrl';

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

let refreshPromise: Promise<string | null> | null = null;

async function tryRefreshAccessToken(baseUrl: string): Promise<string | null> {
    if (typeof window === 'undefined') return null;
    if (!refreshPromise) {
        refreshPromise = (async () => {
            try {
                const res = await fetch(`${baseUrl}/users/refresh`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                });
                if (!res.ok) return null;
                const data = await res.json().catch(() => ({}));
                const newToken = data?.data?.token || data?.token || null;
                if (newToken) {
                    setStoredToken(newToken);
                }
                return newToken;
            } catch {
                return null;
            } finally {
                refreshPromise = null;
            }
        })();
    }
    return refreshPromise;
}

export const fetchClient = async (endpoint: string, options: RequestInit = {}) => {
    const token = getStoredToken();
    const buildHeaders = (bearerToken: string | null) => ({
        'Content-Type': 'application/json',
        ...options.headers,
        ...(bearerToken ? { Authorization: `Bearer ${bearerToken}` } : {})
    });

    let activeBaseUrl = PRIMARY_API_URL;
    let response: Response | null = null;

    try {
        response = await fetch(`${activeBaseUrl}${endpoint}`, {
            cache: 'no-store',
            credentials: 'include',
            ...options,
            headers: buildHeaders(token),
        });
    } catch (primaryErr) {
        // Fallback sang Railway khi backend localhost chưa khởi động
        if (PRIMARY_API_URL.includes('localhost') && PRIMARY_API_URL !== FALLBACK_API_URL) {
            console.warn(`[fetchClient] Kết nối ${PRIMARY_API_URL} thất bại, tự động chuyển sang Railway...`);
            activeBaseUrl = FALLBACK_API_URL;
            try {
                response = await fetch(`${activeBaseUrl}${endpoint}`, {
                    cache: 'no-store',
                    credentials: 'include',
                    ...options,
                    headers: buildHeaders(token),
                });
            } catch {
                throw new Error('Không thể kết nối đến máy chủ bán hàng. Vui lòng kiểm tra mạng!');
            }
        } else {
            throw primaryErr;
        }
    }

    if (!response) {
        throw new Error('Không nhận được phản hồi từ máy chủ.');
    }

    // Tự động Refresh Access Token bằng HttpOnly Refresh Cookie khi Access Token hết hạn (401/403)
    const isAuthEndpoint = endpoint.includes('/users/login') || endpoint.includes('/users/refresh') || endpoint.includes('/users/logout');
    if (response.status === 401 && !isAuthEndpoint && typeof window !== 'undefined') {
        const refreshedToken = await tryRefreshAccessToken(activeBaseUrl);
        if (refreshedToken) {
            response = await fetch(`${activeBaseUrl}${endpoint}`, {
                cache: 'no-store',
                credentials: 'include',
                ...options,
                headers: buildHeaders(refreshedToken),
            });
        }
    }

    if (!response.ok) {
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

    try {
        const decoded = jwtDecode<JwtCustomPayload>(token);

        if (decoded.exp && decoded.exp * 1000 < Date.now()) {
            console.warn("[JWT] Token đã hết hạn. Đang dọn dẹp phiên đăng nhập.");
            clearStoredToken();
            return null;
        }

        return decoded;
    } catch (error) {
        console.warn("[JWT] Token không hợp lệ, đã dọn dẹp phiên đăng nhập:", error);
        clearStoredToken();
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
    return resolveMediaUrl(url);
};
