import crypto from 'crypto';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

export type MediaProvider = 'r2';

export interface MediaUploadResult {
    provider: MediaProvider;
    url: string;
    key: string;
    contentType: string;
    size: number;
}

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;  // 10 MB
const MAX_VIDEO_BYTES = 100 * 1024 * 1024; // 100 MB

const ALLOWED_IMAGE_TYPES = new Set([
    'image/jpeg',
    'image/png',
    'image/webp',
    'image/gif',
    'image/avif',
]);

const ALLOWED_VIDEO_TYPES = new Set([
    'video/mp4',
    'video/webm',
    'video/quicktime',
]);

export function validateMediaFile(contentType: string, sizeBytes: number): { kind: 'image' | 'video' } {
    if (ALLOWED_IMAGE_TYPES.has(contentType)) {
        if (sizeBytes > MAX_IMAGE_BYTES) {
            throw new Error('Dung lượng ảnh vượt quá giới hạn 10MB.');
        }
        return { kind: 'image' };
    }
    if (ALLOWED_VIDEO_TYPES.has(contentType)) {
        if (sizeBytes > MAX_VIDEO_BYTES) {
            throw new Error('Dung lượng video vượt quá giới hạn 100MB.');
        }
        return { kind: 'video' };
    }
    throw new Error(`Định dạng tệp không được hỗ trợ (${contentType}). Chỉ chấp nhận JPG, PNG, WebP, AVIF, MP4, WebM.`);
}

export function isR2Configured(): boolean {
    return Boolean(
        process.env.R2_ACCOUNT_ID &&
        process.env.R2_ACCESS_KEY_ID &&
        process.env.R2_SECRET_ACCESS_KEY &&
        process.env.R2_BUCKET_NAME
    );
}

function createR2Client(): S3Client {
    const accountId = process.env.R2_ACCOUNT_ID!;
    const endpoint = process.env.R2_ENDPOINT || `https://${accountId}.r2.cloudflarestorage.com`;

    return new S3Client({
        region: 'auto',
        endpoint,
        credentials: {
            accessKeyId: process.env.R2_ACCESS_KEY_ID!,
            secretAccessKey: process.env.R2_SECRET_ACCESS_KEY!,
        },
    });
}

function sanitizeFileName(fileName: string): string {
    const ext = fileName.includes('.') ? fileName.slice(fileName.lastIndexOf('.')).toLowerCase() : '';
    const base = fileName
        .replace(/\.[^.]+$/, '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9_-]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 60) || 'media';
    return `${Date.now()}-${crypto.randomBytes(4).toString('hex')}-${base}${ext}`;
}

export async function uploadBufferToR2(params: {
    buffer: Buffer;
    fileName: string;
    contentType: string;
    folder?: string;
}): Promise<MediaUploadResult> {
    const bucket = process.env.R2_BUCKET_NAME!;
    const publicBaseUrl = (process.env.R2_PUBLIC_URL || process.env.NEXT_PUBLIC_MEDIA_CDN_URL || '').replace(/\/+$/, '');
    if (!publicBaseUrl) {
        throw new Error('Thiếu cấu hình R2_PUBLIC_URL hoặc NEXT_PUBLIC_MEDIA_CDN_URL để trả về đường dẫn CDN.');
    }

    const folder = (params.folder || 'products').replace(/^\/+|\/+$/g, '');
    const key = `${folder}/${sanitizeFileName(params.fileName)}`;

    const client = createR2Client();
    await client.send(
        new PutObjectCommand({
            Bucket: bucket,
            Key: key,
            Body: params.buffer,
            ContentType: params.contentType,
            CacheControl: 'public, max-age=31536000, immutable',
        })
    );

    return {
        provider: 'r2',
        url: `${publicBaseUrl}/${key}`,
        key,
        contentType: params.contentType,
        size: params.buffer.length,
    };
}

/**
 * Tạo Presigned PUT URL để trình duyệt upload video 4K dung lượng lớn trực tiếp lên Cloudflare R2
 * mà không đi qua băng thông Next.js server.
 */
export async function createR2PresignedUploadUrl(params: {
    fileName: string;
    contentType: string;
    folder?: string;
}): Promise<{ uploadUrl: string; publicUrl: string; key: string; expiresIn: number }> {
    const bucket = process.env.R2_BUCKET_NAME!;
    const publicBaseUrl = (process.env.R2_PUBLIC_URL || process.env.NEXT_PUBLIC_MEDIA_CDN_URL || '').replace(/\/+$/, '');
    if (!publicBaseUrl) {
        throw new Error('Thiếu cấu hình R2_PUBLIC_URL hoặc NEXT_PUBLIC_MEDIA_CDN_URL.');
    }

    const folder = (params.folder || 'products').replace(/^\/+|\/+$/g, '');
    const key = `${folder}/${sanitizeFileName(params.fileName)}`;
    const expiresIn = 300; // 5 phút

    const client = createR2Client();
    const command = new PutObjectCommand({
        Bucket: bucket,
        Key: key,
        ContentType: params.contentType,
        CacheControl: 'public, max-age=31536000, immutable',
    });

    const uploadUrl = await getSignedUrl(client, command, { expiresIn });
    return {
        uploadUrl,
        publicUrl: `${publicBaseUrl}/${key}`,
        key,
        expiresIn,
    };
}
