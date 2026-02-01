TRUNCATE TABLE
    notifications,
    mock_data,
    async_jobs,
    data_model_erd_column_relations,
    data_models,
    edit_history,
    snapshots,
    versions,
    replies,
    comments,
    erd_relations,
    erd_columns,
    erd_tables,
    project_member_history,
    project_member_relations,
    projects,
    user_team_history,
    user_team_relations,
    teams,
    user_social_accounts,
    users
CASCADE;

-- =====================================================
-- 1. USERS (Root Entity)
-- =====================================================

INSERT INTO users (email, nickname, created_at, updated_at) VALUES
('kimjh@naver.com', '김지훈', now() - interval '30 days', now() - interval '30 days'),
('leesy@kakao.com', '이서연', now() - interval '29 days', now() - interval '29 days'),
('parkms@gmail.com', '박민수', now() - interval '28 days', now() - interval '28 days'),
('choiyj@naver.com', '최윤정', now() - interval '27 days', now() - interval '27 days'),
('jungdh@daum.net', '정다현', now() - interval '26 days', now() - interval '26 days'),
('kangth@kakao.com', '강태훈', now() - interval '25 days', now() - interval '25 days'),
('hansu@naver.com', '한수진', now() - interval '24 days', now() - interval '24 days'),
('ohjs@gmail.com', '오준석', now() - interval '23 days', now() - interval '23 days');

INSERT INTO users (email, nickname, created_at, updated_at) VALUES
('sjihyun0756@gmail.com', '지효니', now() - interval '30 days', now() - interval '30 days');

INSERT INTO users (email, nickname, created_at, updated_at) VALUES
    ('hjihyun0756@gmail.com', '지효니2', now() - interval '30 days', now() - interval '30 days');

-- =====================================================
-- 2. USER SOCIAL ACCOUNTS
-- =====================================================

INSERT INTO user_social_accounts (user_key, provider, oauth_user_id, created_at, updated_at) VALUES
(1, 'GOOGLE', 'google_kimjh_12345', now() - interval '30 days', now() - interval '30 days'),
(1, 'GITHUB', 'github_kimjh_67890', now() - interval '30 days', now() - interval '30 days'),
(2, 'GOOGLE', 'google_leesy_11111', now() - interval '29 days', now() - interval '29 days'),
(3, 'GITHUB', 'github_parkms_22222', now() - interval '28 days', now() - interval '28 days'),
(4, 'SSAFY', 'ssafy_choiyj_33333', now() - interval '27 days', now() - interval '27 days'),
(5, 'GOOGLE', 'google_jungdh_44444', now() - interval '26 days', now() - interval '26 days'),
(6, 'GITHUB', 'github_kangth_55555', now() - interval '25 days', now() - interval '25 days'),
(7, 'GOOGLE', 'google_hansu_66666', now() - interval '24 days', now() - interval '24 days'),
(8, 'SSAFY', 'ssafy_ohjs_77777', now() - interval '23 days', now() - interval '23 days');

-- =====================================================
-- 3. TEAMS
-- =====================================================

INSERT INTO teams (owned_by, name, created_at, updated_at) VALUES
(1, '알파팀', now() - interval '25 days', now() - interval '25 days'),
(2, '베타개발팀', now() - interval '24 days', now() - interval '24 days'),
(3, '감마스쿼드', now() - interval '23 days', now() - interval '23 days'),
(5, '델타포스', now() - interval '22 days', now() - interval '22 days');

-- =====================================================
-- 4. USER-TEAM RELATIONS
-- =====================================================

INSERT INTO user_team_relations (user_key, team_key, created_at, updated_at) VALUES
-- 알파팀 멤버
(1, 1, now() - interval '25 days', now() - interval '25 days'), -- 김지훈 (팀장)
(2, 1, now() - interval '24 days', now() - interval '24 days'), -- 이서연
(3, 1, now() - interval '23 days', now() - interval '23 days'), -- 박민수
-- 베타개발팀 멤버
(2, 2, now() - interval '24 days', now() - interval '24 days'), -- 이서연 (팀장)
(4, 2, now() - interval '22 days', now() - interval '22 days'), -- 최윤정
(5, 2, now() - interval '21 days', now() - interval '21 days'), -- 정다현
-- 감마스쿼드 멤버
(3, 3, now() - interval '23 days', now() - interval '23 days'), -- 박민수 (팀장)
(6, 3, now() - interval '20 days', now() - interval '20 days'), -- 강태훈
-- 델타포스 멤버
(5, 4, now() - interval '22 days', now() - interval '22 days'), -- 정다현 (팀장)
(7, 4, now() - interval '19 days', now() - interval '19 days'), -- 한수진
(8, 4, now() - interval '18 days', now() - interval '18 days'); -- 오준석

-- =====================================================
-- 5. USER TEAM HISTORY
-- =====================================================

