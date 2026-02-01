-- =====================================================
-- WARN: sql 원본이지만 프로젝트의 스키마는 flyway (db.migration)에서 관리됩니다.
-- 04_triggers.sql
-- Soft Delete Cascade Triggers
-- =====================================================

-- Team 삭제 시 하위 Projects도 soft delete
CREATE OR REPLACE FUNCTION soft_delete_cascade_team_projects()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        UPDATE projects
        SET deleted_at = NEW.deleted_at
        WHERE team_key = NEW.team_key
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_soft_delete_cascade_team_projects
AFTER UPDATE ON teams
FOR EACH ROW
EXECUTE FUNCTION soft_delete_cascade_team_projects();

-- Project 삭제 시 하위 ERD Tables, Versions, Data Models도 soft delete
CREATE OR REPLACE FUNCTION soft_delete_cascade_project_children()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        -- ERD Tables 삭제
        UPDATE erd_tables
        SET deleted_at = NEW.deleted_at
        WHERE project_key = NEW.project_key
          AND deleted_at IS NULL;

        -- Versions 삭제
        UPDATE versions
        SET deleted_at = NEW.deleted_at
        WHERE project_key = NEW.project_key
          AND deleted_at IS NULL;

        -- Data Models 삭제
        UPDATE data_models
        SET deleted_at = NEW.deleted_at
        WHERE project_key = NEW.project_key
          AND deleted_at IS NULL;

        -- Snapshots 삭제
        UPDATE snapshots
        SET deleted_at = NEW.deleted_at
        WHERE project_key = NEW.project_key
          AND deleted_at IS NULL;

        -- ERD Relations 삭제 (project_key로 직접 참조)
        UPDATE erd_relations
        SET deleted_at = NEW.deleted_at
        WHERE project_key = NEW.project_key
          AND deleted_at IS NULL;

        -- Comments 삭제 (table_key를 통해 간접 참조)
        UPDATE comments
        SET deleted_at = NEW.deleted_at
        WHERE table_key IN (
            SELECT table_key FROM erd_tables WHERE project_key = NEW.project_key
        ) AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_soft_delete_cascade_project_children
AFTER UPDATE ON projects
FOR EACH ROW
EXECUTE FUNCTION soft_delete_cascade_project_children();

-- Table 삭제 시 하위 Columns와 Relations도 soft delete
CREATE OR REPLACE FUNCTION soft_delete_cascade_table_children()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        -- Columns 삭제
        UPDATE erd_columns
        SET deleted_at = NEW.deleted_at
        WHERE table_key = NEW.table_key
          AND deleted_at IS NULL;

        -- Relations 삭제 (from_table 또는 to_table)
        UPDATE erd_relations
        SET deleted_at = NEW.deleted_at
        WHERE (from_table_key = NEW.table_key OR to_table_key = NEW.table_key)
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_soft_delete_cascade_table_children
AFTER UPDATE ON erd_tables
FOR EACH ROW
EXECUTE FUNCTION soft_delete_cascade_table_children();

-- Version 삭제 시 하위 Mock Data도 soft delete
CREATE OR REPLACE FUNCTION soft_delete_cascade_version_children()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        -- Mock Data 삭제
        UPDATE mock_data
        SET deleted_at = NEW.deleted_at
        WHERE version_key = NEW.version_key
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_soft_delete_cascade_version_children
AFTER UPDATE ON versions
FOR EACH ROW
EXECUTE FUNCTION soft_delete_cascade_version_children();

