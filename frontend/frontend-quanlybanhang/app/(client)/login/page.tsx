"use client";

import React, { useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import {
    authLayoutStyles,
    formStyles,
    buttonStyles,
    alertStyles,
    linkStyles,
} from '@/styles/constants';

function LoginForm() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const redirectUrl = searchParams.get('redirect');

    const [formData, setFormData] = useState({ email: '', password: '' });
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!formData.email.trim() || !formData.password) {
            setError('Vui lòng nhập đầy đủ thông tin tài khoản và mật khẩu!');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            const apiBase = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';

            // Lưu thông tin đăng nhập tạm thời
            sessionStorage.setItem('pendingAuthData', JSON.stringify({
                email: formData.email.trim(),
                password: formData.password,
            }));

            // Gửi yêu cầu mã xác thực OTP qua email
            let otpSent = false;
            try {
                const nextRes = await fetch('/api/auth/otp/send', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: formData.email.trim(), type: 'LOGIN' }),
                });
                const nextData = await nextRes.json().catch(() => ({}));
                if (nextRes.ok && nextData.success) {
                    otpSent = true;
                    if (nextData.otpSessionToken) {
                        sessionStorage.setItem('pendingOtpSession', nextData.otpSessionToken);
                    }
                }
            } catch (nextErr) {
                console.warn('[Login] Đang kết nối dự phòng sang Backend:', nextErr);
            }

            // Nếu Next.js API chưa được hoặc fallback, gọi sang Backend
            if (!otpSent) {
                const response = await fetch(`${apiBase}/users/request-otp`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: formData.email.trim(), type: 'LOGIN' }),
                });

                const data = await response.json().catch(() => ({}));

                if (!response.ok && response.status !== 404) {
                    throw new Error(data.message || data.error || 'Email hoặc số điện thoại không tồn tại trong hệ thống!');
                }
            }

            const targetUrl = '/verify-otp?action=login' + (redirectUrl ? `&redirect=${encodeURIComponent(redirectUrl)}` : '');
            router.push(targetUrl);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Không thể kết nối máy chủ');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className={authLayoutStyles.page}>
            <div className={authLayoutStyles.container}>

                {/* Logo bên trái */}
                <div className={authLayoutStyles.logoWrapper}>
                    <img
                        src="/HINH_TRANG_CHU/logo_dang nhap.png"
                        alt="IPWORLD Logo"
                        className={authLayoutStyles.logoImage}
                        onError={(e) => { (e.target as HTMLImageElement).src = '/HINH_TRANG_CHU/logo2.jpg'; }}
                    />
                    <h2 className={authLayoutStyles.logoText} style={{ textShadow: '2px 3px 8px #173B70' }}>
                        IPWORLD
                    </h2>
                </div>

                {/* Form đăng nhập */}
                <div className={authLayoutStyles.card}>
                    <div className="flex items-center justify-between mb-2">
                        <h2 className={authLayoutStyles.cardTitle}>ĐĂNG NHẬP</h2>
                    </div>

                    {redirectUrl && (
                        <div className="mb-4 p-3 rounded-lg bg-blue-50 border border-blue-200 text-xs font-bold text-black text-center">
                            Quý khách vui lòng đăng nhập để tiếp tục thanh toán và tạo hóa đơn!
                        </div>
                    )}

                    {error && <div className={alertStyles.error}>{error}</div>}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label className={formStyles.label}>Tài khoản (Email hoặc SĐT)</label>
                            <input
                                type="text"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                required
                                className={formStyles.input}
                                placeholder="Nhập email hoặc số điện thoại..."
                            />
                        </div>

                        <div>
                            <label className={formStyles.label}>Mật khẩu</label>
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                required
                                className={formStyles.input}
                                placeholder="Nhập mật khẩu..."
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={isLoading}
                            className={`${buttonStyles.primary} ${isLoading ? buttonStyles.primaryDisabled : buttonStyles.primaryActive}`}
                        >
                            {isLoading ? 'ĐANG XỬ LÝ...' : 'ĐĂNG NHẬP'}
                        </button>

                        <div className="flex justify-between items-center text-xs pt-2">
                            <span className="text-black font-medium">Chưa có tài khoản?</span>
                            <Link href="/register" className={linkStyles.accent}>Đăng ký ngay</Link>
                        </div>

                        <div className="text-center pt-3 border-t border-slate-100">
                            <Link href="/" className={linkStyles.footerNav}>← Về Trang Chủ</Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default function LoginPage() {
    return (
        <Suspense fallback={
            <div className="flex items-center justify-center min-h-[50vh] text-black font-semibold text-sm">
                Đang tải...
            </div>
        }>
            <LoginForm />
        </Suspense>
    );
}
