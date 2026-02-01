package com.yaldi.domain.erd.export;

import com.yaldi.domain.erd.entity.SqlDialect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SQL Generator Factory
 */
@Component
@RequiredArgsConstructor
public class SqlGeneratorFactory {

    private final PostgreSqlGenerator postgreSqlGenerator;
    private final MySqlGenerator mySqlGenerator;

    public SqlGenerator getGenerator(SqlDialect dialect) {
        switch (dialect) {
            case POSTGRESQL:
                return postgreSqlGenerator;
            case MYSQL:
                return mySqlGenerator;
            default:
                throw new IllegalArgumentException("Unsupported SQL dialect: " + dialect);
        }
    }
}