INSERT INTO user_team_history (team_key, actor_key, target_key, email, action_type, reason, created_at, updated_at) VALUES
-- 알파팀 이력
(1, 1, 2, 'leesy@kakao.com', 'INVITE_SENT', '팀원 초대', now() - interval '24 days', now() - interval '24 days'),
(1, 2, 2, 'leesy@kakao.com', 'INVITE_ACCEPTED', '초대 수락', now() - interval '24 days', now() - interval '24 days'),
(1, 1, 3, 'parkms@gmail.com', 'INVITE_SENT', '팀원 초대', now() - interval '23 days', now() - interval '23 days'),
(1, 3, 3, 'parkms@gmail.com', 'INVITE_ACCEPTED', '초대 수락', now() - interval '23 days', now() - interval '23 days'),
-- 베타개발팀 이력
(2, 2, 4, 'choiyj@naver.com', 'INVITE_SENT', '팀원 초대', now() - interval '22 days', now() - interval '22 days'),
(2, 4, 4, 'choiyj@naver.com', 'INVITE_ACCEPTED', '초대 수락', now() - interval '22 days', now() - interval '22 days');

-- =====================================================
-- 6. PROJECTS
-- =====================================================

INSERT INTO projects (team_key, name, description, image_url, last_activity_at, created_at, updated_at) VALUES
-- 알파팀 프로젝트
(1, '이커머스 플랫폼', '쿠팡/11번가 스타일의 종합 쇼핑몰 플랫폼', 'https://example.com/images/ecommerce.png', now() - interval '2 days', now() - interval '20 days', now() - interval '2 days'),
(1, '배달앱 백엔드', '배달의민족 스타일 배달 주문 시스템', 'https://example.com/images/delivery-api.png', now() - interval '5 days', now() - interval '18 days', now() - interval '5 days'),
-- 베타개발팀 프로젝트
(2, '실시간 분석 대시보드', '네이버 애널리틱스 스타일 실시간 데이터 분석', 'https://example.com/images/analytics.png', now() - interval '1 day', now() - interval '17 days', now() - interval '1 day'),
(2, '재고관리 시스템', '물류센터 재고 관리 및 입출고 시스템', NULL, now() - interval '3 days', now() - interval '15 days', now() - interval '3 days'),
-- 감마스쿼드 프로젝트
(3, 'SNS 플랫폼', '인스타그램 스타일 소셜 미디어 플랫폼', 'https://example.com/images/social.png', now() - interval '4 days', now() - interval '16 days', now() - interval '4 days'),
-- 델타포스 프로젝트
(4, '예약 시스템', '호텔/항공 통합 예약 플랫폼', 'https://example.com/images/booking.png', now() - interval '1 day', now() - interval '14 days', now() - interval '1 day');

-- =====================================================
-- 7. PROJECT MEMBER RELATIONS
-- =====================================================

INSERT INTO project_member_relations (project_key, member_key, role, created_at, updated_at) VALUES
-- 이커머스 플랫폼
(1, 1, 'OWNER', now() - interval '20 days', now() - interval '20 days'),
(1, 2, 'EDITOR', now() - interval '19 days', now() - interval '19 days'),
(1, 3, 'EDITOR', now() - interval '19 days', now() - interval '19 days'),
-- 배달앱 백엔드
(2, 1, 'OWNER', now() - interval '18 days', now() - interval '18 days'),
(2, 2, 'ADMIN', now() - interval '17 days', now() - interval '17 days'),
-- 실시간 분석 대시보드
(3, 2, 'OWNER', now() - interval '17 days', now() - interval '17 days'),
(3, 4, 'EDITOR', now() - interval '16 days', now() - interval '16 days'),
(3, 5, 'EDITOR', now() - interval '16 days', now() - interval '16 days'),
-- 재고관리 시스템
(4, 2, 'OWNER', now() - interval '15 days', now() - interval '15 days'),
(4, 4, 'ADMIN', now() - interval '14 days', now() - interval '14 days'),
-- SNS 플랫폼
(5, 3, 'OWNER', now() - interval '16 days', now() - interval '16 days'),
(5, 6, 'EDITOR', now() - interval '15 days', now() - interval '15 days'),
-- 예약 시스템
(6, 5, 'OWNER', now() - interval '14 days', now() - interval '14 days'),
(6, 7, 'EDITOR', now() - interval '13 days', now() - interval '13 days'),
(6, 8, 'EDITOR', now() - interval '13 days', now() - interval '13 days');

-- =====================================================
-- 8. PROJECT MEMBER HISTORY
-- =====================================================

INSERT INTO project_member_history (project_key, actor_key, target_key, action_type, created_at) VALUES
-- 이커머스 플랫폼 이력
(1, 1, 1, 'ADD', now() - interval '20 days'),
(1, 1, 2, 'ADD', now() - interval '19 days'),
(1, 1, 3, 'ADD', now() - interval '19 days'),
-- 실시간 분석 대시보드 이력
(3, 2, 2, 'ADD', now() - interval '17 days'),
(3, 2, 4, 'ADD', now() - interval '16 days'),
(3, 2, 5, 'ADD', now() - interval '16 days'),
-- SNS 플랫폼 이력
(5, 3, 3, 'ADD', now() - interval '16 days'),
(5, 3, 6, 'ADD', now() - interval '15 days');

