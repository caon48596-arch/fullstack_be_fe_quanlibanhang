/**
 * util/auth.ts
 * 
 * Các tiện ích quản lý và giải mã JWT authentication token.
 * Re-export từ util/fetchClient để đảm bảo tính tập trung (Single Source of Truth).
 */

export { 
    getUserRoleFromToken, 
    getUserNameFromToken, 
    getDecodedToken 
} from './fetchClient';

export type { JwtCustomPayload } from './fetchClient';
