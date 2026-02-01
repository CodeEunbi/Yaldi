# ERD SQL Export 기능 가이드

## 개요

Yaldi ERD의 SQL Export 기능은 프로젝트의 ERD를 PostgreSQL 또는 MySQL DDL(Data Definition Language) 스크립트로 변환하여 내보내는 기능입니다.

## API 엔드포인트

### ERD SQL Export

```http
GET /api/v1/erd/projects/{projectKey}/export/sql?dialect={POSTGRESQL|MYSQL}
```

**파라미터:**
- `projectKey` (Path): 프로젝트 고유 키
- `dialect` (Query, Optional): SQL 방언 선택
  - `POSTGRESQL` (기본값)
  - `MYSQL`

**응답:**
```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": "-- PostgreSQL DDL Export\n-- Project Key: 1\n..."
}
```

## 지원 기능

### 1. CREATE TABLE 문 생성

- 테이블 정의 (물리적 테이블명 사용)
- 컬럼 정의 (데이터 타입, 제약조건)
- PRIMARY KEY 제약조건
- 테이블/컬럼 코멘트 (PostgreSQL: COMMENT ON, MySQL: COMMENT 절)

### 2. 컬럼 제약조건

- `NOT NULL`: nullable이 false인 경우
- `UNIQUE`: 고유 제약조건
- `PRIMARY KEY`: 복합 키 지원
- `DEFAULT`: 기본값 설정
- `AUTO_INCREMENT` (MySQL) / `SERIAL` (PostgreSQL): 자동 증가

### 3. 외래키 제약조건

- `FOREIGN KEY` 정의
- `ON DELETE` 액션 (CASCADE, SET NULL, SET DEFAULT, RESTRICT, NO ACTION)
- `ON UPDATE` 액션
- 제약조건 이름 자동 생성

## 데이터 타입 매핑

### PostgreSQL ↔ MySQL 타입 변환

| 개념 | PostgreSQL | MySQL | 비고 |
|------|-----------|-------|------|
| 작은 정수 | SMALLINT | TINYINT | MySQL → PostgreSQL 시 SMALLINT |
| 정수 | INTEGER | INT | |
| 큰 정수 | BIGINT | BIGINT | |
| 부동소수 | REAL | FLOAT | |
| 배정밀도 | DOUBLE PRECISION | DOUBLE | |
| 고정소수 | NUMERIC(p,s) | DECIMAL(p,s) | |
| 고정문자 | CHAR(n) | CHAR(n) | |
| 가변문자 | VARCHAR(n) | VARCHAR(n) | |
| 긴 텍스트 | TEXT | TEXT | |
| 날짜 | DATE | DATE | |
| 시간 | TIME | TIME | |
| 타임스탬프 | TIMESTAMP | DATETIME | |
| 참/거짓 | BOOLEAN | TINYINT(1) | |
| JSON | JSON, JSONB | JSON | |
| 바이너리 | BYTEA | BLOB | |
| UUID | UUID | CHAR(36) | MySQL은 네이티브 지원 없음 |
| 배열 | TEXT[], INT[] 등 | JSON | **중요: MySQL은 배열을 JSON으로 변환** |

### 배열 타입 처리

**PostgreSQL 전용 배열 타입:**
```sql
-- PostgreSQL에 저장된 데이터
data_type: "TEXT[]"
data_detail: null

-- PostgreSQL Export
CREATE TABLE example (
  tags TEXT[]
);

-- MySQL Export (자동 변환)
CREATE TABLE `example` (
  `tags` JSON
);
```

**주의사항:**
- PostgreSQL에서 배열로 정의된 컬럼은 MySQL로 export 시 JSON 타입으로 변환됩니다.
- 데이터 마이그레이션 시 배열 값을 JSON 배열로 변환해야 합니다.
- 예: `{1,2,3}` → `[1,2,3]`

## Export 규칙 및 기준

### 1. NULL 값 처리

| 항목 | NULL 또는 빈 값 처리 | 기본값 |
|------|---------------------|--------|
| `data_type` | TEXT로 변환 | TEXT |
| `data_detail` | 무시 (파라미터 없는 타입) | - |
| `default_value` | DEFAULT 절 생략 | - |
| `constraint_name` | 자동 생성 | `fk_{테이블명}_{컬럼명}` |
| `comment` | COMMENT 절 생성 안 함 | - |

### 2. 제약조건 이름 생성 규칙

**자동 생성 형식:**
```
fk_{from_table}_{from_column}
```

**규칙:**
1. 사용자가 지정한 제약조건 이름이 있으면 우선 사용
2. 없으면 `fk_` 접두사 + 테이블명 + 컬럼명으로 자동 생성
3. 특수문자는 언더스코어(`_`)로 변환
4. 길이 제한:
   - PostgreSQL: 63자 초과 시 잘림
   - MySQL: 64자 초과 시 잘림