-- =====================================================
-- 9. TABLES (ERD Tables)
-- =====================================================

INSERT INTO erd_tables (project_key, logical_name, physical_name, x_position, y_position, color_hex, created_at, updated_at) VALUES
-- 이커머스 플랫폼 테이블
(1, '회원', 'members', 100.00, 100.00, 'FF6B6B', now() - interval '19 days', now() - interval '19 days'),
(1, '주문', 'orders', 400.00, 100.00, '4ECDC4', now() - interval '19 days', now() - interval '19 days'),
(1, '상품', 'products', 700.00, 100.00, 'FFE66D', now() - interval '19 days', now() - interval '19 days'),
(1, '주문상세', 'order_items', 550.00, 300.00, '95E1D3', now() - interval '18 days', now() - interval '18 days'),
(1, '결제', 'payments', 400.00, 450.00, 'FFA07A', now() - interval '18 days', now() - interval '2 days'),
-- 실시간 분석 대시보드 테이블
(3, '이벤트', 'events', 150.00, 150.00, '667EEA', now() - interval '16 days', now() - interval '1 day'),
(3, '세션', 'sessions', 450.00, 150.00, '764BA2', now() - interval '16 days', now() - interval '1 day'),
(3, '사용자', 'users', 150.00, 350.00, 'F093FB', now() - interval '16 days', now() - interval '1 day'),
-- SNS 플랫폼 테이블
(5, '사용자', 'users', 200.00, 100.00, '6BCF7F', now() - interval '15 days', now() - interval '4 days'),
(5, '게시글', 'posts', 500.00, 100.00, '4D96FF', now() - interval '15 days', now() - interval '4 days'),
(5, '댓글', 'comments', 500.00, 300.00, 'FFB84D', now() - interval '15 days', now() - interval '4 days'),
(5, '팔로우', 'follows', 200.00, 300.00, 'FF6B9D', now() - interval '14 days', now() - interval '4 days');

-- =====================================================
-- 10. COLUMNS
-- =====================================================

INSERT INTO erd_columns (table_key, logical_name, physical_name, data_type, data_detail, is_nullable, is_primary_key, is_foreign_key, is_unique, is_incremental, default_value, comment, created_at, updated_at) VALUES
-- members 테이블 (table_key=1) 컬럼
(1, '회원ID', 'member_id', 'BIGINT', NULL, false, true, false, true, true, NULL, '회원 고유 식별자', now() - interval '19 days', now() - interval '19 days'),
(1, '이메일', 'email', 'VARCHAR', ARRAY['255'], false, false, false, true, false, NULL, '회원 이메일 주소', now() - interval '19 days', now() - interval '19 days'),
(1, '이름', 'name', 'VARCHAR', ARRAY['50'], false, false, false, false, false, NULL, '회원 이름', now() - interval '19 days', now() - interval '19 days'),
(1, '휴대폰번호', 'phone', 'VARCHAR', ARRAY['20'], true, false, false, false, false, NULL, '휴대폰 번호 (010-xxxx-xxxx)', now() - interval '19 days', now() - interval '19 days'),
(1, '가입일시', 'created_at', 'TIMESTAMP', NULL, false, false, false, false, false, 'CURRENT_TIMESTAMP', '가입 일시', now() - interval '19 days', now() - interval '19 days'),
-- orders 테이블 (table_key=2) 컬럼
(2, '주문ID', 'order_id', 'BIGINT', NULL, false, true, false, true, true, NULL, '주문 고유 식별자', now() - interval '19 days', now() - interval '19 days'),
(2, '회원ID', 'member_id', 'BIGINT', NULL, false, false, true, false, false, NULL, '주문한 회원 ID', now() - interval '19 days', now() - interval '19 days'),
(2, '주문일시', 'order_date', 'TIMESTAMP', NULL, false, false, false, false, false, 'CURRENT_TIMESTAMP', '주문 일시', now() - interval '19 days', now() - interval '19 days'),
(2, '총금액', 'total_amount', 'DECIMAL', ARRAY['12', '0'], false, false, false, false, false, '0', '주문 총 금액 (원)', now() - interval '19 days', now() - interval '19 days'),
(2, '주문상태', 'status', 'VARCHAR', ARRAY['20'], false, false, false, false, false, '주문접수', '주문 상태 (주문접수/배송중/배송완료/취소)', now() - interval '19 days', now() - interval '19 days'),
-- products 테이블 (table_key=3) 컬럼
(3, '상품ID', 'product_id', 'BIGINT', NULL, false, true, false, true, true, NULL, '상품 고유 식별자', now() - interval '19 days', now() - interval '19 days'),
(3, '상품명', 'product_name', 'VARCHAR', ARRAY['200'], false, false, false, false, false, NULL, '상품 이름', now() - interval '19 days', now() - interval '19 days'),
(3, '판매가격', 'price', 'DECIMAL', ARRAY['12', '0'], false, false, false, false, false, '0', '판매 가격 (원)', now() - interval '19 days', now() - interval '19 days'),
(3, '재고수량', 'stock', 'INTEGER', NULL, false, false, false, false, false, '0', '현재 재고 수량', now() - interval '19 days', now() - interval '19 days'),
(3, '카테고리', 'category', 'VARCHAR', ARRAY['50'], true, false, false, false, false, NULL, '상품 카테고리', now() - interval '19 days', now() - interval '19 days'),
(3, '할인율', 'discount_rate', 'INTEGER', NULL, true, false, false, false, false, '0', '할인율 (삭제된 컬럼)', now() - interval '19 days', now() - interval '19 days'),
-- order_items 테이블 (table_key=4) 컬럼
(4, '주문상세ID', 'order_item_id', 'BIGINT', NULL, false, true, false, true, true, NULL, '주문 상세 고유 식별자', now() - interval '18 days', now() - interval '18 days'),
(4, '주문ID', 'order_id', 'BIGINT', NULL, false, false, true, false, false, NULL, '주문 ID', now() - interval '18 days', now() - interval '18 days'),
(4, '상품ID', 'product_id', 'BIGINT', NULL, false, false, true, false, false, NULL, '상품 ID', now() - interval '18 days', now() - interval '18 days'),
(4, '수량', 'quantity', 'INTEGER', NULL, false, false, false, false, false, '1', '주문 수량', now() - interval '18 days', now() - interval '18 days'),
(4, '단가', 'unit_price', 'DECIMAL', ARRAY['12', '0'], false, false, false, false, false, '0', '주문 당시 단가 (원)', now() - interval '18 days', now() - interval '18 days');

