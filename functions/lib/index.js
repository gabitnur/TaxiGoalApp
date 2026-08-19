"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitDiagnosticReport = exports.verifyGeminiModel = exports.geminiChat = void 0;
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
const gemini = __importStar(require("./gemini"));
const github = __importStar(require("./github"));
const sanitizer_1 = require("./sanitizer");
const fingerprint_1 = require("./fingerprint");
admin.initializeApp();
exports.geminiChat = (0, https_1.onCall)({
    secrets: ["GEMINI_API_KEY"],
    region: "us-central1"
}, async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Auth required");
    }
    const { message, safeContext, requestId } = request.data;
    if (!message) {
        throw new https_1.HttpsError("invalid-argument", "Message is missing");
    }
    const currentRequestId = requestId || `FUN-${Date.now()}`;
    try {
        return await gemini.chat(message, safeContext || "", currentRequestId);
    }
    catch (error) {
        console.error(`INDEX_DEBUG_ERROR | RequestId: ${currentRequestId} | Message: ${error.message} | Stack: ${error.stack}`);
        return {
            success: false,
            errorType: "UNKNOWN_ERROR",
            message: error.message,
            debug: { requestId: currentRequestId, internal: error.stack }
        };
    }
});
exports.verifyGeminiModel = (0, https_1.onCall)({
    secrets: ["GEMINI_API_KEY"],
    region: "us-central1"
}, async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Auth required");
    }
    try {
        const result = await gemini.testConnectivity();
        return { success: true, result };
    }
    catch (error) {
        return { success: false, message: error.message };
    }
});
exports.submitDiagnosticReport = (0, https_1.onCall)({
    secrets: ["GITHUB_TOKEN"],
    region: "us-central1"
}, async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Auth required");
    }
    const report = request.data;
    report.userDescription = (0, sanitizer_1.sanitize)(report.userDescription);
    report.safeErrorMessage = (0, sanitizer_1.sanitize)(report.safeErrorMessage);
    report.safeStackTrace = (0, sanitizer_1.sanitize)(report.safeStackTrace);
    if (report.diagnosticLogs) {
        report.diagnosticLogs = report.diagnosticLogs.map((log) => (0, sanitizer_1.sanitize)(log));
    }
    const fingerprint = (0, fingerprint_1.generateFingerprint)(report.category, report.errorType, report.safeErrorMessage, report.safeStackTrace);
    try {
        const issueNumber = await github.submitReport(report, fingerprint);
        return { success: true, reportId: report.reportId, issueNumber };
    }
    catch (error) {
        return { success: false, message: error.message };
    }
});
//# sourceMappingURL=index.js.map