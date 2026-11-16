import crypto from 'crypto';

const OTP_SECRET = process.env.OTP_SECRET || 'ipworld_secret_otp_encryption_key_2026';

/**
 * Sinh mã OTP session token có chữ ký số bí mật (giúp serverless nhận diện OTP mà không bị mất session)
 */
export function generateOtpSessionToken(email: string, otp: string, ttlSeconds = 300): string {
    const expiresAt = Math.floor(Date.now() / 1000) + ttlSeconds;
    const normalizedEmail = email.toLowerCase().trim();
    const dataToSign = `${normalizedEmail}:${otp.trim()}:${expiresAt}`;
    const signature = crypto.createHmac('sha256', OTP_SECRET).update(dataToSign).digest('hex');

    const payload = {
        email: normalizedEmail,
        expiresAt,
        signature,
    };

    return Buffer.from(JSON.stringify(payload)).toString('base64');
}

/**
 * Kiểm tra tính hợp lệ của OTP dựa trên session token và mã người dùng nhập
 */
export function verifyOtpWithSessionToken(
    sessionToken: string,
    email: string,
    submittedOtp: string
): { valid: boolean; reason?: string } {
    try {
        if (!sessionToken) return { valid: false, reason: 'Thiếu phiên xác thực OTP' };

        const raw = Buffer.from(sessionToken, 'base64').toString('utf-8');
        const payload = JSON.parse(raw);

        const normalizedEmail = email.toLowerCase().trim();
        if (payload.email !== normalizedEmail) {
            return { valid: false, reason: 'Email không khớp với phiên OTP' };
        }

        const now = Math.floor(Date.now() / 1000);
        if (now > payload.expiresAt) {
            return { valid: false, reason: 'Mã OTP đã hết hạn. Vui lòng nhận mã mới.' };
        }

        const dataToSign = `${normalizedEmail}:${submittedOtp.trim()}:${payload.expiresAt}`;
        const expectedSignature = crypto.createHmac('sha256', OTP_SECRET).update(dataToSign).digest('hex');

        if (expectedSignature !== payload.signature) {
            return { valid: false, reason: 'Mã OTP không chính xác.' };
        }

        return { valid: true };
    } catch (err) {
        return { valid: false, reason: 'Dữ liệu phiên xác thực không hợp lệ.' };
    }
}