-- =====================================================
-- 11. RELATIONS
-- =====================================================

INSERT INTO erd_relations (project_key, from_table_key, to_table_key, relation_type, constraint_name, on_delete_action, on_update_action, created_at, updated_at) VALUES
-- 이커머스 플랫폼 관계
(1, 2, 1, 'STRICT_ONE_TO_MANY', 'fk_orders_member_id', 'CASCADE', 'CASCADE', now() - interval '19 days', now() - interval '2 days'),
(1, 4, 2, 'STRICT_ONE_TO_MANY', 'fk_order_items_order_id', 'CASCADE', 'CASCADE', now() - interval '18 days', now() - interval '2 days'),
(1, 4, 3, 'STRICT_ONE_TO_MANY', 'fk_order_items_product_id', 'RESTRICT', 'CASCADE', now() - interval '18 days', now() - interval '2 days'),
(1, 5, 2, 'STRICT_ONE_TO_ONE', 'fk_payments_order_id', 'CASCADE', 'CASCADE', now() - interval '18 days', now() - interval '2 days'),
-- SNS 플랫폼 관계
(5, 10, 9, 'STRICT_ONE_TO_MANY', 'fk_posts_user_id', 'CASCADE', 'CASCADE', now() - interval '15 days', now() - interval '4 days'),
(5, 11, 10, 'STRICT_ONE_TO_MANY', 'fk_comments_post_id', 'CASCADE', 'CASCADE', now() - interval '15 days', now() - interval '4 days'),
(5, 11, 9, 'STRICT_ONE_TO_MANY', 'fk_comments_user_id', 'CASCADE', 'CASCADE', now() - interval '15 days', now() - interval '4 days'),
(5, 12, 9, 'OPTIONAL_ONE_TO_MANY', 'fk_follows_user_id', 'CASCADE', 'CASCADE', now() - interval '14 days', now() - interval '4 days');

-- =====================================================
-- 12. COMMENTS
-- =====================================================

INSERT INTO comments (user_key, table_key, project_key, content, color_hex, x_position, y_position, is_resolved, created_at, updated_at) VALUES
(1, 1, 1, '이메일 필드에 인덱스 추가하면 조회 속도가 빨라질 것 같아요', 'FF5733', 120.50, 80.25, false, now() - interval '10 days', now() - interval '10 days'),
(2, 2, 1, '주문 상태에 ENUM 타입 사용하는게 어떨까요? 오타 방지에 좋을 듯', '33B5FF', 220.30, 140.60, true, now() - interval '9 days', now() - interval '5 days'),
(3, 3, 1, '판매가격 필드에 CHECK 제약조건으로 0원 이상만 허용하도록 하면 좋겠습니다', '8D33FF', 310.80, 200.45, false, now() - interval '8 days', now() - interval '8 days'),
(1, 4, 1, '주문상세 테이블에 (order_id, product_id) 복합 인덱스 필요할 것 같습니다', '33FF57', 410.90, 250.70, false, now() - interval '7 days', now() - interval '7 days'),
(5, 9, 1, '사용자 테이블에 프로필 이미지 URL 필드 추가하면 좋겠어요', 'FFC300', 520.40, 320.80, false, now() - interval '6 days', now() - interval '6 days');

