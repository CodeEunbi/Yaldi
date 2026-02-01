package com.yaldi.domain.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProviderConverter implements AttributeConverter<Provider, String> {

    @Override
    public String convertToDatabaseColumn(Provider provider) {
        if (provider == null) {
            return null;
        }
        return provider.getValue();
    }

    @Override
    public Provider convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        for (Provider provider : Provider.values()) {
            if (provider.getValue().equals(dbData)) {
                return provider;
            }
        }

        throw new IllegalArgumentException("Unknown provider value: " + dbData);
    }
}
