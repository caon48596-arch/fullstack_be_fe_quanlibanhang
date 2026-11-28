"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { getValidMediaUrl } from '@/util/fetchClient';
import {
    authLayoutStyles,
    formStyles,
    buttonStyles,
    alertStyles,
    linkStyles,
} from '@/styles/constants';

export default function RegisterPage() {
    const router = useRouter();
    const [formData, setFormData] = useState({
        username: '', email: '', password: '', confirmPassword: '',
    });
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!formData.username.trim() || !formData.email.trim() || !formData.password) {
            setError('Vui lòng nhập đầy đủ tất cả các trường.');
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            setError('Mật khẩu xác nhận không trùng khớp.');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            const apiBase = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';

            // Gửi yêu cầu OTP tập trung qua Backend Spring Boot (Redis TTL + Async SMTP)
            let response: Response | null = null;
            try {
                response = await fetch(`${apiBase}/users/request-otp`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: formData.email.trim(), type: 'REGISTER' }),
                });
            } catch {
                if (apiBase.includes('localhost')) {
                    response = await fetch('https://project1-production-6826.up.railway.app/api/users/request-otp', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email: formData.email.trim(), type: 'REGISTER' }),
                    }).catch(() => null);
                }
            }

            if (!response) {
                throw new Error('Không thể kết nối đến máy chủ xác thực.');
            }

            const data = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(data.message || data.error || 'Email hoặc số điện thoại đã tồn tại trong hệ thống!');
            }

            sessionStorage.setItem('pendingAuthData', JSON.stringify({
                username: formData.username.trim(),
                email:    formData.email.trim(),
                password: formData.password,
            }));

            router.push('/verify-otp?action=register');
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Không thể kết nối đến máy chủ.');
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
                    <h2 className={authLayoutStyles.cardTitle}>ĐĂNG KÝ TÀI KHOẢN</h2>

                    {error && <div className={alertStyles.error}>{error}</div>}

                    <form onSubmit={handleSubmit} className="space-y-3.5">
                        <div>
                            <label className={formStyles.label}>Tên người dùng</label>
                            <input
                                type="text"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                required
                                className={formStyles.input}
                                placeholder="Họ và tên hoặc nickname..."
                            />
                        </div>

                        <div>
                            <label className={formStyles.label}>Email</label>
                            <input
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                required
                                className={formStyles.input}
                                placeholder="example@gmail.com"
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
                                placeholder="Tạo mật khẩu..."
                            />
                        </div>

                        <div>
                            <label className={formStyles.label}>Xác nhận mật khẩu</label>
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                required
                                className={formStyles.input}
                                placeholder="Nhập lại mật khẩu..."
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={isLoading}
                            className={`${buttonStyles.primary} ${isLoading ? buttonStyles.primaryDisabled : buttonStyles.primaryActive}`}
                        >
                            {isLoading ? 'ĐANG XỬ LÝ...' : 'TIẾP TỤC (XÁC THỰC OTP)'}
                        </button>

                        <div className="flex justify-between items-center text-xs pt-2">
                            <span className="text-slate-500">Đã có tài khoản?</span>
                            <Link href="/login" className={linkStyles.accent}>Đăng nhập</Link>
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
