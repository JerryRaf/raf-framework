package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for FileTypeEnum.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class FileTypeEnumTest {

    @Test
    void allEnumValues_haveNonNullKeyAndValue() {
        for (FileTypeEnum e : FileTypeEnum.values()) {
            Assertions.assertNotNull(e.getKey(), "key should not be null for " + e.name());
            Assertions.assertFalse(e.getKey().isEmpty(), "key should not be empty for " + e.name());
            Assertions.assertNotNull(e.getValue(), "value should not be null for " + e.name());
            Assertions.assertFalse(e.getValue().isEmpty(), "value should not be empty for " + e.name());
        }
    }

    @Test
    void jpgEnum_hasCorrectMagicNumber() {
        Assertions.assertEquals("JPG", FileTypeEnum.JPG.getKey());
        Assertions.assertEquals("FFD8FF", FileTypeEnum.JPG.getValue());
    }

    @Test
    void pngEnum_hasCorrectMagicNumber() {
        Assertions.assertEquals("PNG", FileTypeEnum.PNG.getKey());
        Assertions.assertEquals("89504E47", FileTypeEnum.PNG.getValue());
    }

    @Test
    void pdfEnum_hasCorrectMagicNumber() {
        Assertions.assertEquals("PDF", FileTypeEnum.PDF.getKey());
        Assertions.assertEquals("255044462D312E", FileTypeEnum.PDF.getValue());
    }

    @Test
    void zipEnum_hasCorrectMagicNumber() {
        Assertions.assertEquals("ZIP", FileTypeEnum.ZIP.getKey());
        Assertions.assertEquals("504B0304", FileTypeEnum.ZIP.getValue());
    }

    @Test
    void enumCount_isExpected() {
        // Ensure no accidental removal of enum entries
        Assertions.assertTrue(FileTypeEnum.values().length >= 30);
    }
}