-- User 삭제 시 연관 데이터 처리
CREATE OR REPLACE FUNCTION soft_delete_cascade_user_data()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        -- 1. User Social Accounts 완전 삭제
        DELETE FROM user_social_accounts
        WHERE user_key = NEW.user_key;

        -- 2. Snapshots 완전 삭제
        DELETE FROM snapshots
        WHERE created_by = NEW.user_key;

        -- 3. Comments & Replies Soft Delete
        UPDATE comments
        SET deleted_at = NEW.deleted_at
        WHERE user_key = NEW.user_key
          AND deleted_at IS NULL;

        UPDATE replies
        SET deleted_at = NEW.deleted_at
        WHERE user_key = NEW.user_key
          AND deleted_at IS NULL;

        -- 4. Notifications 완전 삭제
        DELETE FROM notifications
        WHERE user_key = NEW.user_key;

        -- 5. Edit History 완전 삭제
        DELETE FROM edit_history
        WHERE user_key = NEW.user_key;

        -- 6. User Team History -> MEMBER_WITHDRAWAL 이력 삽입 (Relations 삭제 전에 먼저!)
        INSERT INTO user_team_history (team_key, actor_key, target_key, email, action_type, reason, created_at, updated_at)
        SELECT
            utr.team_key,
            NEW.user_key,
            NEW.user_key,
            NEW.email,
            'MEMBER_WITHDRAWAL',
            'User account deleted',
            NEW.deleted_at,
            NEW.deleted_at
        FROM user_team_relations utr
        WHERE utr.user_key = NEW.user_key;

        -- 7. User Team Relations 완전 삭제
        DELETE FROM user_team_relations
        WHERE user_key = NEW.user_key;

        -- 8. Project Member History -> WITHDRAWAL 이력 삽입 (Relations 삭제 전에 먼저!)
        INSERT INTO project_member_history (project_key, actor_key, target_key, action_type, created_at)
        SELECT
            pmr.project_key,
            NEW.user_key,
            NEW.user_key,
            'WITHDRAWAL',
            NEW.deleted_at
        FROM project_member_relations pmr
        WHERE pmr.member_key = NEW.user_key;

        -- 9. Project Member Relations 완전 삭제
        DELETE FROM project_member_relations
        WHERE member_key = NEW.user_key;

    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_soft_delete_cascade_user_data
AFTER UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION soft_delete_cascade_user_data();

-- User 재가입 시 연관 데이터 복구 및 이력 기록
CREATE OR REPLACE FUNCTION restore_user_on_reactivation()
RETURNS TRIGGER AS $$
BEGIN
    -- deleted_at이 NULL로 변경된 경우 (재가입)
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        -- Comments & Replies 복구
        UPDATE comments
        SET deleted_at = NULL
        WHERE user_key = NEW.user_key
          AND deleted_at = OLD.deleted_at;

        UPDATE replies
        SET deleted_at = NULL
        WHERE user_key = NEW.user_key
          AND deleted_at = OLD.deleted_at;

        -- User Team History -> MEMBER_REJOIN 이력 삽입 및 멤버십 복구
        -- 재가입 전에 MEMBER_WITHDRAWAL 이력이 있는 팀들에 대해 REJOIN 기록
        INSERT INTO user_team_history (team_key, actor_key, target_key, email, action_type, reason, created_at, updated_at)
        SELECT DISTINCT
            uth.team_key,
            NEW.user_key,
            NEW.user_key,
            NEW.email,
            'MEMBER_REJOIN',
            'User account reactivated',
            now(),
            now()
        FROM user_team_history uth
        WHERE uth.target_key = NEW.user_key
          AND uth.action_type = 'MEMBER_WITHDRAWAL'
          AND uth.created_at >= OLD.deleted_at;

        -- User Team Relations 복구 (팀 멤버십 재추가)
        INSERT INTO user_team_relations (user_key, team_key, created_at, updated_at)
        SELECT DISTINCT
            NEW.user_key,
            uth.team_key,
            now(),
            now()
        FROM user_team_history uth
        WHERE uth.target_key = NEW.user_key
          AND uth.action_type = 'MEMBER_WITHDRAWAL'
          AND uth.created_at >= OLD.deleted_at
        ON CONFLICT (user_key, team_key) DO NOTHING;  -- 이미 존재하면 무시

        -- Project Member History -> REJOIN 이력 삽입 및 멤버십 복구
        -- 재가입 전에 WITHDRAWAL 이력이 있는 프로젝트들에 대해 REJOIN 기록
        INSERT INTO project_member_history (project_key, actor_key, target_key, action_type, created_at)
        SELECT DISTINCT
            pmh.project_key,
            NEW.user_key,
            NEW.user_key,
            'REJOIN',
            now()
        FROM project_member_history pmh
        WHERE pmh.target_key = NEW.user_key
          AND pmh.action_type = 'WITHDRAWAL'
          AND pmh.created_at >= OLD.deleted_at;

        -- Project Member Relations 복구 (프로젝트 멤버십 재추가)
        INSERT INTO project_member_relations (project_key, member_key, role, created_at, updated_at)
        SELECT DISTINCT
            pmh.project_key,
            NEW.user_key,
            'EDITOR'::project_member_role_type,  -- 기본 역할로 복구 (타입 캐스팅)
            now(),
            now()
        FROM project_member_history pmh
        WHERE pmh.target_key = NEW.user_key
          AND pmh.action_type = 'WITHDRAWAL'
          AND pmh.created_at >= OLD.deleted_at
        ON CONFLICT (project_key, member_key) DO NOTHING;  -- 이미 존재하면 무시

        -- 참고: user_social_accounts, notifications 등 완전 삭제된 데이터는 복구 불가
        -- 필요시 애플리케이션 레벨에서 재생성 필요
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_restore_user_on_reactivation
AFTER UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION restore_user_on_reactivation();

