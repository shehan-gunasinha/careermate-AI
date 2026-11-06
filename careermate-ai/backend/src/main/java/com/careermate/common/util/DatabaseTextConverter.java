package com.careermate.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Applies {@link DatabaseText#safe(String)} to every String column so text that the database
 * encoding cannot represent never breaks a write. Auto-applied to all entities.
 */
@Converter(autoApply = true)
public class DatabaseTextConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return DatabaseText.safe(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData;
    }
}
