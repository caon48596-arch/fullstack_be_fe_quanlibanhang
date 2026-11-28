"use client";

import React, { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { getUserRoleFromToken, getValidMediaUrl } from '@/util/fetchClient';
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

    // Tự động chuyển trang nếu đã có phiên đăng nhập
    useEffect(() => {
        try {
            const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
            if (token) {
                const role = getUserRoleFromToken();
                if (role === 'ROLE_ADMIN' || role === 'ADMIN') {
                    router.replace('/admin');
                } else {
                    router.replace('/');
                }
            }
        } catch {}
    }, [router]);

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
            const cleanEmail = formData.email.trim();
            const password = formData.password;

            // Đăng nhập trực tiếp qua Backend Spring Boot (Single Source of Truth)
            let loginRes: Response | null = null;
            try {
                loginRes = await fetch(`${apiBase}/users/login`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: cleanEmail, password }),
                });
            } catch {
                if (apiBase.includes('localhost')) {
                    loginRes = await fetch('https://project1-production-6826.up.railway.app/api/users/login', {
                        method: 'POST',
                        credentials: 'include',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email: cleanEmail, password }),
                    }).catch(() => null);
                }
            }

            if (!loginRes) {
                throw new Error('Không thể kết nối đến máy chủ xác thực. Vui lòng thử lại sau!');
            }

            const loginData = await loginRes.json().catch(() => ({}));
            if (!loginRes.ok) {
                throw new Error(loginData.message || loginData.error || 'Sai tài khoản hoặc mật khẩu!');
            }

            const directUser = loginData.data || loginData;
            const directToken = directUser.token || directUser.accessToken;

            if (directToken) {
                sessionStorage.setItem('accessToken', directToken);
                localStorage.setItem('accessToken', directToken);

                const role = getUserRoleFromToken();
                const isAdmin = role === 'ROLE_ADMIN' || role === 'ADMIN';

                if (redirectUrl && (isAdmin || !redirectUrl.startsWith('/admin'))) {
                    alert('Đăng nhập thành công!');
                    router.push(redirectUrl);
                } else if (isAdmin) {
                    alert('Đăng nhập Quản trị viên thành công!');
                    router.push('/admin');
                } else {
                    alert('Đăng nhập thành công!');
                    router.push('/');
                }
                return;
            }

            // Nếu backend yêu cầu xác thực bước 2 (OTP), gửi yêu cầu OTP trực tiếp tới Spring Boot
            const otpRes = await fetch(`${apiBase}/users/request-otp`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: cleanEmail, type: 'LOGIN' }),
            });
            const otpData = await otpRes.json().catch(() => ({}));
            if (!otpRes.ok) {
                throw new Error(otpData.message || otpData.error || 'Không thể gửi mã OTP xác thực.');
            }

            sessionStorage.setItem('pendingAuthData', JSON.stringify({
                email: cleanEmail,
                password: password,
            }));

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
                <div className={authLayoutStyles.logoWrapper}>
                    <img
                        src={getValidMediaUrl('/HINH_TRANG_CHU/logo_dang nhap.png')}
                        alt="IPWORLD Logo"
                        className={authLayoutStyles.logoImage}
                        onError={(e) => { (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/logo2.jpg'); }}
                    />
                    <h2 className={authLayoutStyles.logoText} style={{ textShadow: '2px 3px 8px #173B70' }}>
                        IPWORLD
                    </h2>
                </div>

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
