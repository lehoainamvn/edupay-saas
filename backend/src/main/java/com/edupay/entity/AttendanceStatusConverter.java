package com.edupay.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter cho AttendanceStatus, đảm bảo tương thích ngược với dữ liệu cũ (ABSENT_PERMITTED).
 */
@Converter(autoApply = true)
public class AttendanceStatusConverter implements AttributeConverter<AttendanceStatus, String> {

    @Override
    public String convertToDatabaseColumn(AttendanceStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public AttendanceStatus convertToEntityAttribute(String dbData) {
        return AttendanceStatus.fromString(dbData);
    }
}

