import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, PutCommand, GetCommand, DeleteCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';

export interface OtpRecord {
    identifier: string; // Partition key: email/phone
    otp: string;
    expiresAt: number; // Unix epoch (giây) cho DynamoDB TTL
    attempts: number;
    type: 'LOGIN' | 'REGISTER';
    createdAt: string;
}

// Fallback in-memory khi chưa cấu hình AWS credentials
const inMemoryDynamoFallback = new Map<string, OtpRecord>();

const region = process.env.AWS_REGION || process.env.AWS_DEFAULT_REGION || 'ap-southeast-1';
const accessKeyId = process.env.AWS_ACCESS_KEY_ID;
const secretAccessKey = process.env.AWS_SECRET_ACCESS_KEY;
export const DYNAMODB_TABLE_NAME = process.env.DYNAMODB_TABLE_NAME || 'ipworld_otp_tokens';

let docClient: DynamoDBDocumentClient | null = null;

if (accessKeyId && secretAccessKey) {
    try {
        const client = new DynamoDBClient({
            region,
            credentials: {
                accessKeyId,
                secretAccessKey,
            },
        });
        docClient = DynamoDBDocumentClient.from(client, {
            marshallOptions: { removeUndefinedValues: true },
        });
        console.log(`[AWS DynamoDB] Đã khởi tạo kết nối DynamoDB tại Region: ${region}, Bảng: ${DYNAMODB_TABLE_NAME}`);
    } catch (err) {
        console.warn('[AWS DynamoDB] Không thể khởi tạo AWS DynamoDB Client, sử dụng fallback:', err);
        docClient = null;
    }
} else {
    console.log('[AWS DynamoDB] Chưa cấu hình AWS_ACCESS_KEY_ID & AWS_SECRET_ACCESS_KEY trong .env.local -> Kích hoạt chế độ mô phỏng DynamoDB an toàn.');
}

export async function saveOtpToDynamoDB(params: {
    email: string;
    otp: string;
    ttlSeconds?: number;
    type?: 'LOGIN' | 'REGISTER';
}): Promise<{ success: boolean; channel: 'AWS_DYNAMODB' | 'MEMORY_FALLBACK' }> {
    const { email, otp, ttlSeconds = 300, type = 'LOGIN' } = params;
    const identifier = email.toLowerCase().trim();
    const nowEpoch = Math.floor(Date.now() / 1000);
    const expiresAt = nowEpoch + ttlSeconds;

    const record: OtpRecord = {
        identifier,
        otp,
        expiresAt,
        attempts: 0,
        type,
        createdAt: new Date().toISOString(),
    };

    if (docClient) {
        try {
            await docClient.send(
                new PutCommand({
                    TableName: DYNAMODB_TABLE_NAME,
                    Item: record,
                })
            );
            console.log(`[AWS DynamoDB] ✅ Đã lưu OTP vào DynamoDB Table '${DYNAMODB_TABLE_NAME}' cho: ${identifier} (TTL: ${expiresAt})`);
            return { success: true, channel: 'AWS_DYNAMODB' };
        } catch (error: any) {
            console.error(`[AWS DynamoDB] ❌ Lỗi khi PutItem lên DynamoDB:`, error);
            inMemoryDynamoFallback.set(identifier, record);
            return { success: true, channel: 'MEMORY_FALLBACK' };
        }
    } else {
        inMemoryDynamoFallback.set(identifier, record);
        console.log(`[AWS DynamoDB Simulation] Đã lưu OTP cho ${identifier} (Mã: ${otp}, Hết hạn sau ${ttlSeconds}s)`);
        return { success: true, channel: 'MEMORY_FALLBACK' };
    }
}

export async function getOtpFromDynamoDB(email: string): Promise<OtpRecord | null> {
    const identifier = email.toLowerCase().trim();

    if (docClient) {
        try {
            const result = await docClient.send(
                new GetCommand({
                    TableName: DYNAMODB_TABLE_NAME,
                    Key: { identifier },
                })
            );
            return (result.Item as OtpRecord) || null;
        } catch (error) {
            console.error(`[AWS DynamoDB] ❌ Lỗi khi GetItem từ DynamoDB:`, error);
            return inMemoryDynamoFallback.get(identifier) || null;
        }
    } else {
        return inMemoryDynamoFallback.get(identifier) || null;
    }
}

export async function incrementOtpAttempts(email: string, currentAttempts: number): Promise<void> {
    const identifier = email.toLowerCase().trim();
    const newAttempts = currentAttempts + 1;

    if (docClient) {
        try {
            await docClient.send(
                new UpdateCommand({
                    TableName: DYNAMODB_TABLE_NAME,
                    Key: { identifier },
                    UpdateExpression: 'SET attempts = :a',
                    ExpressionAttributeValues: { ':a': newAttempts },
                })
            );
        } catch (error) {
            console.error('[AWS DynamoDB] Lỗi tăng số lần nhập sai:', error);
        }
    } else {
        const item = inMemoryDynamoFallback.get(identifier);
        if (item) {
            item.attempts = newAttempts;
            inMemoryDynamoFallback.set(identifier, item);
        }
    }
}

export async function deleteOtpFromDynamoDB(email: string): Promise<void> {
    const identifier = email.toLowerCase().trim();

    if (docClient) {
        try {
            await docClient.send(
                new DeleteCommand({
                    TableName: DYNAMODB_TABLE_NAME,
                    Key: { identifier },
                })
            );
            console.log(`[AWS DynamoDB] 🗑️ Đã xóa OTP khỏi DynamoDB cho: ${identifier}`);
        } catch (error) {
            console.error('[AWS DynamoDB] Lỗi xóa OTP khỏi DynamoDB:', error);
        }
    }
    inMemoryDynamoFallback.delete(identifier);
}
