package com.careermate.common.util;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTextTest {

    @Test
    void removesControlCharactersButKeepsWhitespace() {
        assertEquals("a\nb\tc", DatabaseText.safe("a\u0000\nb\tc\u0083"));
    }

    @Test
    void replacesCharactersTheDatabaseEncodingCannotStore() {
        String cleaned = DatabaseText.transcode("Resume \u0083 \uD83D\uDE00 café", Charset.forName("windows-1252"));
        assertFalse(cleaned.contains("\u0083"));
        assertFalse(cleaned.contains("\uD83D\uDE00"));
        assertTrue(cleaned.contains("café"));
    }
}
