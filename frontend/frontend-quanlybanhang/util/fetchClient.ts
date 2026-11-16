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

/**
 * Lấy Token an toàn từ cả sessionStorage và localStorage
 */
export const getStoredToken = (): string | null => {
    if (typeof window === 'undefined') return null;
    return sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken') || null;
};

/**
 * Lưu Token đồng bộ vào cả sessionStorage và localStorage
 */
export const setStoredToken = (token: string) => {
    if (typeof window === 'undefined') return;
    try {
        sessionStorage.setItem('accessToken', token);
        localStorage.setItem('accessToken', token);
    } catch (e) {
        console.warn('[Auth] Không thể lưu token:', e);
    }
};

/**
 * Xóa Token khỏi bộ nhớ phiên
 */
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
    // 1. Lấy token từ bộ nhớ
    const token = getStoredToken();

    // 2. Gắn token vào Header (nếu có)
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
        ...(token ? { Authorization: `Bearer ${token}` } : {})
    };

    let response: Response | null = null;
    let usedBaseUrl = PRIMARY_API_URL;

    // 3. Gọi sang Backend (Có cơ chế tự động chuyển dự phòng nếu Backend cục bộ chưa bật)
    try {
        response = await fetch(`${PRIMARY_API_URL}${endpoint}`, {
            cache: 'no-store',
            ...options,
            headers
        });
    } catch (primaryErr) {
        // Nếu địa chỉ chính là localhost và không kết nối được, tự động chuyển sang Railway
        if (PRIMARY_API_URL.includes('localhost') && PRIMARY_API_URL !== FALLBACK_API_URL) {
            console.warn(`[fetchClient] Kết nối ${PRIMARY_API_URL} thất bại, tự động chuyển sang Railway...`);
            usedBaseUrl = FALLBACK_API_URL;
            try {
                response = await fetch(`${FALLBACK_API_URL}${endpoint}`, {
                    cache: 'no-store',
                    ...options,
                    headers
                });
            } catch (fallbackErr) {
                throw new Error('Không thể kết nối đến máy chủ máy chủ bán hàng. Vui lòng kiểm tra mạng!');
            }
        } else {
            throw primaryErr;
        }
    }

    if (!response) {
        throw new Error('Không nhận được phản hồi từ máy chủ.');
    }

    // 4. Xử lý lỗi nếu Backend trả về mã lỗi (như 401, 403, 404, 500)
    if (!response.ok) {
        // Chỉ dọn dẹp token khi thực sự bị 401 Unauthorized từ endpoint bảo mật người dùng
        if (response.status === 401 && typeof window !== 'undefined') {
            const isProtectedUserEndpoint = endpoint.includes('/users/profile') ||
                                            endpoint.includes('/orders/my-order-items') ||
                                            endpoint.includes('/orders/my-orders');
            if (isProtectedUserEndpoint) {
                console.warn('[Auth] Phiên đăng nhập đã hết hạn trên máy chủ.');
                clearStoredToken();
            }
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
    if (response.status === 204 || response.headers.get('content-length') === '0') {
        return {};
    }

    return response.json();
};

/**
 * Giải mã an toàn JWT và tự động kiểm tra thời hạn hết hạn (exp).
 * Bảo vệ phiên đăng nhập không bị tự động logout ngoài ý muốn.
 */
export const getDecodedToken = (): JwtCustomPayload | null => {
    if (typeof window === 'undefined') return null;

    const token = getStoredToken();
    if (!token) return null;

    // Hỗ trợ token phiên fallback có dạng ipworld_verified_
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

        // Kiểm tra thời hạn hết hạn của Token (exp tính bằng giây, Date.now() tính bằng mili-giây)
        if (decoded.exp && decoded.exp * 1000 < Date.now()) {
            console.warn("[JWT] Token đã hết hạn. Đang dọn dẹp phiên đăng nhập.");
            clearStoredToken();
            return null;
        }

        return decoded;
    } catch (error) {
        console.warn("[JWT] Token không chuẩn format chuẩn nhưng vẫn được giữ để duy trì phiên:", error);
        // Cố gắng giải mã phần payload (phần thứ 2 của token nếu có)
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
        } catch {
            // Không parse được
        }

        // Không xóa token vội vã để tránh bị văng session đăng nhập
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

    // Nếu là base64 thì giữ nguyên
    if (cleanUrl.startsWith('data:')) return cleanUrl;

    // Nếu bắt đầu bằng // (ví dụ //imgur.com/...)
    if (cleanUrl.startsWith('//')) return `https:${cleanUrl}`;

    // Nếu là đường dẫn tương đối từ backend
    if (cleanUrl.startsWith('/')) {
        const hostUrl = PRIMARY_API_URL.replace(/\/api\/?$/, '');
        return `${hostUrl}${cleanUrl}`;
    }

    // Nếu không có http/https, thêm https mặc định
    if (!cleanUrl.startsWith('http://') && !cleanUrl.startsWith('https://')) {
        return `https://${cleanUrl}`;
    }

    return cleanUrl;
};
