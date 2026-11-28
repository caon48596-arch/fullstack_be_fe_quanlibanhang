#!/usr/bin/env node
/**
 * Script di chuyển toàn bộ ảnh & video 4K từ thư mục `media-assets/` lên Cloudflare R2 (S3-compatible).
 *
 * Cách chạy:
 *   npm run media:upload -- --dry-run
 *   npm run media:upload
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_DIR = path.resolve(__dirname, '..');
const MEDIA_ROOT = path.resolve(FRONTEND_DIR, '..', '..', 'media-assets');
const MANIFEST_PATH = path.join(FRONTEND_DIR, 'media-manifest.json');

// Nạp biến môi trường từ .env.local nếu tồn tại
function loadEnvFile(envPath) {
    if (!fs.existsSync(envPath)) return;
    const lines = fs.readFileSync(envPath, 'utf-8').split(/\r?\n/);
    for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) continue;
        const eqIdx = trimmed.indexOf('=');
        if (eqIdx <= 0) continue;
        const key = trimmed.slice(0, eqIdx).trim();
        const val = trimmed.slice(eqIdx + 1).trim().replace(/^["']|["']$/g, '');
        if (!(key in process.env)) {
            process.env[key] = val;
        }
    }
}

loadEnvFile(path.join(FRONTEND_DIR, '.env.local'));

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');

const MIME_MAP = {
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.webp': 'image/webp',
    '.gif': 'image/gif',
    '.avif': 'image/avif',
    '.mp4': 'video/mp4',
    '.webm': 'video/webm',
};

function walkFiles(dir) {
    if (!fs.existsSync(dir)) return [];
    const results = [];
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            results.push(...walkFiles(full));
        } else if (entry.isFile()) {
            results.push(full);
        }
    }
    return results;
}

async function uploadToR2(client, bucket, publicBase, fullPath, relativeKey) {
    const ext = path.extname(fullPath).toLowerCase();
    const contentType = MIME_MAP[ext] || 'application/octet-stream';
    const body = fs.readFileSync(fullPath);

    await client.send(new PutObjectCommand({
        Bucket: bucket,
        Key: relativeKey,
        Body: body,
        ContentType: contentType,
        CacheControl: 'public, max-age=31536000, immutable',
    }));

    const encodedKey = relativeKey.split('/').map(encodeURIComponent).join('/');
    return `${publicBase}/${encodedKey}`;
}

async function main() {
    const files = walkFiles(MEDIA_ROOT);
    const totalMB = (files.reduce((sum, f) => sum + fs.statSync(f).size, 0) / (1024 * 1024)).toFixed(2);

    console.log(`[Media Migration] Tìm thấy ${files.length} tệp (${totalMB} MB) trong ${MEDIA_ROOT}`);
    console.log(`[Media Migration] Provider: CLOUDFLARE R2 | Dry-run: ${dryRun}`);

    if (files.length === 0) {
        console.log('Không có tệp nào để upload.');
        return;
    }

    if (dryRun) {
        files.slice(0, 10).forEach(f => {
            const rel = path.relative(MEDIA_ROOT, f).replace(/\\/g, '/');
            const sizeMB = (fs.statSync(f).size / (1024 * 1024)).toFixed(2);
            console.log(`  - /${rel} (${sizeMB} MB)`);
        });
        if (files.length > 10) console.log(`  ... và ${files.length - 10} tệp khác.`);
        return;
    }

    const accountId = process.env.R2_ACCOUNT_ID;
    const r2Bucket = process.env.R2_BUCKET_NAME || '';
    const r2PublicBase = (process.env.R2_PUBLIC_URL || process.env.NEXT_PUBLIC_MEDIA_CDN_URL || '').replace(/\/+$/, '');
    if (!accountId || !process.env.R2_ACCESS_KEY_ID || !process.env.R2_SECRET_ACCESS_KEY || !r2Bucket || !r2PublicBase) {
        throw new Error('Thiếu biến môi trường Cloudflare R2 trong .env.local (R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET_NAME, R2_PUBLIC_URL).');
    }

    const r2Client = new S3Client({
        region: 'auto',
        endpoint: process.env.R2_ENDPOINT || `https://${accountId}.r2.cloudflarestorage.com`,
        credentials: {
            accessKeyId: process.env.R2_ACCESS_KEY_ID,
            secretAccessKey: process.env.R2_SECRET_ACCESS_KEY,
        },
    });

    const manifest = {};
    let uploaded = 0;

    for (const fullPath of files) {
        const relKey = path.relative(MEDIA_ROOT, fullPath).replace(/\\/g, '/');
        try {
            const url = await uploadToR2(r2Client, r2Bucket, r2PublicBase, fullPath, relKey);
            manifest[`/${relKey}`] = url;
            uploaded++;
            console.log(`[${uploaded}/${files.length}] ✅ /${relKey} -> ${url}`);
        } catch (err) {
            console.error(`❌ Lỗi upload /${relKey}:`, err.message);
        }
    }

    fs.writeFileSync(MANIFEST_PATH, JSON.stringify(manifest, null, 2), 'utf-8');
    console.log(`\nHoàn tất ${uploaded}/${files.length} tệp. Manifest lưu tại: ${MANIFEST_PATH}`);
}

main().catch(err => {
    console.error('[Media Migration] Lỗi:', err.message);
    process.exit(1);
});
