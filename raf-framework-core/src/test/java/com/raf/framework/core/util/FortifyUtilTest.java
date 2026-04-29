package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for FortifyUtil (file name / path validation, no MultipartFile needed).
 *
 * @author Jerry
 * @since 2026-04-29
 */
class FortifyUtilTest {

    // ─── checkFileName(String) ───────────────────────────────────────────────

    @Test
    void checkFileName_returnsTrueForValidName() {
        Assertions.assertTrue(FortifyUtil.checkFileName("report.pdf"));
        Assertions.assertTrue(FortifyUtil.checkFileName("image.jpg"));
        Assertions.assertTrue(FortifyUtil.checkFileName("data.xlsx"));
    }

    @Test
    void checkFileName_returnsFalseForNull() {
        Assertions.assertFalse(FortifyUtil.checkFileName((String) null));
    }

    @Test
    void checkFileName_returnsFalseForEmpty() {
        Assertions.assertFalse(FortifyUtil.checkFileName(""));
    }

    @Test
    void checkFileName_returnsFalseForForwardSlash() {
        Assertions.assertFalse(FortifyUtil.checkFileName("dir/file.pdf"));
    }

    @Test
    void checkFileName_returnsFalseForBackSlash() {
        Assertions.assertFalse(FortifyUtil.checkFileName("dir\\file.pdf"));
    }

    @Test
    void checkFileName_returnsFalseForMultipleDots() {
        // "file.extra.pdf" -> base name "file.extra" contains a dot
        Assertions.assertFalse(FortifyUtil.checkFileName("file.extra.pdf"));
    }

    // ─── checkFileName(String, FileTypeEnum...) ──────────────────────────────

    @Test
    void checkFileName_withEnums_returnsTrueForMatchingType() {
        Assertions.assertTrue(FortifyUtil.checkFileName("photo.jpg", FileTypeEnum.JPG));
        Assertions.assertTrue(FortifyUtil.checkFileName("doc.pdf", FileTypeEnum.PDF));
    }

    @Test
    void checkFileName_withEnums_isCaseInsensitive() {
        Assertions.assertTrue(FortifyUtil.checkFileName("PHOTO.JPG", FileTypeEnum.JPG));
    }

    @Test
    void checkFileName_withEnums_returnsFalseForMismatchedType() {
        Assertions.assertFalse(FortifyUtil.checkFileName("photo.png", FileTypeEnum.JPG));
    }

    @Test
    void checkFileName_withEnums_returnsFalseForNull() {
        Assertions.assertFalse(FortifyUtil.checkFileName(null, FileTypeEnum.JPG));
    }

    @Test
    void checkFileName_withEnums_returnsFalseForEmpty() {
        Assertions.assertFalse(FortifyUtil.checkFileName("", FileTypeEnum.JPG));
    }

    @Test
    void checkFileName_withEnums_fallsBackToNoEnumCheckWhenEnumsEmpty() {
        // null fileTypeEnums -> delegates to checkFileName(String)
        Assertions.assertTrue(FortifyUtil.checkFileName("report.pdf", (FileTypeEnum[]) null));
        Assertions.assertTrue(FortifyUtil.checkFileName("report.pdf", new FileTypeEnum[0]));
    }

    @Test
    void checkFileName_withEnums_returnsFalseForSlashInName() {
        Assertions.assertFalse(FortifyUtil.checkFileName("dir/photo.jpg", FileTypeEnum.JPG));
    }

    // ─── checkPath ───────────────────────────────────────────────────────────

    @Test
    void checkPath_returnsTrueForValidPath() {
        Assertions.assertTrue(FortifyUtil.checkPath("/uploads/file.pdf"));
        Assertions.assertTrue(FortifyUtil.checkPath("/a/b/c/d/file.jpg"));
    }

    @Test
    void checkPath_returnsFalseForMultipleDots() {
        Assertions.assertFalse(FortifyUtil.checkPath("/uploads/file.extra.pdf"));
    }

    @Test
    void checkPath_returnsFalseForNoDot() {
        Assertions.assertFalse(FortifyUtil.checkPath("/uploads/filename"));
    }

    @Test
    void checkPath_returnsFalseForTooManySlashes() {
        // 11 slashes
        Assertions.assertFalse(FortifyUtil.checkPath("/a/b/c/d/e/f/g/h/i/j/k/file.pdf"));
    }

    @Test
    void checkPath_returnsFalseForDoubleSlash() {
        Assertions.assertFalse(FortifyUtil.checkPath("/uploads//file.pdf"));
    }

    @Test
    void checkPath_returnsFalseForBackSlash() {
        Assertions.assertFalse(FortifyUtil.checkPath("/uploads\\file.pdf"));
    }
}
