"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.sanitize = sanitize;
function sanitize(input) {
    if (!input)
        return "";
    return input
        .replace(/AIza[0-9A-Za-z-_]{35}/g, "[REDACTED_API_KEY]")
        .replace(/ya29\.[0-9A-Za-z-_]+/g, "[REDACTED_TOKEN]")
        .replace(/Bearer\s+[0-9A-Za-z-_.]+/gi, "Bearer [REDACTED]")
        .replace(/password=\S+/gi, "password=[REDACTED]")
        .replace(/secret=\S+/gi, "secret=[REDACTED]")
        .replace(/api_key=\S+/gi, "api_key=[REDACTED]");
}
//# sourceMappingURL=sanitizer.js.map