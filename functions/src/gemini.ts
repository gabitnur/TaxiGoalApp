import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";

const geminiApiKey = defineSecret("GEMINI_API_KEY");

export async function chat(message: string, context: string) {
    const apiKey = geminiApiKey.value();
    if (!apiKey) {
        throw new Error("GEMINI_API_KEY is not configured on server");
    }

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

    const prompt = `${context}\n\nUser Question: ${message}`;

    try {
        const result = await model.generateContent(prompt);
        const response = await result.response;
        return response.text();
    } catch (error: any) {
        console.error("Gemini Error:", error);
        if (error.message?.includes("503") || error.message?.includes("demand")) {
            return { errorType: "TEMPORARILY_UNAVAILABLE", message: "AI service high demand" };
        }
        if (error.message?.includes("429")) {
            return { errorType: "RATE_LIMITED", message: "Too many requests" };
        }
        return { errorType: "UNKNOWN_ERROR", message: error.message };
    }
}
