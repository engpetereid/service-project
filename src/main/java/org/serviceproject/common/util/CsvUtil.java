package org.serviceproject.common.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility for generating RFC-4180 compliant CSV files with Arabic UTF-8 BOM
 * and CSV formula injection prevention.
 */
public final class CsvUtil {

    /** UTF-8 Byte Order Mark (BOM) to force spreadsheet software (like Excel) to render Arabic properly */
    public static final String UTF8_BOM = "\uFEFF";

    private CsvUtil() {}

    /**
     * Formats a single value into an RFC-4180 and security-safe CSV cell string.
     */
    public static String escapeCell(Object value) {
        if (value == null) {
            return "";
        }

        String str = String.valueOf(value);
        if (str.isEmpty()) {
            return "";
        }

        // CSV Injection Prevention: Neutralize formula triggers (=, +, -, @, \t, \r)
        char firstChar = str.charAt(0);
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
            str = "'" + str;
        }

        // RFC-4180: If the string contains comma, quote, or newline, escape quotes and wrap in quotes
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }

        return str;
    }

    /**
     * Formats a single row of cells into a comma-separated line ending with CRLF.
     */
    public static String formatRow(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "\r\n";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeCell(values.get(i)));
        }
        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * Generates complete CSV content as UTF-8 encoded bytes with the BOM prefix.
     */
    public static byte[] generateCsvBytes(List<String> headers, List<List<?>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(UTF8_BOM);

        if (headers != null && !headers.isEmpty()) {
            sb.append(formatRow(headers));
        }

        if (rows != null) {
            for (List<?> row : rows) {
                sb.append(formatRow(row));
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
