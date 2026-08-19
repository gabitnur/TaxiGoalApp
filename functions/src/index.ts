import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as gemini from "./gemini";
import * as github from "./github";
import { sanitize } from "./sanitizer";
import { generateFingerprint } from "./fingerprint";

admin.initializeApp();

export const geminiChat = functions.https.onCall({
    secrets: ["GEMINI_API_KEY"],
    region: "us-central1"
}, async (request) => {
    if (!request.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Auth required");
    }

    const { message, safeContext, requestId } = request.data;
    if (!message) {
        throw new functions.https.HttpsError("invalid-argument", "Message is missing");
    }

    const currentRequestId = requestId || `FUN-${Date.now()}`;

    try {
        // chat function now returns a structured object { success, reply?, debug?, errorType?, message? }
        return await gemini.chat(message, safeContext || "", currentRequestId);
    } catch (error: any) {
        return {
            success: false,
            errorType: "UNKNOWN_ERROR",
            message: error.message,
            debug: { requestId: currentRequestId, internal: error.stack }
        };
    }
});

/**
 * Endpoint for verifying Gemini model availability.
 */
export const verifyGeminiModel = functions.https.onCall({
    secrets: ["GEMINI_API_KEY"],
    region: "us-central1"
}, async (request) => {
    if (!request.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Auth required");
    }

    try {
        const result = await gemini.testConnectivity();
        return { success: true, result };
    } catch (error: any) {
        return { success: false, message: error.message };
    }
});

export const submitDiagnosticReport = functions.https.onCall({
    secrets: ["GITHUB_TOKEN"],
    region: "us-central1"
}, async (request) => {
    if (!request.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Auth required");
    }

    const report = request.data;

    // Sanitize everything
    report.userDescription = sanitize(report.userDescription);
    report.safeErrorMessage = sanitize(report.safeErrorMessage);
    report.safeStackTrace = sanitize(report.safeStackTrace);
    if (report.diagnosticLogs) {
        report.diagnosticLogs = report.diagnosticLogs.map((log: string) => sanitize(log));
    }

    const fingerprint = generateFingerprint(
        report.category,
        report.errorType,
        report.safeErrorMessage,
        report.safeStackTrace
    );

    try {
        const issueNumber = await github.submitReport(report, fingerprint);
        return { success: true, reportId: report.reportId, issueNumber };
    } catch (error: any) {
        return { success: false, message: error.message };
    }
});