-- =====================================================
-- 13. REPLIES
-- =====================================================

INSERT INTO replies (comment_key, user_key, content, created_at, updated_at) VALUES
(1, 2, '좋은 의견이네요! 바로 인덱스 추가하겠습니다', now() - interval '10 days', now() - interval '10 days'),
(1, 1, '감사합니다!', now() - interval '9 days', now() - interval '9 days'),
(2, 1, '동의합니다. status 컬럼을 ENUM으로 변경했어요', now() - interval '8 days', now() - interval '8 days'),
(2, 2, '확인했습니다. 감사합니다!', now() - interval '5 days', now() - interval '5 days'),
(3, 6, 'CHECK (price >= 0) 제약조건 추가 완료했습니다', now() - interval '7 days', now() - interval '7 days'),
(5, 3, 'profile_image_url 컬럼 추가했습니다', now() - interval '5 days', now() - interval '5 days');

-- =====================================================
-- 14. VERSIONS
-- =====================================================

INSERT INTO versions (project_key, job_id, name, schema_data, description, design_verification_status, verification_result, is_public, vector, created_at, updated_at) VALUES
(1, NULL, 'v1.0.0', '{"tables": [], "relations": []}', '이커머스 초기 스키마 설계', 'SUCCESS', NULL, true, NULL, now() - interval '19 days', now() - interval '19 days'),
(1, NULL, 'v1.1.0', '{"tables": [], "relations": []}', '결제 테이블 및 PG연동 추가', 'SUCCESS', NULL, true, NULL, now() - interval '18 days', now() - interval '18 days'),
(1, NULL, 'v1.2.0', '{"tables": [], "relations": []}', '주문 상태 관리 개선', 'RUNNING', NULL, false, NULL, now() - interval '2 days', now() - interval '1 day'),
(3, NULL, 'v1.0.0', '{"tables": [], "relations": []}', '실시간 분석 초기 버전', 'SUCCESS', NULL, true, NULL, now() - interval '16 days', now() - interval '16 days'),
(3, NULL, 'v2.0.0', '{"tables": [], "relations": []}', '대용량 데이터 처리 로직 추가', 'WARNING', '{"errors": [], "warnings": ["대용량 테이블에 인덱스가 필요합니다"], "message": "스키마 검증 완료, 경고 사항 있음", "suggestions": ["events 테이블에 created_at 인덱스 추가 권장"]}', false, NULL, now() - interval '5 days', now() - interval '1 day'),
(5, NULL, 'v1.0.0', '{"tables": [], "relations": []}', 'SNS 플랫폼 초기 설계', 'SUCCESS', NULL, true, NULL, now() - interval '15 days', now() - interval '15 days'),
(5, NULL, 'v1.1.0', '{"tables": [], "relations": []}', '팔로우/팔로워 기능 추가', 'QUEUED', NULL, false, NULL, now() - interval '4 days', now() - interval '4 days');

-- =====================================================
-- 15. SNAPSHOTS
-- =====================================================

INSERT INTO snapshots (project_key, created_by, name, schema_data, created_at, updated_at) VALUES
(1, 1, '결제모듈 통합 전', '{"tables": [{"name": "members"}, {"name": "orders"}, {"name": "products"}], "relations": []}', now() - interval '18 days', now() - interval '18 days'),
(1, 1, '결제모듈 통합 후', '{"tables": [{"name": "members"}, {"name": "orders"}, {"name": "products"}, {"name": "payments"}], "relations": []}', now() - interval '17 days', now() - interval '17 days'),
(3, 2, '초기 설계안', '{"tables": [{"name": "events"}, {"name": "sessions"}], "relations": []}', now() - interval '16 days', now() - interval '16 days'),
(5, 3, '기본 소셜 기능', '{"tables": [{"name": "users"}, {"name": "posts"}, {"name": "comments"}], "relations": []}', now() - interval '15 days', now() - interval '15 days'),
(5, 3, '팔로우 시스템 추가', '{"tables": [{"name": "users"}, {"name": "posts"}, {"name": "comments"}, {"name": "follows"}], "relations": []}', now() - interval '14 days', now() - interval '14 days');

-- =====================================================
-- 16. EDIT HISTORY
-- =====================================================

