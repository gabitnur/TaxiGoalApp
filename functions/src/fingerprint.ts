import * as crypto from 'crypto';

export function generateFingerprint(category: string, errorType: string, message: string, stack: string): string {
    const data = `${category}|${errorType}|${message}|${stack.split('\n').slice(0, 3).join('')}`;
    return crypto.createHash('sha256').update(data).digest('hex').substring(0, 16);
}
