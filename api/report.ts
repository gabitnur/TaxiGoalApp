import { VercelRequest, VercelResponse } from '@vercel/node';
import axios from 'axios';
import { verifyAuth } from './_utils/auth';
import { sanitize } from './_utils/sanitizer';
import { generateFingerprint } from './_utils/fingerprint';

export default async function handler(req: VercelRequest, res: VercelResponse) {
    if (req.method !== 'POST') {
        return res.status(405).json({ success: false, message: 'Method Not Allowed' });
    }

    try {
        await verifyAuth(req.headers.authorization);
        const report = req.body;

        // Sanitize
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

        const issueNumber = await submitToGithub(report, fingerprint);

        return res.status(200).json({
            success: true,
            reportId: report.reportId,
            issueNumber
        });

    } catch (error: any) {
        console.error('Report submission failed:', error.message);
        return res.status(500).json({ success: false, message: error.message });
    }
}

async function submitToGithub(report: any, fingerprint: string) {
    const token = process.env.GITHUB_TOKEN;
    if (!token) throw new Error('GITHUB_TOKEN missing');

    const repo = "gabitnur/TaxiGoalApp";
    const authHeader = { Authorization: `token ${token}` };

    const query = `repo:${repo} state:open "${fingerprint}"`;
    const searchUrl = `https://api.github.com/search/issues?q=${encodeURIComponent(query)}`;
    const searchRes = await axios.get(searchUrl, { headers: authHeader });

    if (searchRes.data.total_count > 0) {
        const issueNumber = searchRes.data.items[0].number;
        const commentBody = `New occurrence detected.\nReport ID: ${report.reportId}\nApp Version: ${report.appVersion}\nTimestamp: ${new Date().toISOString()}`;
        await axios.post(`https://api.github.com/repos/${repo}/issues/${issueNumber}/comments`, { body: commentBody }, { headers: authHeader });
        return issueNumber;
    } else {
        const title = `[APP][${report.category}] ${report.safeErrorMessage || "Error Report"}`;
        const body = `
## User Report
${report.userDescription}

## App
Version: ${report.appVersion} (${report.versionCode})

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
        const createRes = await axios.post(`https://api.github.com/repos/${repo}/issues`, {
            title,
            body,
            labels: ["bug", "user-report", report.category.toLowerCase()]
        }, { headers: authHeader });
        return createRes.data.number;
    }
}