INSERT INTO edit_history (user_key, project_key, target_key, target_type, action_type, delta, created_at, updated_at, before_state, after_state) VALUES
(1, 1, 1, 'TABLE', 'ADD', '{"logical_name": "회원", "physical_name": "members"}', now() - interval '19 days', now() - interval '19 days', NULL, '{"logical_name": "회원", "physical_name": "members"}'),
(1, 1, 2, 'TABLE', 'ADD', '{"logical_name": "주문", "physical_name": "orders"}', now() - interval '19 days', now() - interval '19 days', NULL, '{"logical_name": "주문", "physical_name": "orders"}'),
(1, 1, 5, 'TABLE', 'ADD', '{"logical_name": "결제", "physical_name": "payments"}', now() - interval '18 days', now() - interval '18 days', NULL, '{"logical_name": "결제", "physical_name": "payments"}'),
(2, 1, 2, 'TABLE', 'UPDATE', '{"status_field": "enum"}', now() - interval '2 days', now() - interval '2 days', '{"status": "varchar"}', '{"status": "enum"}'),
(3, 5, 10, 'TABLE', 'ADD', '{"logical_name": "게시글", "physical_name": "posts"}', now() - interval '15 days', now() - interval '15 days', NULL, '{"logical_name": "게시글", "physical_name": "posts"}'),
(3, 5, 12, 'TABLE', 'ADD', '{"logical_name": "팔로우", "physical_name": "follows"}', now() - interval '14 days', now() - interval '14 days', NULL, '{"logical_name": "팔로우", "physical_name": "follows"}');

-- =====================================================
-- 17. DATA MODELS
-- =====================================================

INSERT INTO data_models (project_key, name, type, source_table_key, last_synced_at, created_at, updated_at) VALUES
-- 프로젝트 1 (이커머스 플랫폼)
(1, 'MemberEntity', 'ENTITY', 1, now() - interval '18 days', now() - interval '18 days', now() - interval '18 days'),
(1, 'OrderEntity', 'ENTITY', 2, now() - interval '18 days', now() - interval '18 days', now() - interval '18 days'),
(1, 'CreateOrderRequest', 'DTO_REQUEST', NULL, now() - interval '17 days', now() - interval '17 days', now() - interval '17 days'),
(1, 'OrderResponse', 'DTO_RESPONSE', NULL, now() - interval '17 days', now() - interval '17 days', now() - interval '17 days'),
(1, 'ProductDto', 'DTO_RESPONSE', 3, now() - interval '16 days', now() - interval '16 days', now() - interval '16 days'),
-- 프로젝트 3 (실시간 분석 대시보드)
(3, 'EventEntity', 'ENTITY', 6, now() - interval '16 days', now() - interval '16 days', now() - interval '16 days'),
-- 프로젝트 5 (SNS 플랫폼)
(5, 'PostEntity', 'ENTITY', 10, now() - interval '15 days', now() - interval '15 days', now() - interval '15 days'),
(5, 'CreatePostRequest', 'DTO_REQUEST', NULL, now() - interval '14 days', now() - interval '14 days', now() - interval '14 days');

-- =====================================================
-- 18. MODEL COLUMN RELATIONS
-- =====================================================

INSERT INTO data_model_erd_column_relations (column_key, model_key, created_at, updated_at) VALUES
-- MemberEntity 관계 (model_key=1)
(1, 1, now() - interval '18 days', now() - interval '18 days'),
(2, 1, now() - interval '18 days', now() - interval '18 days'),
(3, 1, now() - interval '18 days', now() - interval '18 days'),
(5, 1, now() - interval '18 days', now() - interval '18 days'),
-- OrderEntity 관계 (model_key=2)
(6, 2, now() - interval '18 days', now() - interval '18 days'),
(7, 2, now() - interval '18 days', now() - interval '18 days'),
(8, 2, now() - interval '18 days', now() - interval '18 days'),
(9, 2, now() - interval '18 days', now() - interval '18 days'),
(10, 2, now() - interval '18 days', now() - interval '18 days'),
-- ProductDto 관계 (model_key=5) - 삭제된 컬럼 포함 (INVALID 테스트용)
(11, 5, now() - interval '16 days', now() - interval '16 days'),
(12, 5, now() - interval '16 days', now() - interval '16 days'),
(16, 5, now() - interval '16 days', now() - interval '16 days');

-- INVALID 테스트용: ProductDto의 discount_rate 컬럼을 삭제 처리
UPDATE erd_columns SET deleted_at = now() - interval '15 days' WHERE column_key = 16;

-- =====================================================
-- 19. ASYNC JOBS (Kafka 비동기 작업)
-- =====================================================

INSERT INTO async_jobs (job_id, job_type, user_key, reference_key, status, error_message, completed_at, created_at) VALUES
('01ARZ3NDEKTSV4RRFFQ69G5FAV', 'MOCK_DATA', 1, 1, 'COMPLETED', NULL, now() - interval '17 days', now() - interval '17 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAW', 'MOCK_DATA', 1, 1, 'COMPLETED', NULL, now() - interval '17 days', now() - interval '17 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAX', 'MOCK_DATA', 1, 1, 'COMPLETED', NULL, now() - interval '17 days', now() - interval '17 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAY', 'MOCK_DATA', 1, 2, 'COMPLETED', NULL, now() - interval '16 days', now() - interval '16 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAZ', 'MOCK_DATA', 2, 4, 'COMPLETED', NULL, now() - interval '15 days', now() - interval '15 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FB0', 'MOCK_DATA', 3, 6, 'COMPLETED', NULL, now() - interval '14 days', now() - interval '14 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FB1', 'MOCK_DATA', 3, 6, 'COMPLETED', NULL, now() - interval '13 days', now() - interval '13 days');

