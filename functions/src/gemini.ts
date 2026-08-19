import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";

const geminiApiKey = defineSecret("GEMINI_API_KEY");

// Primary model identifier for deep debug
const PRIMARY_MODEL = "gemini-flash-latest";
const ENDPOINT_HOST = "generativelanguage.googleapis.com";

export async function chat(message: string, context: string, requestId: string) {
    let apiKey = "";
    try {
        apiKey = geminiApiKey.value();
    } catch (e) {
        console.error(`[Gemini][${requestId}] SECRET_ERROR | GEMINI_API_KEY could not be read from secrets`);
    }

    if (!apiKey) {
        console.error(`[Gemini][${requestId}] CONFIG_ERROR | GEMINI_API_KEY is empty or missing`);
        return {
            success: false,
            errorType: "CONFIGURATION_ERROR",
            message: "GEMINI_API_KEY is not configured on server",
            debug: { requestId, status: "SECRET_MISSING" }
        };
    }

    console.info(`GEMINI_DEBUG_REQUEST | RequestId: ${requestId} | Model: ${PRIMARY_MODEL} | Host: ${ENDPOINT_HOST}`);

    try {
        const genAI = new GoogleGenerativeAI(apiKey);

        // Debug model requested
        console.info(`GEMINI_DEBUG_MODEL_INIT | RequestId: ${requestId} | RequestedModel: ${PRIMARY_MODEL}`);
        const model = genAI.getGenerativeModel({ model: PRIMARY_MODEL });

        const prompt = `${context}\n\nUser Question: ${message}`;
        console.info(`GEMINI_DEBUG_API_REQUEST | RequestId: ${requestId} | PromptLength: ${prompt.length}`);

        const result = await model.generateContent(prompt);
        const response = await result.response;
        const text = response.text();

        if (!text) {
            console.error(`[Gemini][${requestId}] EMPTY_RESPONSE | AI returned no text`);
            throw new Error("EMPTY_RESPONSE");
        }

        return { success: true, reply: text };
    } catch (error: any) {
        // SERVER LOG: Log the full error object safely
        const errorDetails = {
            name: error.name || "UnknownError",
            message: error.message || "No message",
            status: error.status || "UNKNOWN",
            statusCode: error.statusCode || (error.response ? error.response.status : "UNKNOWN"),
            statusText: error.statusText || (error.response ? error.response.statusText : "UNKNOWN"),
            reason: error.reason || "NONE"
        };

        console.error(`GEMINI_DEBUG_EXCEPTION | RequestId: ${requestId} | Details: ${JSON.stringify(errorDetails)}`);

        if (error.response && error.response.data) {
            // Log raw response body if available (usually in error.response.data or similar)
            try {
                console.error(`GEMINI_DEBUG_RESPONSE_BODY | RequestId: ${requestId} | Body: ${JSON.stringify(error.response.data)}`);
            } catch (e) {}
        }

        // Return structured debug info to Android
        const debugInfo = {
            requestId: requestId,
            provider: "google-generative-ai",
            model: PRIMARY_MODEL,
            apiVersion: "NOT_EXPOSED",
            httpStatus: errorDetails.statusCode,
            status: errorDetails.status,
            message: errorDetails.message,
            errorClass: errorDetails.name
        };

        return {
            success: false,
            debug: debugInfo,
            errorType: mapErrorType(errorDetails.message),
            message: errorDetails.message
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
    let apiKey = "";
    try {
        apiKey = geminiApiKey.value();
    } catch (e) {}

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
            error: error.message,
            statusCode: error.status || error.statusCode
        };
    }
}
