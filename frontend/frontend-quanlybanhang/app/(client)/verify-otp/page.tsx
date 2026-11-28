"use client";

import React, { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { ShieldCheck, Mail, KeyRound, RefreshCw, ArrowLeft } from 'lucide-react';
import { getUserRoleFromToken, getValidMediaUrl } from '@/util/fetchClient';
import {
    authLayoutStyles,
    formStyles,
    buttonStyles,
    alertStyles,
    linkStyles,
} from '@/styles/constants';

function VerifyOtpContent() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const action = searchParams.get('action') || 'login';
    const redirectUrl = searchParams.get('redirect');

    const [identifier, setIdentifier]           = useState('');
    const [otp, setOtp]                         = useState('');
    const [isLoading, setIsLoading]             = useState(false);
    const [isRequestingOtp, setIsRequestingOtp] = useState(false);
    const [cooldown, setCooldown]               = useState(0);
    const [error, setError]                     = useState<string | null>(null);
    const [successMessage, setSuccessMessage]   = useState<string | null>(
        'Mã xác thực OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.',
    );
    const [authData, setAuthData]               = useState<any>(null);

    const apiBase = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';
    const isRegister = action === 'register';

    useEffect(() => {
        try {
            const stored = sessionStorage.getItem('pendingAuthData');
            if (stored) {
                const parsed = JSON.parse(stored);
                setAuthData(parsed);
                if (parsed.email) setIdentifier(parsed.email);
            } else {
                router.replace('/login');
            }
        } catch {
            router.replace('/login');
        }
    }, [router]);

    useEffect(() => {
        if (cooldown > 0) {
            const timer = setTimeout(() => setCooldown((prev) => prev - 1), 1000);
            return () => clearTimeout(timer);
        }
    }, [cooldown]);

    const postBackend = async (path: string, payload: Record<string, unknown>): Promise<Response> => {
        try {
            return await fetch(`${apiBase}${path}`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
        } catch (err) {
            if (apiBase.includes('localhost')) {
                return await fetch(`https://project1-production-6826.up.railway.app/api${path}`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload),
                });
            }
            throw err;
        }
    };

    const handleRequestOtp = async () => {
        if (!identifier) {
            setError('Không tìm thấy thông tin email.');
            return;
        }

        setIsRequestingOtp(true);
        setError(null);
        setSuccessMessage(null);

        try {
            const response = await postBackend('/users/request-otp', {
                email: identifier,
                type: isRegister ? 'REGISTER' : 'LOGIN',
            });

            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || data.error || 'Không thể gửi lại mã OTP.');

            setSuccessMessage(data.message || 'Mã OTP mới đã được gửi đến email của bạn!');
            setCooldown(60);
        } catch (err: any) {
            setError(err.message || 'Lỗi kết nối máy chủ.');
        } finally {
            setIsRequestingOtp(false);
        }
    };

    const handleVerify = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!otp.trim()) {
            setError('Vui lòng nhập mã OTP gồm 6 chữ số');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            if (isRegister) {
                const response = await postBackend('/users/register', {
                    ...authData,
                    email: identifier,
                    otp: otp.trim(),
                });
                const data = await response.json().catch(() => ({}));
                if (!response.ok) throw new Error(data.message || data.error || 'Đăng ký thất bại!');

                alert('Đăng ký tài khoản thành công! Vui lòng đăng nhập.');
                sessionStorage.removeItem('pendingAuthData');
                router.push('/login');
            } else {
                const response = await postBackend('/users/login-otp', {
                    email: identifier,
                    password: authData?.password,
                    otp: otp.trim(),
                });
                const data = await response.json().catch(() => ({}));
                if (!response.ok) throw new Error(data.message || data.error || 'Mã OTP không chính xác hoặc đã hết hạn!');

                const token = data?.data?.token ?? data?.token;
                if (!token) throw new Error('Không nhận được mã xác thực (Token) từ máy chủ.');

                sessionStorage.setItem('accessToken', token);
                localStorage.setItem('accessToken', token);
                sessionStorage.removeItem('pendingAuthData');

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
            }
        } catch (err: any) {
            setError(err.message || 'Lỗi kết nối máy chủ.');
        } finally {
            setIsLoading(false);
        }
    };

    const isOtpBtnDisabled = isRequestingOtp || cooldown > 0;

    const otpBtnLabel = isRequestingOtp
        ? 'Đang gửi...'
        : cooldown > 0
            ? `Gửi lại (${cooldown}s)`
            : null;

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
                    <div className="flex items-center justify-center gap-2 mb-2">
                        <ShieldCheck size={28} className="text-[#173B70]" />
                        <h2 className={authLayoutStyles.cardTitle}>XÁC THỰC OTP</h2>
                    </div>

                    <p className="text-xs text-slate-500 text-center mb-6">
                        {isRegister
                            ? 'Nhập mã xác thực để hoàn tất đăng ký tài khoản mới'
                            : 'Nhập mã xác thực để đăng nhập an toàn vào hệ thống'}
                    </p>

                    {error          && <div className={alertStyles.error}>{error}</div>}
                    {successMessage && <div className={alertStyles.success}>{successMessage}</div>}

                    <form onSubmit={handleVerify} className="space-y-4" autoComplete="off">
                        <div>
                            <label className={formStyles.labelWithIcon}>
                                <Mail size={14} /> Email nhận mã xác thực
                            </label>
                            <input
                                type="email"
                                value={identifier}
                                readOnly
                                className={formStyles.inputReadOnly}
                            />
                        </div>

                        <div>
                            <label className={formStyles.labelWithIcon}>
                                <KeyRound size={14} /> Mã xác thực OTP
                            </label>
                            <div className="flex gap-2">
                                <input
                                    type="text"
                                    value={otp}
                                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                                    required
                                    maxLength={6}
                                    placeholder="Nhập 6 số OTP..."
                                    className={`${formStyles.input} flex-1 font-bold tracking-widest text-center`}
                                />
                                <button
                                    type="button"
                                    onClick={handleRequestOtp}
                                    disabled={isOtpBtnDisabled}
                                    className={`${buttonStyles.otpAction} ${isOtpBtnDisabled ? 'bg-slate-400 cursor-not-allowed' : 'bg-[#173B70] hover:bg-[#0866e8] shadow-sm'}`}
                                >
                                    {otpBtnLabel ?? (<><RefreshCw size={13} /> Lấy OTP</>)}
                                </button>
                            </div>
                        </div>

                        <button
                            type="submit"
                            disabled={isLoading}
                            className={`${buttonStyles.primary} mt-3 ${isLoading ? buttonStyles.primaryDisabled : buttonStyles.primaryActive}`}
                        >
                            {isLoading ? 'ĐANG XỬ LÝ...' : 'XÁC THỰC & HOÀN TẤT'}
                        </button>

                        <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                            <Link
                                href={isRegister ? '/register' : '/login'}
                                className={`${linkStyles.accent} flex items-center gap-1`}
                            >
                                <ArrowLeft size={13} />
                                {isRegister ? 'Quay lại Đăng ký' : 'Quay lại Đăng nhập'}
                            </Link>
                            <Link href="/" className={linkStyles.muted}>Về Trang Chủ</Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default function VerifyOtpPage() {
    return (
        <Suspense fallback={
            <div className="min-h-screen flex items-center justify-center text-white font-bold">
                Đang tải trang xác thực OTP...
            </div>
        }>
            <VerifyOtpContent />
        </Suspense>
    );
}
