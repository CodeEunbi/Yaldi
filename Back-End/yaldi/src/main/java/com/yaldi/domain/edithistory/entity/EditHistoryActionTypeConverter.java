package com.yaldi.domain.edithistory.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EditHistoryActionTypeConverter implements AttributeConverter<EditHistoryActionType, String> {

    @Override
    public String convertToDatabaseColumn(EditHistoryActionType actionType) {
        if (actionType == null) {
            return null;
        }
        return actionType.getValue();
    }

    @Override
    public EditHistoryActionType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        for (EditHistoryActionType actionType : EditHistoryActionType.values()) {
            if (actionType.getValue().equals(dbData)) {
                return actionType;
            }
        }

        throw new IllegalArgumentException("Unknown action type value: " + dbData);
    }
}
