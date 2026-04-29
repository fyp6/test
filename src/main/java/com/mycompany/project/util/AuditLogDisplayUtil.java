package com.mycompany.project.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AuditLogDisplayUtil {
    private AuditLogDisplayUtil() {
    }

    public static String formatDetails(String action, String details) {
        if (details == null || details.isBlank()) {
            return "";
        }

        String normalized = details.replace("\r\n", "\n").replace('\r', '\n').trim();

        if ("ISSUE_REPORTED".equals(action)) {
            String issueDisplay = formatIssueReportedDetails(normalized);
            if (issueDisplay != null && !issueDisplay.isBlank()) {
                return issueDisplay;
            }
        }

        String keyValueDisplay = formatKeyValueDetails(normalized);
        if (keyValueDisplay != null && !keyValueDisplay.isBlank()) {
            return keyValueDisplay;
        }

        return escapeHtml(normalized).replace("\n", "<br>");
    }

    private static String formatIssueReportedDetails(String details) {
        Map<String, String> fields = parseKeyValuePairs(details);
        if (fields.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"audit-summary\">");
        appendField(builder, "Issue Type", humanizeCode(fields.get("issueType")));
        appendField(builder, "Severity", humanizeCode(fields.get("severity")));
        appendField(builder, "Clinic", valueOrDefault(fields.get("clinic")));
        appendField(builder, "Service", valueOrDefault(fields.get("service")));
        appendField(builder, "Title", valueOrDefault(fields.get("title")));
        appendField(builder, "Details", valueOrDefault(fields.get("details")));
        builder.append("</div>");
        return builder.toString();
    }

    private static String formatKeyValueDetails(String details) {
        Map<String, String> fields = parseKeyValuePairs(details);
        if (fields.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"audit-summary\">");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            appendField(builder, humanizeCode(entry.getKey()), formatDisplayValue(entry.getValue()));
        }
        builder.append("</div>");
        return builder.toString();
    }

    private static Map<String, String> parseKeyValuePairs(String details) {
        Map<String, String> fields = new LinkedHashMap<>();
        String[] pairs = details.split(";\\s*");
        for (String pair : pairs) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = pair.substring(0, separator).trim();
            String value = pair.substring(separator + 1).trim();
            if (!key.isEmpty()) {
                fields.put(key, value);
            }
        }
        return fields;
    }

    private static void appendField(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        builder.append("<div class=\"audit-summary__item\">")
                .append("<span class=\"audit-summary__label\">").append(escapeHtml(label)).append("</span>")
                .append("<span class=\"audit-summary__value\">").append(escapeHtml(value.trim())).append("</span>")
                .append("</div>");
    }

    private static String valueOrDefault(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private static String formatDisplayValue(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        String trimmed = value.trim();
        if (trimmed.indexOf(' ') < 0 && (trimmed.indexOf('_') >= 0 || trimmed.indexOf('-') >= 0)) {
            return humanizeCode(trimmed);
        }
        return trimmed;
    }

    private static String humanizeCode(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        String normalized = value.replace('_', ' ').replace('-', ' ');
        normalized = normalized.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        normalized = normalized.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isWhitespace(ch)) {
                builder.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(ch));
                capitalizeNext = false;
            } else {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    private static String escapeHtml(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\'':
                    builder.append("&#39;");
                    break;
                default:
                    builder.append(ch);
                    break;
            }
        }
        return builder.toString();
    }
}