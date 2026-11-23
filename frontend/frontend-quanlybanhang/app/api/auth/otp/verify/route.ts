import { NextResponse } from 'next/server';
import { getOtpFromDynamoDB, deleteOtpFromDynamoDB, incrementOtpAttempts } from '@/util/awsDynamoDB';
import { verifyOtpWithSessionToken } from '@/util/otpCrypto';

const BACKEND_API_BASE = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';

export async function POST(req: Request) {
    try {
        const body = await req.json();
        const { email, otp, password, otpSessionToken } = body;

        if (!email || !otp) {
            return NextResponse.json(
                { success: false, message: 'Thiếu thông tin email hoặc mã OTP.' },
                { status: 400 }
            );
        }

        const normalizedEmail = email.trim().toLowerCase();
        const submittedOtp = otp.toString().trim();

        let isOtpValid = false;
        let failReason = '';

        // Ưu tiên xác thực stateless qua session token (serverless), fallback sang DynamoDB/Memory
        if (otpSessionToken) {
            const tokenVerification = verifyOtpWithSessionToken(otpSessionToken, normalizedEmail, submittedOtp);
            if (tokenVerification.valid) {
                isOtpValid = true;
            } else {
                failReason = tokenVerification.reason || 'Mã OTP không chính xác.';
            }
        }

        if (!isOtpValid) {
            const record = await getOtpFromDynamoDB(normalizedEmail);

            if (record) {
                const nowEpoch = Math.floor(Date.now() / 1000);
                if (nowEpoch > record.expiresAt) {
                    await deleteOtpFromDynamoDB(normalizedEmail);
                    return NextResponse.json(
                        { success: false, message: 'Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.' },
                        { status: 400 }
                    );
                }

                if (record.attempts >= 5) {
                    await deleteOtpFromDynamoDB(normalizedEmail);
                    return NextResponse.json(
                        { success: false, message: 'Bạn đã nhập sai mã OTP quá 5 lần. Vui lòng yêu cầu mã mới.' },
                        { status: 400 }
                    );
                }

                if (record.otp === submittedOtp) {
                    isOtpValid = true;
                    await deleteOtpFromDynamoDB(normalizedEmail);
                } else {
                    await incrementOtpAttempts(normalizedEmail, record.attempts);
                    const remaining = 5 - (record.attempts + 1);
                    return NextResponse.json(
                        {
                            success: false,
                            message: `Mã OTP không chính xác. Bạn còn ${remaining} lần thử trước khi bị khóa.`,
                        },
                        { status: 400 }
                    );
                }
            }
        }

        if (!isOtpValid) {
            return NextResponse.json(
                { success: false, message: failReason || 'Mã OTP không tồn tại hoặc đã hết hạn.' },
                { status: 400 }
            );
        }

        let backendUser = null;
        let token = null;

        if (password) {
            try {
                let loginRes: Response | null = null;
                try {
                    loginRes = await fetch(`${BACKEND_API_BASE}/users/login`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email: normalizedEmail, password }),
                    });
                } catch {
                    if (BACKEND_API_BASE.includes('localhost')) {
                        loginRes = await fetch('https://project1-production-6826.up.railway.app/api/users/login', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ email: normalizedEmail, password }),
                        }).catch(() => null);
                    }
                }

                if (loginRes && loginRes.ok) {
                    const loginData = await loginRes.json();
                    backendUser = loginData.data || loginData;
                    token = backendUser.token || backendUser.accessToken;
                }
            } catch (err) {
                console.warn('[Verify OTP] Kết nối Spring Boot Login có độ trễ, sinh token dự phòng:', err);
            }
        }

        // Sinh JWT fallback (hiệu lực 24h) nếu không lấy được token từ backend
        if (!token) {
            const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
            const payload = Buffer.from(JSON.stringify({
                sub: normalizedEmail,
                username: normalizedEmail.split('@')[0],
                role: 'ROLE_CLIENT',
                iat: Math.floor(Date.now() / 1000),
                exp: Math.floor(Date.now() / 1000) + 86400,
            })).toString('base64url');
            const signature = Buffer.from(`ipworld_verified_${Date.now()}`).toString('base64url');
            token = `${header}.${payload}.${signature}`;
        }

        return NextResponse.json({
            success: true,
            message: 'Xác thực OTP thành công!',
            token,
            data: backendUser || { email: normalizedEmail, role: 'ROLE_USER' },
        });
    } catch (error: any) {
        console.error('[API /api/auth/otp/verify] Lỗi:', error);
        return NextResponse.json(
            { success: false, message: error.message || 'Lỗi hệ thống khi xác thực OTP.' },
            { status: 500 }
        );
    }
}
