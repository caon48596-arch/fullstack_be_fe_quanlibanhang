import { NextResponse } from 'next/server';
import { saveOtpToDynamoDB } from '@/util/awsDynamoDB';
import { sendOtpEmail } from '@/util/emailSender';
import { generateOtpSessionToken } from '@/util/otpCrypto';

export async function POST(req: Request) {
    try {
        const body = await req.json();
        const { email, type = 'LOGIN' } = body;

        if (!email || typeof email !== 'string' || !email.trim()) {
            return NextResponse.json(
                { success: false, message: 'Vui lòng cung cấp địa chỉ email hợp lệ.' },
                { status: 400 }
            );
        }

        const normalizedEmail = email.trim().toLowerCase();
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();

        await saveOtpToDynamoDB({
            email: normalizedEmail,
            otp: otpCode,
            ttlSeconds: 300,
            type,
        });

        const emailResult = await sendOtpEmail({
            to: normalizedEmail,
            otp: otpCode,
            type,
        });

        // Signed token giúp xác thực OTP stateless khi chạy serverless
        const otpSessionToken = generateOtpSessionToken(normalizedEmail, otpCode, 300);

        const isDev = process.env.NODE_ENV !== 'production' || emailResult.channel === 'DEV_CONSOLE';

        return NextResponse.json({
            success: true,
            message: 'Mã xác thực OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.',
            otpSessionToken,
            debugOtp: isDev ? otpCode : undefined,
        });
    } catch (error: any) {
        console.error('[API /api/auth/otp/send] Lỗi:', error);
        return NextResponse.json(
            { success: false, message: error.message || 'Lỗi xử lý gửi mã OTP từ máy chủ.' },
            { status: 500 }
        );
    }
}
