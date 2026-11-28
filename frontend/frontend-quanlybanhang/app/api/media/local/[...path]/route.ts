import { NextRequest, NextResponse } from 'next/server';
import fs from 'fs';
import path from 'path';

/**
 * Dev-only fallback streaming media từ thư mục `media-assets/` bên ngoài `public/`.
 * Hỗ trợ HTTP Range (206 Partial Content) cho phát video 4K MP4 mượt mà ở máy cục bộ
 * trước khi đẩy lên Cloudflare R2 bằng lệnh `npm run media:upload`.
 */

const MEDIA_ROOT = path.resolve(process.cwd(), '..', '..', 'media-assets');

const MIME_TYPES: Record<string, string> = {
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.webp': 'image/webp',
    '.gif': 'image/gif',
    '.avif': 'image/avif',
    '.svg': 'image/svg+xml',
    '.mp4': 'video/mp4',
    '.webm': 'video/webm',
    '.ogg': 'video/ogg',
};

export async function GET(
    req: NextRequest,
    context: { params: Promise<{ path: string[] }> }
) {
    const { path: segments } = await context.params;
    if (!segments || segments.length === 0) {
        return new NextResponse('Not Found', { status: 404 });
    }

    const decodedSegments = segments.map(s => decodeURIComponent(s));
    const resolvedPath = path.resolve(MEDIA_ROOT, ...decodedSegments);

    // Chống Path Traversal tuyệt đối
    if (!resolvedPath.startsWith(MEDIA_ROOT + path.sep)) {
        return new NextResponse('Forbidden', { status: 403 });
    }

    if (!fs.existsSync(resolvedPath) || !fs.statSync(resolvedPath).isFile()) {
        return new NextResponse('Not Found', { status: 404 });
    }

    const stat = fs.statSync(resolvedPath);
    const ext = path.extname(resolvedPath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    const fileSize = stat.size;
    const rangeHeader = req.headers.get('range');

    // Hỗ trợ HTTP Range Streaming cho video 4K (.mp4)
    if (rangeHeader) {
        const match = rangeHeader.match(/bytes=(\d+)-(\d*)/);
        if (match) {
            const start = parseInt(match[1], 10);
            const end = match[2] ? Math.min(parseInt(match[2], 10), fileSize - 1) : Math.min(start + 2 * 1024 * 1024 - 1, fileSize - 1);
            if (start >= fileSize) {
                return new NextResponse(null, {
                    status: 416,
                    headers: { 'Content-Range': `bytes */${fileSize}` },
                });
            }

            const chunkSize = end - start + 1;
            const stream = fs.createReadStream(resolvedPath, { start, end });
            const webStream = new ReadableStream({
                start(controller) {
                    stream.on('data', chunk => controller.enqueue(chunk));
                    stream.on('end', () => controller.close());
                    stream.on('error', err => controller.error(err));
                },
                cancel() {
                    stream.destroy();
                },
            });

            return new NextResponse(webStream, {
                status: 206,
                headers: {
                    'Content-Range': `bytes ${start}-${end}/${fileSize}`,
                    'Accept-Ranges': 'bytes',
                    'Content-Length': String(chunkSize),
                    'Content-Type': contentType,
                    'Cache-Control': 'public, max-age=86400',
                },
            });
        }
    }

    const fileBuffer = fs.readFileSync(resolvedPath);
    return new NextResponse(fileBuffer, {
        status: 200,
        headers: {
            'Content-Type': contentType,
            'Content-Length': String(fileSize),
            'Accept-Ranges': 'bytes',
            'Cache-Control': 'public, max-age=86400',
        },
    });
}
