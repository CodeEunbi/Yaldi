package com.yaldi.domain.datamodel.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataModelTypeConverter implements AttributeConverter<DataModelType, String> {

    @Override
    public String convertToDatabaseColumn(DataModelType modelType) {
        if (modelType == null) {
            return null;
        }
        return modelType.getValue();
    }

    @Override
    public DataModelType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        for (DataModelType modelType : DataModelType.values()) {
            if (modelType.getValue().equals(dbData)) {
                return modelType;
            }
        }

        throw new IllegalArgumentException("Unknown data model type value: " + dbData);
    }
}
