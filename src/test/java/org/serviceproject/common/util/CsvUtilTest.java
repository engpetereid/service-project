package org.serviceproject.common.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {

    @Test
    void escapeCell_null_returnsEmptyString() {
        assertEquals("", CsvUtil.escapeCell(null));
    }

    @Test
    void escapeCell_regularText_returnsUnchanged() {
        assertEquals("مارك مينا", CsvUtil.escapeCell("مارك مينا"));
    }

    @Test
    void escapeCell_formulaInjection_neutralizesWithQuote() {
        assertEquals("'=1+1", CsvUtil.escapeCell("=1+1"));
        assertEquals("'+cmd", CsvUtil.escapeCell("+cmd"));
        assertEquals("'-calc", CsvUtil.escapeCell("-calc"));
        assertEquals("'@SUM(A1:A10)", CsvUtil.escapeCell("@SUM(A1:A10)"));
        assertEquals("'\ttab", CsvUtil.escapeCell("\ttab"));
        assertEquals("\"'\rreturn\"", CsvUtil.escapeCell("\rreturn"));
    }

    @Test
    void escapeCell_specialCharacters_quotesAndDoublesQuotes() {
        // Contains comma
        assertEquals("\"القاهرة, مصر\"", CsvUtil.escapeCell("القاهرة, مصر"));

        // Contains quotes
        assertEquals("\"ملاحظة \"\"مهمة\"\"\"", CsvUtil.escapeCell("ملاحظة \"مهمة\""));

        // Contains newline
        assertEquals("\"سطر أول\nسطر ثان\"", CsvUtil.escapeCell("سطر أول\nسطر ثان"));
    }

    @Test
    void formatRow_formatsCrlf() {
        String row = CsvUtil.formatRow(List.of("أول", "ثاني", 123));
        assertEquals("أول,ثاني,123\r\n", row);
    }

    @Test
    void generateCsvBytes_includesUtf8BomAndArabicData() {
        List<String> headers = List.of("الاسم", "الهاتف");
        List<List<?>> rows = List.of(
                List.of("مينا جرجس", "01001111111"),
                List.of("=HYPERLINK(\"http://evil.com\")", "01122223333")
        );

        byte[] bytes = CsvUtil.generateCsvBytes(headers, rows);
        assertNotNull(bytes);

        String result = new String(bytes, StandardCharsets.UTF_8);

        // Verify UTF-8 BOM
        assertTrue(result.startsWith("\uFEFF"));

        // Verify headers
        assertTrue(result.contains("الاسم,الهاتف\r\n"));

        // Verify data
        assertTrue(result.contains("مينا جرجس,01001111111\r\n"));

        // Verify formula injection neutralized
        assertTrue(result.contains("'=HYPERLINK(\"\"http://evil.com\"\")"));
    }
}
