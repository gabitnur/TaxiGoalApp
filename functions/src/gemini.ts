import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";

const geminiApiKey = defineSecret("GEMINI_API_KEY");

// Primary model identifier for deep debug
const PRIMARY_MODEL = "gemini-flash-latest";
const API_VERSION = "v1.5-flash"; // Internal mapping
const ENDPOINT_HOST = "generativelanguage.googleapis.com";

export async function chat(message: string, context: string, requestId: string) {
    const apiKey = geminiApiKey.value();
    if (!apiKey) {
        throw new Error("GEMINI_API_KEY is not configured on server");
    }

    console.info(`[Gemini][${requestId}] DEBUG_API_REQUEST | Model: ${PRIMARY_MODEL} | Provider: Google | Host: ${ENDPOINT_HOST}`);

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: PRIMARY_MODEL });

    const prompt = `${context}\n\nUser Question: ${message}`;

    try {
        const result = await model.generateContent(prompt);
        const response = await result.response;
        const text = response.text();

        if (!text) {
            throw new Error("EMPTY_RESPONSE");
        }

        return { success: true, reply: text };
    } catch (error: any) {
        console.error(`[Gemini][${requestId}] DEBUG ERROR | Status: ${error.status || "UNKNOWN"} | Msg: ${error.message}`);

        // Extract as much info as possible from the SDK error
        const debugInfo = {
            requestId: requestId,
            provider: "google-gemini",
            model: PRIMARY_MODEL,
            apiVersion: API_VERSION,
            endpointHost: ENDPOINT_HOST,
            httpStatus: error.status || 500,
            status: error.statusText || "INTERNAL_ERROR",
            message: error.message || "Unknown error occurred"
        };

        return {
            success: false,
            debug: debugInfo,
            errorType: mapErrorType(error.message),
            message: error.message
        };
    }
}

function mapErrorType(msg: string): string {
    if (!msg) return "UNKNOWN_ERROR";
    const lower = msg.toLowerCase();
    if (lower.includes("404") || lower.includes("not found")) return "MODEL_NOT_FOUND";
    if (lower.includes("503") || lower.includes("demand")) return "TEMPORARILY_UNAVAILABLE";
    if (lower.includes("429")) return "RATE_LIMITED";
    if (lower.includes("401") || lower.includes("api key")) return "AUTH_ERROR";
    return "UNKNOWN_ERROR";
}

/**
 * Diagnostic function to verify model connectivity.
 */
export async function testConnectivity() {
    const apiKey = geminiApiKey.value();
    if (!apiKey) return { status: "SECRET_MISSING" };

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: PRIMARY_MODEL });

    try {
        const result = await model.generateContent("Return only: OK");
        const text = (await result.response).text().trim();
        return {
            model: PRIMARY_MODEL,
            status: text === "OK" ? "PASS" : "FAIL",
            response: text
        };
    } catch (error: any) {
        return {
            model: PRIMARY_MODEL,
            status: "ERROR",
            error: error.message
        };
    }
}