**예시:**
```sql
-- 원본 데이터
from_table: "order_items"
from_column: "order_id"
constraint_name: NULL

-- 생성된 제약조건 이름
fk_order_items_order_id
```

### 3. DEFAULT 값 포맷팅

**숫자/Boolean/NULL/함수:**
```sql
DEFAULT 0
DEFAULT true
DEFAULT NULL
DEFAULT CURRENT_TIMESTAMP
DEFAULT now()
```

**문자열:**
```sql
DEFAULT '기본값'
DEFAULT ''
```

**자동 판별 규칙:**
- 숫자 패턴 (`-?\d+(\.\d+)?`): 그대로 출력
- `true`, `false`, `null` (대소문자 무시): 그대로 출력
- `CURRENT_*`, `()` 포함: 함수로 간주하여 그대로 출력
- 그 외: 작은따옴표로 감싸서 문자열로 처리

### 4. 컬럼 순서

**정렬 기준:**
1. `column_order` 필드 기준 오름차순 정렬
2. 동일한 `column_order` 값이 있을 경우 데이터베이스 순서 유지

**PRIMARY KEY 위치:**
- 컬럼 순서는 `column_order`를 따름
- PRIMARY KEY는 별도 제약조건으로 테이블 끝에 정의

### 5. 식별자 이스케이프

**PostgreSQL:**
```sql
-- 큰따옴표 사용
"table_name"
"column_name"

-- 큰따옴표가 포함된 경우 이중 이스케이프
"table""name"  -- table"name
```

**MySQL:**
```sql
-- 백틱 사용
`table_name`
`column_name`
```

### 6. 코멘트 처리

**우선순위:**
1. `comment` 필드가 있으면 사용
2. 없으면 `logical_name` 사용
3. 둘 다 없으면 코멘트 생성 안 함

**PostgreSQL:**
```sql
COMMENT ON TABLE "members" IS '회원';
COMMENT ON COLUMN "members"."email" IS '회원 이메일 주소';
```

**MySQL:**
```sql
CREATE TABLE `members` (
  `email` VARCHAR(255) NOT NULL COMMENT '회원 이메일 주소'
) COMMENT='회원';
```

### 7. AUTO_INCREMENT 처리

**is_incremental이 true인 경우:**

**PostgreSQL:**
```sql
-- 현재 구현: 타입 그대로 사용 (향후 SERIAL 변환 가능)
member_id BIGINT NOT NULL

-- 권장사항: BIGSERIAL 사용 시
member_id BIGSERIAL NOT NULL
```

**MySQL:**
```sql
`member_id` BIGINT NOT NULL AUTO_INCREMENT
```

## 제약사항 및 주의사항

### 1. 미지원 기능

다음 기능들은 현재 버전에서 지원하지 않습니다:

- **ENUM 타입**: VARCHAR 등으로 사전 변환 필요
- **CHECK 제약조건**: 별도 처리 필요
- **INDEX 생성**: DDL에 포함되지 않음
- **TRIGGER**: 포함되지 않음
- **VIEW**: ERD 테이블만 export
- **SEQUENCE**: PostgreSQL의 독립 시퀀스는 미포함
- **Geometric/Network 타입**: PostgreSQL 고급 타입 미지원

### 2. 데이터베이스별 제약

**PostgreSQL 제약:**
- Identifier 최대 길이: 63자
- ENUM 타입 정의는 별도 처리 필요
- 배열 타입은 그대로 유지

**MySQL 제약:**
- Identifier 최대 길이: 64자
- ENUM 타입은 별도 정의 필요
- 배열 타입은 JSON으로 자동 변환
- 엔진: InnoDB (고정)
- 문자셋: utf8mb4 (고정)
- Collation: utf8mb4_unicode_ci (고정)

### 3. 데이터 마이그레이션 고려사항

**ERD를 다른 DB로 마이그레이션할 때:**

1. **배열 타입 변환**
   ```sql
   -- PostgreSQL
   tags TEXT[] = '{tag1,tag2,tag3}'

   -- MySQL로 변환 시
   tags JSON = '["tag1","tag2","tag3"]'
   ```

2. **ENUM 타입**
   - 사전에 VARCHAR 등으로 정의 권장
   - 또는 애플리케이션 레벨에서 validation

3. **DEFAULT 함수 차이**
   ```sql
   -- PostgreSQL
   created_at TIMESTAMP DEFAULT now()

   -- MySQL
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP
   ```

