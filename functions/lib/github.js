"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitReport = submitReport;
const axios_1 = __importDefault(require("axios"));
const params_1 = require("firebase-functions/params");
const githubToken = (0, params_1.defineSecret)("GITHUB_TOKEN");
async function submitReport(report, fingerprint) {
    const token = githubToken.value();
    if (!token) {
        throw new Error("GITHUB_TOKEN is not configured on server");
    }
    const repo = "gabitnur/TaxiGoalApp";
    const authHeader = { Authorization: `token ${token}` };
    try {
        const query = `repo:${repo} state:open "${fingerprint}"`;
        const searchUrl = `https://api.github.com/search/issues?q=${encodeURIComponent(query)}`;
        const searchRes = await axios_1.default.get(searchUrl, { headers: authHeader });
        if (searchRes.data.total_count > 0) {
            const issueNumber = searchRes.data.items[0].number;
            const commentBody = `New occurrence detected.\nReport ID: ${report.reportId}\nApp Version: ${report.appVersion}\nTimestamp: ${new Date(report.timestamp).toISOString()}`;
            await axios_1.default.post(`https://api.github.com/repos/${repo}/issues/${issueNumber}/comments`, { body: commentBody }, { headers: authHeader });
            return issueNumber;
        }
        else {
            const title = `[APP][${report.category}] ${report.safeErrorMessage || "Error Report"}`;
            const body = formatIssueBody(report, fingerprint);
            const createRes = await axios_1.default.post(`https://api.github.com/repos/${repo}/issues`, {
                title,
                body,
                labels: ["bug", "user-report", report.category.toLowerCase()]
            }, { headers: authHeader });
            return createRes.data.number;
        }
    }
    catch (error) {
        console.error("GitHub Error:", error.response?.data || error.message);
        throw error;
    }
}
function formatIssueBody(report, fingerprint) {
    return `
## User Report
${report.userDescription}

## App
Version: ${report.appVersion} (${report.versionCode})

## Device
Manufacturer: ${report.deviceManufacturer}
Model: ${report.deviceModel}
Android: ${report.androidVersion} (SDK ${report.sdkInt})

## Update
Current version: ${report.appVersion}
State: ${report.lastKnownUpdateState}

## Diagnostics
Category: ${report.category}
Error: ${report.errorType}
Message: ${report.safeErrorMessage}

\`\`\`
${report.safeStackTrace}
\`\`\`

## Fingerprint
${fingerprint}

## Report ID
${report.reportId}
`;
}
//# sourceMappingURL=github.js.map