import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";

const geminiApiKey = defineSecret("GEMINI_API_KEY");

// Primary model identifier
const PRIMARY_MODEL = "gemini-flash-latest";

export async function chat(message: string, context: string) {
    const apiKey = geminiApiKey.value();
    if (!apiKey) {
        throw new Error("GEMINI_API_KEY is not configured on server");
    }

    const genAI = new GoogleGenerativeAI(apiKey);

    // According to Google AI SDK, "gemini-flash-latest" is the alias to the latest 1.5 Flash model.
    const model = genAI.getGenerativeModel({ model: PRIMARY_MODEL });

    const prompt = `${context}\n\nUser Question: ${message}`;

    try {
        console.log(`Gemini: Using model ${PRIMARY_MODEL}`);
        const result = await model.generateContent(prompt);
        const response = await result.response;
        const text = response.text();

        if (!text) {
            throw new Error("EMPTY_RESPONSE");
        }

        return text;
    } catch (error: any) {
        console.error("Gemini Error:", error);

        // Detailed error mapping
        if (error.message?.includes("404") || error.message?.includes("not found")) {
            return { errorType: "MODEL_NOT_FOUND", message: `Model ${PRIMARY_MODEL} not found or unavailable` };
        }
        if (error.message?.includes("503") || error.message?.includes("demand")) {
            return { errorType: "TEMPORARILY_UNAVAILABLE", message: "AI service high demand" };
        }
        if (error.message?.includes("429")) {
            return { errorType: "RATE_LIMITED", message: "Too many requests" };
        }
        if (error.message?.includes("401") || error.message?.includes("API key")) {
            return { errorType: "AUTH_ERROR", message: "Invalid API Key on server" };
        }

        return { errorType: "UNKNOWN_ERROR", message: error.message };
    }
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