-- Team 복구 시 하위 Projects도 복구
CREATE OR REPLACE FUNCTION restore_team_children()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        -- Projects 복구 (같은 시간에 삭제된 것만)
        UPDATE projects
        SET deleted_at = NULL
        WHERE team_key = NEW.team_key
          AND deleted_at = OLD.deleted_at;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_restore_team_children
AFTER UPDATE ON teams
FOR EACH ROW
EXECUTE FUNCTION restore_team_children();

-- Project 복구 시 하위 ERD Tables, Versions, Data Models도 복구
CREATE OR REPLACE FUNCTION restore_project_children()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        -- ERD Tables 복구
        UPDATE erd_tables
        SET deleted_at = NULL
        WHERE project_key = NEW.project_key
          AND deleted_at = OLD.deleted_at;

        -- Versions 복구
        UPDATE versions
        SET deleted_at = NULL
        WHERE project_key = NEW.project_key
          AND deleted_at = OLD.deleted_at;

        -- Data Models 복구
        UPDATE data_models
        SET deleted_at = NULL
        WHERE project_key = NEW.project_key
          AND deleted_at = OLD.deleted_at;

        -- Snapshots 복구
        UPDATE snapshots
        SET deleted_at = NULL
        WHERE project_key = NEW.project_key
          AND deleted_at = OLD.deleted_at;

        -- ERD Relations 복구 (project_key로 직접 참조)
        UPDATE erd_relations
        SET deleted_at = NULL
        WHERE project_key = NEW.project_key
          AND deleted_at = OLD.deleted_at;

        -- Comments 복구 (table_key를 통해 간접 참조)
        UPDATE comments
        SET deleted_at = NULL
        WHERE table_key IN (
            SELECT table_key FROM erd_tables WHERE project_key = NEW.project_key
        ) AND deleted_at = OLD.deleted_at;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_restore_project_children
AFTER UPDATE ON projects
FOR EACH ROW
EXECUTE FUNCTION restore_project_children();

-- ERD Table 복구 시 하위 Columns와 Relations도 복구
CREATE OR REPLACE FUNCTION restore_table_children()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        -- Columns 복구
        UPDATE erd_columns
        SET deleted_at = NULL
        WHERE table_key = NEW.table_key
          AND deleted_at = OLD.deleted_at;

        -- Relations 복구 (from_table 또는 to_table)
        UPDATE erd_relations
        SET deleted_at = NULL
        WHERE (from_table_key = NEW.table_key OR to_table_key = NEW.table_key)
          AND deleted_at = OLD.deleted_at;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_restore_table_children
