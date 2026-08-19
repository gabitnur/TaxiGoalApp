import { VercelRequest, VercelResponse } from '@vercel/node';
import { GoogleGenerativeAI } from "@google/generative-ai";
import { verifyAuth } from './_utils/auth';

const PRIMARY_MODEL = "gemini-flash-latest";
const ENDPOINT_HOST = "generativelanguage.googleapis.com";

export default async function handler(req: VercelRequest, res: VercelResponse) {
    if (req.method !== 'POST') {
        return res.status(405).json({ success: false, message: 'Method Not Allowed' });
    }

    const startTime = Date.now();
    const { message, safeContext, requestId } = req.body;
    const currentRequestId = requestId || `VER-${Date.now()}`;

    try {
        const decodedToken = await verifyAuth(req.headers.authorization);
        const userUidSafe = decodedToken.uid.substring(0, 5) + '...';

        const apiKey = process.env.GEMINI_API_KEY;
        if (!apiKey) {
            console.error(`[Gemini][${currentRequestId}] CONFIG_ERROR | GEMINI_API_KEY is missing`);
            return res.status(500).json({ success: false, errorType: 'CONFIGURATION_ERROR', message: 'Server configuration error' });
        }

        console.info(`GEMINI_DEBUG_REQUEST | RequestId: ${currentRequestId} | User: ${userUidSafe} | Model: ${PRIMARY_MODEL} | Host: ${ENDPOINT_HOST}`);

        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({ model: PRIMARY_MODEL });
        const prompt = `${safeContext}\n\nUser Question: ${message}`;

        const result = await model.generateContent(prompt);
        const response = await result.response;
        const text = response.text();

        const latency = Date.now() - startTime;
        console.info(`GEMINI_DEBUG_SUCCESS | RequestId: ${currentRequestId} | Latency: ${latency}ms`);

        return res.status(200).json({
            success: true,
            requestId: currentRequestId,
            reply: text
        });

    } catch (error: any) {
        const latency = Date.now() - startTime;
        const msg = error.message || 'Unknown error';
        console.error(`GEMINI_DEBUG_ERROR | RequestId: ${currentRequestId} | Latency: ${latency}ms | Msg: ${msg}`);

        let errorType = 'UNKNOWN_ERROR';
        if (msg === 'UNAUTHENTICATED' || msg === 'INVALID_AUTH') errorType = 'AUTH_ERROR';
        else if (msg.includes('404') || msg.includes('not found')) errorType = 'MODEL_NOT_FOUND';
        else if (msg.includes('503') || msg.includes('demand')) errorType = 'TEMPORARILY_UNAVAILABLE';
        else if (msg.includes('429')) errorType = 'RATE_LIMITED';

        return res.status(errorType === 'AUTH_ERROR' ? 401 : 500).json({
            success: false,
            requestId: currentRequestId,
            errorType: errorType,
            message: msg
        });
    }
}
