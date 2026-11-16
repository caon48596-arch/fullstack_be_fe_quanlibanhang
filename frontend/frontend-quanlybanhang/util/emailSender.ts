import nodemailer from 'nodemailer';
import { Resend } from 'resend';

const smtpHost = process.env.SMTP_HOST || 'smtp.gmail.com';
const smtpPort = parseInt(process.env.SMTP_PORT || '587', 10);
const smtpUser = process.env.SMTP_USER || 'caon48596@gmail.com';
const smtpPass = process.env.SMTP_PASS || 'xhwwozzjhvgwfuwn';
const fromName = process.env.SMTP_FROM_NAME || 'IPWORLD Store';

const resendApiKey = process.env.RESEND_API_KEY;
const resendFromEmail = process.env.RESEND_FROM_EMAIL || 'IPWORLD <onboarding@resend.dev>';
const resend = resendApiKey ? new Resend(resendApiKey) : null;

// Khởi tạo Transporter cho SMTP (hỗ trợ gửi đến mọi email người nhận khi deploy)
const createTransporter = () => {
    if (!smtpUser || !smtpPass) return null;
    return nodemailer.createTransport({
        host: smtpHost,
        port: smtpPort,
        secure: smtpPort === 465, // true cho port 465, false cho 587
        auth: {
            user: smtpUser,
            pass: smtpPass,
        },
    });
};

/**
 * Gửi email mã OTP an toàn đến người dùng
 */
export async function sendOtpEmail(params: {
    to: string;
    otp: string;
    type?: 'LOGIN' | 'REGISTER';
}): Promise<{ success: boolean; channel: string; messageId?: string }> {
    const { to, otp, type = 'LOGIN' } = params;
    const actionText = type === 'REGISTER' ? 'Đăng ký tài khoản' : 'Đăng nhập hệ thống';

    const htmlContent = `
    <!DOCTYPE html>
    <html lang="vi">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Mã xác thực OTP - IPWORLD</title>
        <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f0f4f8; margin: 0; padding: 24px; color: #1e293b; }
            .container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); }
            .header { background: linear-gradient(135deg, #173B70 0%, #0a1f44 100%); padding: 32px 24px; text-align: center; color: #ffffff; }
            .brand { font-size: 26px; font-weight: 800; letter-spacing: 2px; color: #60a5fa; margin: 0; }
            .content { padding: 32px 24px; text-align: center; }
            .title { font-size: 20px; font-weight: 700; color: #173B70; margin-bottom: 12px; }
            .desc { font-size: 14px; color: #64748b; line-height: 1.6; margin-bottom: 24px; }
            .otp-box { background: #f8fafc; border: 2px dashed #0866e8; border-radius: 12px; padding: 18px; margin: 0 auto 24px; display: inline-block; min-width: 240px; }
            .otp-code { font-size: 34px; font-weight: 900; letter-spacing: 8px; color: #0866e8; margin: 0; font-family: monospace; }
            .expiry { font-size: 13px; color: #ef4444; font-weight: 600; margin-top: 8px; }
            .footer { background: #f1f5f9; padding: 20px 24px; font-size: 12px; color: #94a3b8; text-align: center; border-top: 1px solid #e2e8f0; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1 class="brand">⚡ IPWORLD STORE</h1>
            </div>
            <div class="content">
                <h2 class="title">Xác thực mã OTP (${actionText})</h2>
                <p class="desc">
                    Chào bạn, chúng tôi nhận được yêu cầu xác thực từ địa chỉ <strong>${to}</strong>. 
                    Vui lòng sử dụng mã xác thực bảo mật bên dưới để hoàn tất:
                </p>
                <div class="otp-box">
                    <p class="otp-code">${otp}</p>
                    <div class="expiry">⏱️ Mã có hiệu lực trong vòng 5 phút (300 giây)</div>
                </div>
                <p class="desc" style="font-size: 12px; margin-top: 16px;">
                    Tuyệt đối không chia sẻ mã này cho bất kỳ ai để bảo vệ an toàn cho tài khoản của bạn.
                </p>
            </div>
            <div class="footer">
                © 2026 IPWORLD Store • Nền tảng mua sắm thiết bị công nghệ chính hãng.
            </div>
        </div>
    </body>
    </html>
    `;

    // 1. Thử gửi qua SMTP (Gmail SMTP đảm bảo đến được mọi hòm thư của người dùng khi deploy)
    const transporter = createTransporter();
    if (transporter) {
        try {
            const info = await transporter.sendMail({
                from: `"${fromName}" <${smtpUser}>`,
                to,
                subject: `[IPWORLD] Mã OTP xác thực của bạn: ${otp}`,
                html: htmlContent,
            });
            console.log(`[SMTP Mailer] ✅ Email đã gửi thành công tới ${to}, messageId: ${info.messageId}`);
            return {
                success: true,
                channel: 'SMTP',
                messageId: info.messageId,
            };
        } catch (smtpErr) {
            console.error('[SMTP Mailer] ❌ Lỗi khi gửi qua SMTP:', smtpErr);
        }
    }

    // 2. Thử gửi qua Resend nếu có API key
    if (resend) {
        try {
            const data = await resend.emails.send({
                from: resendFromEmail,
                to: [to],
                subject: `[IPWORLD] Mã OTP xác thực của bạn: ${otp}`,
                html: htmlContent,
            });
            console.log(`[Resend] ✅ Email gửi thành công tới ${to}, id: ${data.data?.id}`);
            return {
                success: true,
                channel: 'RESEND',
                messageId: data.data?.id,
            };
        } catch (resendErr) {
            console.error('[Resend] ❌ Lỗi gửi qua Resend:', resendErr);
        }
    }

    // 3. Fallback dev console
    console.log(`[Email Dev Fallback] OTP cho ${to}: [ ${otp} ]`);
    return {
        success: true,
        channel: 'DEV_CONSOLE',
    };
}
