import axios from 'axios';
import { defineSecret } from "firebase-functions/params";

const githubToken = defineSecret("GITHUB_TOKEN");

export async function submitReport(report: any, fingerprint: string) {
    const token = githubToken.value();
    if (!token) {
        throw new Error("GITHUB_TOKEN is not configured on server");
    }

    const repo = "gabitnur/TaxiGoalApp";
    const authHeader = { Authorization: `token ${token}` };

    // Check for existing issue with same fingerprint
    try {
        const query = `repo:${repo} state:open "${fingerprint}"`;
        const searchUrl = `https://api.github.com/search/issues?q=${encodeURIComponent(query)}`;
        const searchRes = await axios.get(searchUrl, { headers: authHeader });

        if (searchRes.data.total_count > 0) {
            const issueNumber = searchRes.data.items[0].number;
            const commentBody = `New occurrence detected.\nReport ID: ${report.reportId}\nApp Version: ${report.appVersion}\nTimestamp: ${new Date(report.timestamp).toISOString()}`;
            await axios.post(`https://api.github.com/repos/${repo}/issues/${issueNumber}/comments`, { body: commentBody }, { headers: authHeader });
            return issueNumber;
        } else {
            // Create new issue
            const title = `[APP][${report.category}] ${report.safeErrorMessage || "Error Report"}`;
            const body = formatIssueBody(report, fingerprint);
            const createRes = await axios.post(`https://api.github.com/repos/${repo}/issues`, {
                title,
                body,
                labels: ["bug", "user-report", report.category.toLowerCase()]
            }, { headers: authHeader });
            return createRes.data.number;
        }
    } catch (error: any) {
        console.error("GitHub Error:", error.response?.data || error.message);
        throw error;
    }
}

function formatIssueBody(report: any, fingerprint: string): string {
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
