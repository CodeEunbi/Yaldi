package com.yaldi.domain.team.entity;

import com.yaldi.domain.team.entity.UserTeamActionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserTeamActionTypeConverter implements AttributeConverter<UserTeamActionType, String> {

    @Override
    public String convertToDatabaseColumn(UserTeamActionType actionType) {
        if (actionType == null) {
            return null;
        }
        return actionType.getValue();
    }

    @Override
    public UserTeamActionType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        for (UserTeamActionType actionType : UserTeamActionType.values()) {
            if (actionType.getValue().equals(dbData)) {
                return actionType;
            }
        }

        throw new IllegalArgumentException("Unknown action type value: " + dbData);
    }
}
