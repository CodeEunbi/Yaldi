-- =====================================================
-- WARN: sql 원본이지만 프로젝트의 스키마는 flyway (db.migration)에서 관리됩니다.
-- 02_constraints.sql
-- Foreign Key Constraints
-- =====================================================

-- User-related constraints
ALTER TABLE IF EXISTS user_social_accounts
    ADD CONSTRAINT fk_user_social_accounts_user_key FOREIGN KEY (user_key) REFERENCES users(user_key) ON DELETE CASCADE;

-- Team-related constraints
ALTER TABLE IF EXISTS teams
    ADD CONSTRAINT fk_teams_owned_by FOREIGN KEY (owned_by) REFERENCES users(user_key) ON DELETE CASCADE;

-- Team name uniqueness constraint (활성 팀만, deleted_at IS NULL인 경우)
-- PostgreSQL partial unique index
CREATE UNIQUE INDEX IF NOT EXISTS uk_teams_name_active
    ON teams (name)
    WHERE deleted_at IS NULL;

ALTER TABLE IF EXISTS user_team_relations
    ADD CONSTRAINT fk_user_team_relations_user_key FOREIGN KEY (user_key) REFERENCES users(user_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS user_team_relations
    ADD CONSTRAINT fk_user_team_relations_team_key FOREIGN KEY (team_key) REFERENCES teams(team_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS user_team_history
    ADD CONSTRAINT fk_user_team_history_team_key FOREIGN KEY (team_key) REFERENCES teams(team_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS user_team_history
    ADD CONSTRAINT fk_user_team_history_actor_key FOREIGN KEY (actor_key) REFERENCES users(user_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS user_team_history
    ADD CONSTRAINT fk_user_team_history_target_key FOREIGN KEY (target_key) REFERENCES users(user_key) ON DELETE CASCADE;

-- Project-related constraints
ALTER TABLE IF EXISTS projects
    ADD CONSTRAINT fk_projects_team_key FOREIGN KEY (team_key) REFERENCES teams(team_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS project_member_relations
    ADD CONSTRAINT fk_project_member_relations_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS project_member_relations
    ADD CONSTRAINT fk_project_member_relations_member_key FOREIGN KEY (member_key) REFERENCES users(user_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS project_member_history
    ADD CONSTRAINT fk_project_member_history_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS project_member_history
    ADD CONSTRAINT fk_project_member_history_actor_key FOREIGN KEY (actor_key) REFERENCES users(user_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS project_member_history
    ADD CONSTRAINT fk_project_member_history_target_key FOREIGN KEY (target_key) REFERENCES users(user_key) ON DELETE CASCADE;

-- ERD-related constraints
ALTER TABLE IF EXISTS erd_tables
    ADD CONSTRAINT fk_erd_tables_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS erd_columns
    ADD CONSTRAINT fk_erd_columns_table_key FOREIGN KEY (table_key) REFERENCES erd_tables(table_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS erd_relations
    ADD CONSTRAINT fk_erd_relations_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS erd_relations
    ADD CONSTRAINT fk_erd_relations_from_table_key FOREIGN KEY (from_table_key) REFERENCES erd_tables(table_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS erd_relations
    ADD CONSTRAINT fk_erd_relations_to_table_key FOREIGN KEY (to_table_key) REFERENCES erd_tables(table_key) ON DELETE CASCADE;

-- Comment-related constraints
ALTER TABLE IF EXISTS comments
    ADD CONSTRAINT fk_comments_user_key FOREIGN KEY (user_key) REFERENCES users(user_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS comments
    ADD CONSTRAINT fk_comments_table_key FOREIGN KEY (table_key) REFERENCES erd_tables(table_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS replies
    ADD CONSTRAINT fk_replies_comment_key FOREIGN KEY (comment_key) REFERENCES comments(comment_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS replies
    ADD CONSTRAINT fk_replies_user_key FOREIGN KEY (user_key) REFERENCES users(user_key) ON DELETE CASCADE;

-- Version-related constraints
ALTER TABLE IF EXISTS versions
    ADD CONSTRAINT fk_versions_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS snapshots
    ADD CONSTRAINT fk_snapshots_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS snapshots
    ADD CONSTRAINT fk_snapshots_created_by FOREIGN KEY (created_by) REFERENCES users(user_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS edit_history
    ADD CONSTRAINT fk_edit_history_user_key FOREIGN KEY (user_key) REFERENCES users(user_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS edit_history
    ADD CONSTRAINT fk_edit_history_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

-- Data model-related constraints
ALTER TABLE IF EXISTS data_models
    ADD CONSTRAINT fk_data_models_project_key FOREIGN KEY (project_key) REFERENCES projects(project_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS data_model_erd_column_relations
    ADD CONSTRAINT fk_data_model_erd_column_relations_column_key FOREIGN KEY (column_key) REFERENCES erd_columns(column_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS data_model_erd_column_relations
    ADD CONSTRAINT fk_data_model_erd_column_relations_model_key FOREIGN KEY (model_key) REFERENCES data_models(model_key) ON DELETE CASCADE;

ALTER TABLE IF EXISTS data_models
    ADD CONSTRAINT fk_data_models_source_table_key FOREIGN KEY (source_table_key) REFERENCES erd_tables(table_key) ON DELETE SET NULL;

-- Mock data constraints
ALTER TABLE IF EXISTS mock_data
    ADD CONSTRAINT fk_mock_data_version_key FOREIGN KEY (version_key) REFERENCES versions(version_key) ON DELETE CASCADE;

-- Notification constraints
ALTER TABLE IF EXISTS notifications
    ADD CONSTRAINT fk_notifications_user_key FOREIGN KEY (user_key) REFERENCES users(user_key) ON DELETE CASCADE;

