"use client";

import React, { useRef, useState } from 'react';
import { Upload, Loader2, CheckCircle2 } from 'lucide-react';
import { getStoredToken } from '@/util/fetchClient';

interface MediaUploadFieldProps {
    accept: 'image/*' | 'video/*';
    folder?: string;
    label: string;
    onUploaded: (cdnUrl: string) => void;
}

export default function MediaUploadField({
    accept,
    folder = 'products',
    label,
    onUploaded,
}: MediaUploadFieldProps) {
    const fileInputRef = useRef<HTMLInputElement | null>(null);
    const [uploading, setUploading] = useState(false);
    const [statusMsg, setStatusMsg] = useState<{ text: string; isError: boolean } | null>(null);

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        const token = getStoredToken();
        if (!token) {
            setStatusMsg({ text: 'Vui lòng đăng nhập tài khoản Admin để upload media.', isError: true });
            return;
        }

        setUploading(true);
        setStatusMsg(null);

        try {
            // Ưu tiên Presigned URL cho video dung lượng lớn (> 4MB) nếu đang dùng Cloudflare R2
            if (file.type.startsWith('video/') && file.size > 4 * 1024 * 1024) {
                const signRes = await fetch('/api/media/upload', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        fileName: file.name,
                        fileType: file.type,
                        fileSize: file.size,
                        folder,
                    }),
                });
                const signData = await signRes.json();
                if (signRes.ok && signData.uploadUrl && signData.publicUrl) {
                    const putRes = await fetch(signData.uploadUrl, {
                        method: 'PUT',
                        headers: { 'Content-Type': file.type },
                        body: file,
                    });
                    if (!putRes.ok) {
                        throw new Error('Không thể đẩy video trực tiếp lên Cloudflare R2.');
                    }
                    onUploaded(signData.publicUrl);
                    setStatusMsg({ text: 'Đã tải video trực tiếp lên Cloudflare R2!', isError: false });
                    return;
                }
            }

            // Upload qua Next.js API Route -> Cloudflare R2
            const form = new FormData();
            form.append('file', file);
            form.append('folder', folder);

            const res = await fetch('/api/media/upload', {
                method: 'POST',
                headers: {
                    Authorization: `Bearer ${token}`,
                },
                body: form,
            });

            const json = await res.json();
            if (!res.ok || !json.success || !json.data?.url) {
                throw new Error(json.message || 'Tải lên thất bại.');
            }

            onUploaded(json.data.url);
            setStatusMsg({
                text: 'Đã tải lên Cloudflare R2 thành công!',
                isError: false,
            });
        } catch (err: any) {
            setStatusMsg({ text: err.message || 'Lỗi tải tệp lên CDN.', isError: true });
        } finally {
            setUploading(false);
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    return (
        <div className="mt-1.5 flex flex-wrap items-center gap-2">
            <input
                ref={fileInputRef}
                type="file"
                accept={accept}
                onChange={handleFileChange}
                className="hidden"
            />
            <button
                type="button"
                disabled={uploading}
                onClick={() => fileInputRef.current?.click()}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold bg-[#173B70] hover:bg-[#0866e8] text-white disabled:bg-slate-400 transition-colors cursor-pointer"
            >
                {uploading ? <Loader2 size={13} className="animate-spin" /> : <Upload size={13} />}
                {uploading ? 'Đang tải lên CDN...' : label}
            </button>
            {statusMsg && (
                <span className={`text-[11px] font-semibold inline-flex items-center gap-1 ${statusMsg.isError ? 'text-red-600' : 'text-emerald-700'}`}>
                    {!statusMsg.isError && <CheckCircle2 size={13} />}
                    {statusMsg.text}
                </span>
            )}
        </div>
    );
}
