-- =====================================================
-- WARN: sql 원본이지만 프로젝트의 스키마는 flyway (db.migration)에서 관리됩니다.
-- 00_init.sql
-- PostgreSQL Extensions and Custom Types
-- =====================================================

-- Set timezone to UTC for consistency across all environments
SET TIMEZONE='UTC';
ALTER DATABASE yaldi SET TIMEZONE TO 'UTC';

-- Extensions
CREATE EXTENSION IF NOT EXISTS vector;

-- ENUM Types
CREATE TYPE design_verification_status_type AS ENUM ('QUEUED', 'RUNNING', 'SUCCESS', 'WARNING', 'FAILED', 'CANCELED');
CREATE TYPE relation_type AS ENUM ('OPTIONAL_ONE_TO_ONE', 'STRICT_ONE_TO_ONE', 'OPTIONAL_ONE_TO_MANY', 'STRICT_ONE_TO_MANY');
CREATE TYPE referential_action_type AS ENUM ('CASCADE', 'SET_NULL', 'SET_DEFAULT', 'RESTRICT', 'NO_ACTION');
CREATE TYPE project_member_role_type AS ENUM ('OWNER', 'EDITOR', 'ADMIN');
