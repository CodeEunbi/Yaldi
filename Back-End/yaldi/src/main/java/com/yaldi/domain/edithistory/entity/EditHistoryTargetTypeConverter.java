package com.yaldi.domain.edithistory.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EditHistoryTargetTypeConverter implements AttributeConverter<EditHistoryTargetType, String> {

    @Override
    public String convertToDatabaseColumn(EditHistoryTargetType targetType) {
        if (targetType == null) {
            return null;
        }
        return targetType.getValue();
    }

    @Override
    public EditHistoryTargetType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        for (EditHistoryTargetType targetType : EditHistoryTargetType.values()) {
            if (targetType.getValue().equals(dbData)) {
                return targetType;
            }
        }

        throw new IllegalArgumentException("Unknown target type value: " + dbData);
    }
}
