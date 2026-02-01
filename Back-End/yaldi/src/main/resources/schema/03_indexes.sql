-- =====================================================
-- WARN: sql 원본이지만 프로젝트의 스키마는 flyway (db.migration)에서 관리됩니다.
-- 03_indexes.sql
-- Database Indexes for Performance
-- =====================================================

-- Partial UNIQUE indexes for soft delete support
CREATE UNIQUE INDEX idx_users_email_active ON users(email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_users_nickname_active ON users(nickname) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_teams_owner_name_active ON teams(owned_by, name) WHERE deleted_at IS NULL;

-- Performance indexes for foreign keys
CREATE INDEX idx_snapshots_project_key ON snapshots(project_key);
CREATE INDEX idx_erd_columns_table_key ON erd_columns(table_key);
CREATE INDEX idx_comments_table_key ON comments(table_key);
CREATE INDEX idx_comments_user_key ON comments(user_key);
CREATE INDEX idx_versions_project_key ON versions(project_key);
CREATE INDEX idx_erd_tables_project_key ON erd_tables(project_key);
CREATE INDEX idx_erd_relations_project_key ON erd_relations(project_key);
CREATE INDEX idx_erd_relations_from_table_key ON erd_relations(from_table_key);
CREATE INDEX idx_erd_relations_to_table_key ON erd_relations(to_table_key);
CREATE INDEX idx_replies_comment_key ON replies(comment_key);
CREATE INDEX idx_replies_user_key ON replies(user_key);
CREATE INDEX idx_notifications_user_key ON notifications(user_key);
CREATE INDEX idx_data_model_erd_column_relations_column_key ON data_model_erd_column_relations(column_key);
CREATE INDEX idx_data_model_erd_column_relations_model_key ON data_model_erd_column_relations(model_key);

-- Soft delete indexes (for filtering deleted records)
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_teams_deleted_at ON teams(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_erd_tables_deleted_at ON erd_tables(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_erd_relations_deleted_at ON erd_relations(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_erd_columns_deleted_at ON erd_columns(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_replies_deleted_at ON replies(deleted_at) WHERE deleted_at IS NULL;

-- Composite indexes for common queries
CREATE INDEX idx_notifications_user_read ON notifications(user_key, read_at);
CREATE INDEX idx_versions_project_public ON versions(project_key, is_public);

-- Additional performance indexes
CREATE INDEX idx_projects_team_key ON projects(team_key);
CREATE INDEX idx_teams_owned_by ON teams(owned_by);
CREATE INDEX idx_edit_history_project_key ON edit_history(project_key);
CREATE INDEX idx_edit_history_user_key ON edit_history(user_key);
CREATE INDEX idx_user_team_history_team_key ON user_team_history(team_key);
CREATE INDEX idx_project_member_history_project_key ON project_member_history(project_key);
CREATE INDEX idx_data_models_project_key ON data_models(project_key);
CREATE INDEX idx_data_models_source_table_key ON data_models(source_table_key);
CREATE INDEX idx_data_models_last_synced_at ON data_models(last_synced_at);
CREATE INDEX idx_mock_data_version_key ON mock_data(version_key);

-- Timestamp-based indexes for queries sorted by creation/update time
CREATE INDEX idx_comments_created_at ON comments(created_at DESC);
CREATE INDEX idx_replies_created_at ON replies(created_at DESC);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_versions_created_at ON versions(created_at DESC);
