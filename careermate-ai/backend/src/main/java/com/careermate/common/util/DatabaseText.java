package com.careermate.common.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Makes text writable to databases whose encoding is not UTF-8.
 *
 * <p>Local PostgreSQL clusters on Windows are commonly created with WIN1252 even though the
 * application treats everything as UTF-8. Extracted resume text can contain characters that
 * PostgreSQL cannot represent in that encoding (for example icon-font glyphs from a PDF), which
 * makes the insert fail. This utility removes control characters and replaces anything the
 * database encoding cannot store, so user data is accepted instead of failing with a 409.</p>
 */
public final class DatabaseText {
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}&&[^\\t\\n\\r]]");
    private static final AtomicReference<Charset> CHARSET = new AtomicReference<>(StandardCharsets.UTF_8);
    private static final Map<String, String> POSTGRES_CHARSETS = Map.of(
            "UTF8", "UTF-8",
            "WIN1250", "windows-1250",
            "WIN1251", "windows-1251",
            "WIN1252", "windows-1252",
            "LATIN1", "ISO-8859-1",
            "LATIN2", "ISO-8859-2",
            "SQL_ASCII", "UTF-8");

    private DatabaseText() {
    }

    /** Configures the target encoding from a PostgreSQL encoding name such as {@code WIN1252}. */
    public static void configure(String postgresEncoding) {
        if (postgresEncoding == null || postgresEncoding.isBlank()) {
            return;
        }
        String name = POSTGRES_CHARSETS.get(postgresEncoding.trim().toUpperCase(Locale.ROOT));
        if (name == null) {
            try {
                name = Charset.isSupported(postgresEncoding) ? postgresEncoding : "UTF-8";
            } catch (Exception ignored) {
                name = "UTF-8";
            }
        }
        CHARSET.set(Charset.forName(name));
    }

    /** Returns a value that can always be persisted in the configured database encoding. */
    public static String safe(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = CONTROL.matcher(value).replaceAll("");
        Charset charset = CHARSET.get();
        if (StandardCharsets.UTF_8.equals(charset)) {
            return cleaned;
        }
        return transcode(cleaned, charset);
    }

    static String transcode(String value, Charset charset) {
        if (value.isEmpty()) {
            return value;
        }
        CharsetEncoder encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith(new byte[]{(byte) '?'});
        try {
            ByteBuffer encoded = encoder.encode(CharBuffer.wrap(value));
            return new String(encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining(), charset);
        } catch (CharacterCodingException e) {
            return value.replaceAll("[^\\x00-\\x7F]", "?");
        }
    }
}