4. **BOOLEAN 타입**
   ```sql
   -- PostgreSQL
   is_active BOOLEAN DEFAULT true

   -- MySQL
   is_active TINYINT(1) DEFAULT 1
   ```

## 사용 예시

### 예시 1: PostgreSQL DDL Export

**요청:**
```http
GET /api/v1/erd/projects/1/export/sql?dialect=POSTGRESQL
```

**응답:**
```sql
-- PostgreSQL DDL Export
-- Project Key: 1
-- Generated at: 2025-11-16T10:30:00

CREATE TABLE "members" (
  "member_id" BIGINT NOT NULL,
  "email" VARCHAR(255) NOT NULL UNIQUE,
  "name" VARCHAR(50) NOT NULL,
  "phone" VARCHAR(20),
  "created_at" TIMESTAMP NOT NULL DEFAULT now(),
  PRIMARY KEY ("member_id")
);

COMMENT ON TABLE "members" IS '회원';
COMMENT ON COLUMN "members"."member_id" IS '회원 고유 식별자';
COMMENT ON COLUMN "members"."email" IS '회원 이메일 주소';

CREATE TABLE "orders" (
  "order_id" BIGINT NOT NULL,
  "member_id" BIGINT NOT NULL,
  "total_amount" NUMERIC(12, 0) NOT NULL DEFAULT 0,
  "status" VARCHAR(20) NOT NULL DEFAULT '주문접수',
  PRIMARY KEY ("order_id")
);

-- Foreign Key Constraints
ALTER TABLE "orders"
  ADD CONSTRAINT "fk_orders_member_id"
  FOREIGN KEY ("member_id")
  REFERENCES "members" ("member_id")
  ON DELETE CASCADE
  ON UPDATE NO ACTION;
```

### 예시 2: MySQL DDL Export

**요청:**
```http
GET /api/v1/erd/projects/1/export/sql?dialect=MYSQL
```

**응답:**
```sql
-- MySQL DDL Export
-- Project Key: 1
-- Generated at: 2025-11-16T10:30:00

CREATE TABLE `members` (
  `member_id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '회원 이메일 주소',
  `name` VARCHAR(50) NOT NULL COMMENT '회원 이름',
  `phone` VARCHAR(20) COMMENT '휴대폰 번호',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원';

CREATE TABLE `orders` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT,
  `member_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(12, 0) NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT '주문접수',
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Foreign Key Constraints
ALTER TABLE `orders`
  ADD CONSTRAINT `fk_orders_member_id`
  FOREIGN KEY (`member_id`)
  REFERENCES `members` (`member_id`)
  ON DELETE CASCADE
  ON UPDATE NO ACTION;
```

## 트러블슈팅

### Q1: 배열 타입이 MySQL에서 작동하지 않습니다

**A:** MySQL은 네이티브 배열 타입을 지원하지 않습니다. Export 시 자동으로 JSON 타입으로 변환됩니다. 데이터 삽입 시 JSON 배열 형식을 사용하세요.

```sql
-- PostgreSQL
INSERT INTO products (tags) VALUES ('{electronics,laptop,gaming}');

-- MySQL
INSERT INTO products (tags) VALUES ('["electronics","laptop","gaming"]');
```

### Q2: 제약조건 이름이 너무 깁니다

**A:** PostgreSQL은 63자, MySQL은 64자로 자동 제한됩니다. 더 짧은 테이블명/컬럼명을 사용하거나, ERD에서 직접 constraint_name을 지정하세요.

### Q3: ENUM 타입은 어떻게 처리하나요?

**A:** 현재 버전에서는 ENUM 타입을 직접 지원하지 않습니다. 다음 방법을 권장합니다:

1. **VARCHAR로 정의** + 애플리케이션 레벨 validation
2. **CHECK 제약조건** 별도 추가 (수동)
3. **참조 테이블** 사용 (code_table 패턴)

### Q4: AUTO_INCREMENT가 제대로 동작하지 않습니다

**A:** `is_incremental` 플래그를 true로 설정했는지 확인하세요. PostgreSQL의 경우 SERIAL 타입 사용을 권장합니다.

## 버전 히스토리

- **v1.0.0** (2025-11-16): 초기 릴리스
  - PostgreSQL, MySQL DDL Export 지원
  - 기본 타입 매핑
  - 외래키 제약조건 생성
  - 배열 타입 자동 변환

## 참고 자료

- [PostgreSQL Data Types](https://www.postgresql.org/docs/current/datatype.html)
- [MySQL Data Types](https://dev.mysql.com/doc/refman/8.0/en/data-types.html)
- [PostgreSQL CREATE TABLE](https://www.postgresql.org/docs/current/sql-createtable.html)
- [MySQL CREATE TABLE](https://dev.mysql.com/doc/refman/8.0/en/create-table.html)