-- =====================================================
-- 20. MOCK DATA
-- =====================================================

INSERT INTO mock_data (job_id, version_key, file_name, file_path, row_counts, created_at, updated_at) VALUES
('01ARZ3NDEKTSV4RRFFQ69G5FAV', 1, 'mock_data_v1_0_0_1699012345678.sql', 'https://yaldi-bucket.s3.ap-northeast-2.amazonaws.com/mock-data/mock_data_v1_0_0_1699012345678.sql', 100, now() - interval '17 days', now() - interval '17 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAW', 1, 'mock_data_v1_0_0_1699012345679.sql', 'https://yaldi-bucket.s3.ap-northeast-2.amazonaws.com/mock-data/mock_data_v1_0_0_1699012345679.sql', 250, now() - interval '17 days', now() - interval '17 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAX', 1, 'mock_data_v1_0_0_1699012345680.sql', 'https://yaldi-bucket.s3.ap-northeast-2.amazonaws.com/mock-data/mock_data_v1_0_0_1699012345680.sql', 50, now() - interval '17 days', now() - interval '17 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAY', 2, 'mock_data_v1_1_0_1699012345681.sql', 'https://yaldi-bucket.s3.ap-northeast-2.amazonaws.com/mock-data/mock_data_v1_1_0_1699012345681.sql', 200, now() - interval '16 days', now() - interval '16 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FAZ', 4, 'mock_data_v1_0_0_1699012345682.sql', 'https://yaldi-bucket.s3.ap-northeast-2.amazonaws.com/mock-data/mock_data_v1_0_0_1699012345682.sql', 1000, now() - interval '15 days', now() - interval '15 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FB0', 6, 'mock_data_v1_0_0_1699012345683.sql', 'https://yaldi-bucket.s3.ap-northeast-2.amazonaws.com/mock-data/mock_data_v1_0_0_1699012345683.sql', 500, now() - interval '14 days', now() - interval '14 days'),
('01ARZ3NDEKTSV4RRFFQ69G5FB1', 6, 'mock_data_v1_1_0_1699012345684.sql', 'https://yaldi-bucket.s3.ap-northeast-2.amazonaws.com/mock-data/mock_data_v1_1_0_1699012345684.sql', 300, now() - interval '13 days', now() - interval '13 days');

-- =====================================================
-- 20. NOTIFICATIONS
-- =====================================================

INSERT INTO notifications (user_key, type, content, read_at, created_at) VALUES
(1, 'ADDED_TO_PROJECT', '이커머스 플랫폼 프로젝트에 추가되었습니다.', now() - interval '10 days', now() - interval '10 days'),
(2, 'REMOVED_FROM_PROJECT', '이커머스 플랫폼 프로젝트에서 제거되었습니다.', NULL, now() - interval '9 days'),
(3, 'ADDED_TO_TEAM', '베타개발팀 팀에 추가되었습니다.', now() - interval '8 days', now() - interval '8 days'),
(4, 'REMOVED_FROM_TEAM', '알파팀 팀에서 제거되었습니다.', NULL, now() - interval '7 days'),
(5, 'BE_PROJECT_OWNER', '당신이 배달앱 백엔드 프로젝트의 소유자가 되었습니다.', now() - interval '6 days', now() - interval '6 days'),
(6, 'BE_TEAM_OWNER', '당신이 알파팀 팀의 소유자가 되었습니다.', NULL, now() - interval '5 days'),
(7, 'NEW_VERSION', '실시간 분석 대시보드 프로젝트에 새로운 버전이 릴리즈되었습니다.', now() - interval '4 days', now() - interval '4 days');