AFTER UPDATE ON erd_tables
FOR EACH ROW
EXECUTE FUNCTION restore_table_children();

-- Version 복구 시 하위 Mock Data도 복구
CREATE OR REPLACE FUNCTION restore_version_children()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        -- Mock Data 복구
        UPDATE mock_data
        SET deleted_at = NULL
        WHERE version_key = NEW.version_key
          AND deleted_at = OLD.deleted_at;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_restore_version_children
AFTER UPDATE ON versions
FOR EACH ROW
EXECUTE FUNCTION restore_version_children();

-- =====================================================
-- Last Activity Update Triggers
-- =====================================================

-- Project의 last_activity_at 자동 업데이트 함수
-- 주의: Project 자체의 생성/수정은 JPA Entity의 @PrePersist/@PreUpdate에서 처리됨
-- 이 함수는 하위 엔티티(ERD Tables, Columns 등) 변경 시에만 사용됨
CREATE OR REPLACE FUNCTION update_project_last_activity()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE projects
    SET last_activity_at = now()
    WHERE project_key = COALESCE(NEW.project_key, OLD.project_key);

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- ERD Tables 변경 시
CREATE TRIGGER trigger_update_project_activity_on_table_change
AFTER INSERT OR UPDATE OR DELETE ON erd_tables
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity();

-- ERD Columns 변경 시 (project_key는 erd_tables를 통해 찾음)
CREATE OR REPLACE FUNCTION update_project_last_activity_via_table()
RETURNS TRIGGER AS $$
DECLARE
    v_project_key BIGINT;
BEGIN
    SELECT project_key INTO v_project_key
    FROM erd_tables
    WHERE table_key = COALESCE(NEW.table_key, OLD.table_key);

    IF v_project_key IS NOT NULL THEN
        UPDATE projects
        SET last_activity_at = now()
        WHERE project_key = v_project_key;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_project_activity_on_column_change
AFTER INSERT OR UPDATE OR DELETE ON erd_columns
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity_via_table();

-- ERD Relations 변경 시
CREATE TRIGGER trigger_update_project_activity_on_relation_change
AFTER INSERT OR UPDATE OR DELETE ON erd_relations
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity();

-- Comments 변경 시 (table_key를 통해 project_key 찾음)
CREATE TRIGGER trigger_update_project_activity_on_comment_change
AFTER INSERT OR UPDATE OR DELETE ON comments
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity_via_table();

-- Replies 변경 시 (comment_key -> table_key -> project_key)
CREATE OR REPLACE FUNCTION update_project_last_activity_via_comment()
RETURNS TRIGGER AS $$
DECLARE
    v_project_key BIGINT;
BEGIN
    SELECT et.project_key INTO v_project_key
    FROM comments c
    JOIN erd_tables et ON c.table_key = et.table_key
    WHERE c.comment_key = COALESCE(NEW.comment_key, OLD.comment_key);

    IF v_project_key IS NOT NULL THEN
        UPDATE projects
        SET last_activity_at = now()
        WHERE project_key = v_project_key;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_project_activity_on_reply_change
AFTER INSERT OR UPDATE OR DELETE ON replies
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity_via_comment();

-- Versions 변경 시
CREATE TRIGGER trigger_update_project_activity_on_version_change
AFTER INSERT OR UPDATE OR DELETE ON versions
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity();

-- Data Models 변경 시
CREATE TRIGGER trigger_update_project_activity_on_datamodel_change
AFTER INSERT OR UPDATE OR DELETE ON data_models
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity();

-- Edit History 추가 시 (project_key를 직접 사용)
CREATE TRIGGER trigger_update_project_activity_on_edit_history
AFTER INSERT ON edit_history
FOR EACH ROW
EXECUTE FUNCTION update_project_last_activity();
