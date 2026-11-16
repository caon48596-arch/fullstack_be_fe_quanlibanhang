import { sendOtpEmail } from './emailSender';

/**
 * Gửi email chứa mã OTP (Tương thích ngược với interface cũ, hỗ trợ SMTP + Resend)
 */
export async function sendOtpViaResend(params: {
    to: string;
    otp: string;
    type?: 'LOGIN' | 'REGISTER';
}): Promise<{ success: boolean; messageId?: string; mode: 'RESEND_LIVE' | 'DEV_CONSOLE' }> {
    const result = await sendOtpEmail(params);
    return {
        success: result.success,
        messageId: result.messageId,
        mode: result.channel === 'DEV_CONSOLE' ? 'DEV_CONSOLE' : 'RESEND_LIVE',
    };
}