INSERT INTO versions (
    project_key,
    job_id,
    name,
    schema_data,
    description,
    design_verification_status,
    verification_result,
    is_public,
    vector,
    created_at,
    updated_at
) VALUES (
             1,
             NULL,
             'v1.0.0-test',
             '{
               "tables": [
                 {
                   "tableKey": 1,
                   "logicalName": "사용자",
                   "physicalName": "users",
                   "columns": [
                     {
                       "columnKey": 1,
                       "logicalName": "사용자ID",
                       "physicalName": "user_id",
                       "dataType": "INT",
                       "dataDetail": [],
                       "isPrimaryKey": true,
                       "isForeignKey": false,
                       "isUnique": true,
                       "isNullable": false,
                       "isIncremental": true,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 2,
                       "logicalName": "이메일",
                       "physicalName": "email",
                       "dataType": "VARCHAR",
                       "dataDetail": ["100"],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": true,
                       "isNullable": false,
                       "isIncremental": false,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 3,
                       "logicalName": "이름",
                       "physicalName": "name",
                       "dataType": "VARCHAR",
                       "dataDetail": ["50"],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": false,
                       "isNullable": false,
                       "isIncremental": false,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 4,
                       "logicalName": "나이",
                       "physicalName": "age",
                       "dataType": "INT",
                       "dataDetail": [],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": false,
                       "isNullable": true,
                       "isIncremental": false,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 5,
                       "logicalName": "상태",
                       "physicalName": "status",
                       "dataType": "ENUM",
                       "dataDetail": ["active", "inactive",
         "pending"],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": false,
                       "isNullable": false,
                       "isIncremental": false,
                       "defaultValue": "active"
                     },
                     {
                       "columnKey": 6,
                       "logicalName": "생성일시",
                       "physicalName": "created_at",
                       "dataType": "TIMESTAMP",
                       "dataDetail": [],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": false,
                       "isNullable": false,
                       "isIncremental": false,
                       "defaultValue": "CURRENT_TIMESTAMP"
                     }
                   ]
                 },
                 {
                   "tableKey": 2,
                   "logicalName": "게시글",
                   "physicalName": "posts",
                   "columns": [
                     {
                       "columnKey": 7,
                       "logicalName": "게시글ID",
                       "physicalName": "post_id",
                       "dataType": "INT",
                       "dataDetail": [],
                       "isPrimaryKey": true,
                       "isForeignKey": false,
                       "isUnique": true,
                       "isNullable": false,
                       "isIncremental": true,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 8,
                       "logicalName": "작성자ID",
                       "physicalName": "user_id",
                       "dataType": "INT",
                       "dataDetail": [],
                       "isPrimaryKey": false,
                       "isForeignKey": true,
                       "isUnique": false,
                       "isNullable": false,
                       "isIncremental": false,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 9,
                       "logicalName": "제목",
                       "physicalName": "title",
                       "dataType": "VARCHAR",
                       "dataDetail": ["200"],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": false,
                       "isNullable": false,
                       "isIncremental": false,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 10,
                       "logicalName": "내용",
                       "physicalName": "content",
                       "dataType": "TEXT",
                       "dataDetail": [],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": false,
                       "isNullable": true,
                       "isIncremental": false,
                       "defaultValue": null
                     },
                     {
                       "columnKey": 11,
                       "logicalName": "조회수",
                       "physicalName": "view_count",
                       "dataType": "INT",
                       "dataDetail": [],
                       "isPrimaryKey": false,
                       "isForeignKey": false,
                       "isUnique": false,
                       "isNullable": false,
                       "isIncremental": false,
                       "defaultValue": "0"
                     }
                   ]
                 }
               ],
               "relations": [
                 {
                   "fromTableKey": 2,
                   "toTableKey": 1,
                   "relationType": "N:1",
                   "constraintName": "fk_posts_user",
                   "onDeleteAction": "CASCADE",
                   "onUpdateAction": "CASCADE"
                 }
               ]
             }'::jsonb,
             '블로그 시스템 테스트 스키마',
             'SUCCESS',
             NULL,
             false,
             NULL,
             NOW(),
             NOW()
         );


-- =====================================================
-- 추가 프로젝트 (검색 테스트용)
-- =====================================================

INSERT INTO projects (team_key, name, description, image_url, created_at, updated_at) VALUES
-- 헬스케어
(1, '병원 예약 시스템', '환자 진료 예약 및 의료 기록 관리 시스템. EMR 연동 지원', 'https://example.com/images/hospital.png', now() - interval '12 days', now() - interval '1 day'),
-- 금융
(2, '주식 거래 플랫폼', '실시간 주식 매매 및 계좌 관리 시스템. 토스증권 스타일', 'https://example.com/images/stock.png', now() - interval '11 days', now() - interval '2 days'),
(2, '카드 결제 시스템', '신용카드 PG사 연동 및 간편결제 시스템. 페이팔 스타일', 'https://example.com/images/payment.png', now() - interval '10 days', now() - interval '1 day'),
-- 교육
(3, '온라인 강의 플랫폼', 'MOOC 스타일 온라인 교육 플랫폼. 인프런/유데미 스타일', 'https://example.com/images/education.png', now() - interval '13 days', now() - interval '3 days'),
(3, '학사 관리 시스템', '대학교 수강신청 및 성적 관리 시스템', 'https://example.com/images/university.png', now() - interval '9 days', now() - interval '2 days'),
-- 게임
(4, '멀티플레이어 게임 서버', '실시간 PVP 게임 서버. WebSocket 기반 동기화', 'https://example.com/images/game.png', now() - interval '8 days', now() - interval '1 day'),
-- IoT
(1, '스마트홈 제어 시스템', 'IoT 기기 통합 관리 및 자동화 시스템', 'https://example.com/images/iot.png', now() - interval '7 days', now() - interval '1 day'),
-- 소셜/커뮤니티
(3, '커뮤니티 포럼', '디스코드 스타일 실시간 채팅 및 커뮤니티 플랫폼', 'https://example.com/images/forum.png', now() - interval '6 days', now() - interval '2 days');


select * from projects;