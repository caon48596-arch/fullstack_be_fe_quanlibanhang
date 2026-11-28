import { NextResponse } from 'next/server';
import {
    isR2Configured,
    validateMediaFile,
    uploadBufferToR2,
    createR2PresignedUploadUrl,
} from '@/util/mediaStorage';

const BACKEND_API_BASE = process.env.NEXT_PUBLIC_API_URL || 'https://project1-production-6826.up.railway.app/api';

async function verifyAdminRequest(req: Request): Promise<boolean> {
    const authHeader = req.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return false;
    }
    try {
        // Xác thực trực tiếp với Backend Spring Boot để bảo đảm chữ ký JWT hợp lệ & đúng ROLE_ADMIN
        const res = await fetch(`${BACKEND_API_BASE}/users/profile`, {
            headers: { Authorization: authHeader },
            cache: 'no-store',
        });
        if (!res.ok) return false;
        const json = await res.json();
        const role = json?.data?.role || json?.role;
        return role === 'ROLE_ADMIN' || role === 'ADMIN';
    } catch {
        return false;
    }
}

export async function POST(req: Request) {
    try {
        const isAdmin = await verifyAdminRequest(req);
        if (!isAdmin) {
            return NextResponse.json(
                { success: false, message: 'Chỉ Quản trị viên (Admin) mới được phép tải lên ảnh/video.' },
                { status: 403 }
            );
        }

        if (!isR2Configured()) {
            return NextResponse.json(
                {
                    success: false,
                    message:
                        'Chưa cấu hình Cloudflare R2 (R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET_NAME, R2_PUBLIC_URL) trong .env.local.',
                },
                { status: 503 }
            );
        }

        const contentType = req.headers.get('content-type') || '';

        // Chế độ 1: Cấp Presigned URL cho Cloudflare R2 (tối ưu cho video 4K dung lượng lớn)
        if (contentType.includes('application/json')) {
            const body = await req.json();
            const { fileName, fileType, fileSize = 0, folder = 'products' } = body;
            if (!fileName || !fileType) {
                return NextResponse.json(
                    { success: false, message: 'Thiếu fileName hoặc fileType.' },
                    { status: 400 }
                );
            }
            validateMediaFile(fileType, Number(fileSize));
            const signed = await createR2PresignedUploadUrl({
                fileName,
                contentType: fileType,
                folder,
            });
            return NextResponse.json({ success: true, provider: 'r2', ...signed });
        }

        // Chế độ 2: Upload trực tiếp multipart/form-data lên Cloudflare R2
        const formData = await req.formData();
        const file = formData.get('file');
        const folder = String(formData.get('folder') || 'products');

        if (!file || !(file instanceof File)) {
            return NextResponse.json(
                { success: false, message: 'Vui lòng chọn tệp ảnh hoặc video hợp lệ.' },
                { status: 400 }
            );
        }

        validateMediaFile(file.type, file.size);
        const arrayBuffer = await file.arrayBuffer();
        const buffer = Buffer.from(arrayBuffer);

        const result = await uploadBufferToR2({
            buffer,
            fileName: file.name,
            contentType: file.type,
            folder,
        });

        return NextResponse.json({
            success: true,
            data: result,
        });
    } catch (error: any) {
        console.error('[API /api/media/upload] Lỗi:', error);
        return NextResponse.json(
            { success: false, message: error.message || 'Lỗi tải tệp lên Cloudflare R2.' },
            { status: 400 }
        );
    }
}
