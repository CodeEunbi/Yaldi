package com.yaldi.domain.project.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectMemberActionTypeConverter implements AttributeConverter<ProjectMemberActionType, String> {

    @Override
    public String convertToDatabaseColumn(ProjectMemberActionType actionType) {
        if (actionType == null) {
            return null;
        }
        return actionType.getValue();
    }

    @Override
    public ProjectMemberActionType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        for (ProjectMemberActionType actionType : ProjectMemberActionType.values()) {
            if (actionType.getValue().equals(dbData)) {
                return actionType;
            }
        }

        throw new IllegalArgumentException("Unknown action type value: " + dbData);
    }
}
