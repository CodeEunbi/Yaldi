ALTER TABLE erd_columns
    ALTER COLUMN logical_name DROP NOT NULL,
    ALTER COLUMN physical_name DROP NOT NULL,
    ALTER COLUMN data_type DROP NOT NULL;
