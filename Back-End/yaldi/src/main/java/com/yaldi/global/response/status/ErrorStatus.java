package com.yaldi.global.response.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus {

    /*
    =========================================================================
    Common (공통)
    =========================================================================
    */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "로그인 인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405", "지원하지 않는 HTTP Method입니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON409", "데이터 충돌이 발생했습니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "COMMON413", "요청 데이터가 너무 큽니다."),

    /*
    =========================================================================
    Auth & Token (3900번대)
    =========================================================================
    */
    TOKEN_INVALID_ACCESS(HttpStatus.UNAUTHORIZED, "AUTH3900", "유효하지 않은 Access Token입니다. 다시 로그인해주세요."),
    TOKEN_INVALID_REFRESH(HttpStatus.UNAUTHORIZED, "AUTH3901", "유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."),
    TOKEN_NOT_FOUND_REFRESH(HttpStatus.UNAUTHORIZED, "AUTH3902", "Refresh Token을 찾을 수 없습니다. 다시 로그인해주세요."),
    TOKEN_MISMATCH_REFRESH(HttpStatus.UNAUTHORIZED, "AUTH3903", "Refresh Token이 일치하지 않습니다. 보안을 위해 로그아웃되었습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH3904", "토큰이 만료되었습니다. 다시 로그인해주세요."),
    TOKEN_FORCED_LOGOUT(HttpStatus.UNAUTHORIZED, "AUTH3905", "보안상의 이유로 강제 로그아웃되었습니다. 다시 로그인해주세요."),

    /*
    =========================================================================
    User (4000번대)
    =========================================================================
    */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4000", "사용자를 찾을 수 없습니다."),
    USER_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "USER4001", "이메일이 중복됩니다."),
    USER_DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "USER4002", "닉네임이 중복됩니다."),
    USER_INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER4003", "닉네임은 3-20자 영문/숫자/언더스코어만 허용됩니다."),
    USER_DELETED(HttpStatus.UNAUTHORIZED, "USER4004", "탈퇴한 사용자입니다. 다시 로그인해주세요."),
    USER_HAS_OWNED_TEAMS(HttpStatus.BAD_REQUEST, "USER4005", "오너인 팀이 있습니다. 먼저 오너를 이양하세요."),

    /*
    =========================================================================
    Team (4100번대)
    =========================================================================
    */
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM4100", "팀을 찾을 수 없습니다."),
    TEAM_FORBIDDEN(HttpStatus.FORBIDDEN, "TEAM4101", "이 팀에 대한 권한이 없습니다."),
    TEAM_DUPLICATE_MEMBER(HttpStatus.BAD_REQUEST, "TEAM4102", "이미 팀에 속한 멤버입니다."),
    TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM4103", "팀 멤버를 찾을 수 없습니다."),
    TEAM_OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "TEAM4104", "팀 소유자는 탈퇴할 수 없습니다."),
    TEAM_DUPLICATE_NAME(HttpStatus.BAD_REQUEST, "TEAM4105", "이미 사용 중인 팀 이름입니다."),
    TEAM_NOT_MEMBER(HttpStatus.FORBIDDEN, "TEAM4106", "팀 멤버가 아닙니다."),
    TEAM_OWNER_ONLY(HttpStatus.FORBIDDEN, "TEAM4107", "팀 오너만 수정할 수 있습니다."),
    TEAM_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM4108", "팀 초대를 찾을 수 없습니다."),
    TEAM_INVITATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "TEAM4109", "이미 대기 중인 초대가 있습니다."),
    TEAM_INVITATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "TEAM4110", "이미 처리된 초대입니다."),
    TEAM_CANNOT_INVITE_SELF(HttpStatus.BAD_REQUEST, "TEAM4111", "자기 자신을 초대할 수 없습니다."),
    TEAM_INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "TEAM4112", "만료된 초대입니다."),
    TEAM_INVITATION_CANCELED(HttpStatus.BAD_REQUEST, "TEAM4113", "취소된 초대입니다."),

    /*
    =========================================================================
    Project (4200번대)
    =========================================================================
    */
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT4200", "프로젝트를 찾을 수 없습니다."),
    PROJECT_FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT4201", "이 프로젝트에 대한 권한이 없습니다."),
    PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROJECT4202", "이 프로젝트에 대한 권한이 없습니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT4203", "프로젝트 멤버를 찾을 수 없습니다."),
    PROJECT_DUPLICATE_MEMBER(HttpStatus.BAD_REQUEST, "PROJECT4204", "이미 프로젝트에 속한 멤버입니다."),
    PROJECT_INVALID_ROLE(HttpStatus.BAD_REQUEST, "PROJECT4205", "유효하지 않은 역할입니다."),
    PROJECT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "PROJECT4206", "이 작업을 수행할 권한이 없습니다."),
    CHANGE_PROJECT_OWNER_FIRST(HttpStatus.BAD_REQUEST,"PROJECT4207","프로젝트 오너를 먼저 위임하세요."),
    PROJECT_OWNER_DUPLICATE(HttpStatus.BAD_REQUEST,"PROJECT4207","프로젝트 오너는 한 명만 존재할 수 있습니다."),
    /*
    =========================================================================
    ERD Table (4300번대)
    =========================================================================
    */

    // TODO 아래 error status 리팩토링 필요
    
    ERD_TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERD_TABLE4300", "ERD 테이블을 찾을 수 없습니다."),
    ERD_TABLE_DUPLICATE_PHYSICAL_NAME(HttpStatus.BAD_REQUEST, "ERD_TABLE4301", "중복된 물리명입니다."),
    ERD_TABLE_INVALID_POSITION(HttpStatus.BAD_REQUEST, "ERD_TABLE4302", "유효하지 않은 위치입니다."),

    TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "TABLE4300", "테이블을 찾을 수 없습니다."),
    TABLE_DUPLICATE_PHYSICAL_NAME(HttpStatus.BAD_REQUEST, "TABLE4301", "중복된 물리명입니다."),
    TABLE_INVALID_POSITION(HttpStatus.BAD_REQUEST, "TABLE4302", "유효하지 않은 위치입니다."),
    TABLE_HAS_NO_COLUMNS(HttpStatus.BAD_REQUEST, "TABLE4303", "테이블에 컬럼이 없습니다."),
    TABLE_PROJECT_MISMATCH(HttpStatus.BAD_REQUEST, "TABLE4304", "해당 프로젝트에 속하지 않은 테이블입니다."),


    /*
    =========================================================================
    ERD Column (4400번대)
    =========================================================================
    */

    // TODO 아래 error status 리팩토링 필요
    
    ERD_COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "ERD_COLUMN4400", "ERD 컬럼을 찾을 수 없습니다."),
    ERD_COLUMN_DUPLICATE_PHYSICAL_NAME(HttpStatus.BAD_REQUEST, "ERD_COLUMN4401", "중복된 컬럼 물리명입니다."),
    ERD_COLUMN_INVALID_DATA_TYPE(HttpStatus.BAD_REQUEST, "ERD_COLUMN4402", "유효하지 않은 데이터 타입입니다."),
    ERD_COLUMN_PRIMARY_KEY_CONSTRAINT(HttpStatus.BAD_REQUEST, "ERD_COLUMN4403", "Primary Key 수정 시 제약사항 검증 필요합니다."),

    COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "COLUMN4400", "컬럼을 찾을 수 없습니다."),
    COLUMN_DUPLICATE_PHYSICAL_NAME(HttpStatus.BAD_REQUEST, "COLUMN4401", "중복된 컬럼 물리명입니다."),
    COLUMN_INVALID_DATA_TYPE(HttpStatus.BAD_REQUEST, "COLUMN4402", "유효하지 않은 데이터 타입입니다."),
    COLUMN_PRIMARY_KEY_CONSTRAINT(HttpStatus.BAD_REQUEST, "COLUMN4403", "Primary Key 수정 시 제약사항 검증 필요합니다."),
    COLUMN_TABLE_MISMATCH(HttpStatus.BAD_REQUEST, "COLUMN4404", "컬럼이 지정된 테이블에 속하지 않습니다."),

    /*
    =========================================================================
    ERD Relation (4500번대)
    =========================================================================
    */
    ERD_RELATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERD_RELATION4500", "ERD 관계를 찾을 수 없습니다."),
    ERD_RELATION_INVALID_TYPE(HttpStatus.BAD_REQUEST, "ERD_RELATION4501", "유효하지 않은 관계 타입입니다."),
    ERD_RELATION_CIRCULAR_REFERENCE(HttpStatus.BAD_REQUEST, "ERD_RELATION4502", "순환 참조는 허용되지 않습니다."),

    /*
    =========================================================================
    Comment & Reply (4600번대)
    =========================================================================
    */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT4600", "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT4601", "이 댓글에 대한 권한이 없습니다."),
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT4602", "답글을 찾을 수 없습니다."),
    REPLY_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT4603", "이 답글에 대한 권한이 없습니다."),

    /*
    =========================================================================
    Version & Snapshot (4700번대)
    =========================================================================
    */
    VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "VERSION4700", "버전을 찾을 수 없습니다."),
    VERSION_FORBIDDEN(HttpStatus.FORBIDDEN, "VERSION4701", "이 버전에 대한 권한이 없습니다."),
    SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "VERSION4702", "스냅샷을 찾을 수 없습니다."),
    SNAPSHOT_DUPLICATE_NAME(HttpStatus.BAD_REQUEST, "VERSION4703", "중복된 스냅샷 이름입니다."),
    VERSION_PROJECT_MISMATCH(HttpStatus.BAD_REQUEST, "VERSION4704", "버전이 해당 프로젝트에 속하지 않습니다."),
    VERSION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "VERSION4705", "디자인 검증이 성공한 버전만 Mock 데이터를 생성할 수 있습니다."),

    /*
    =========================================================================
    DataModel (4750번대)
    =========================================================================
    */
    DATA_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "DATA_MODEL4750", "데이터 모델을 찾을 수 없습니다."),
    DATA_MODEL_NAME_DUPLICATE(HttpStatus.BAD_REQUEST, "DATA_MODEL4751", "중복된 모델 이름입니다."),
    DATA_MODEL_FORBIDDEN(HttpStatus.FORBIDDEN, "DATA_MODEL4752", "이 데이터 모델에 대한 권한이 없습니다."),
    DATA_MODEL_INVALID_TYPE(HttpStatus.BAD_REQUEST, "DATA_MODEL4753", "유효하지 않은 데이터 모델 타입입니다. DTO_REQUEST 또는 DTO_RESPONSE만 가능합니다."),
    DATA_MODEL_CANNOT_REFRESH(HttpStatus.BAD_REQUEST, "DATA_MODEL4754", "관련 컬럼이 삭제되어 Refresh할 수 없습니다. 재생성이 필요합니다."),

    /*
    =========================================================================
    Notification (4800번대)
    =========================================================================
    */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION4800", "알림을 찾을 수 없습니다."),

    /*
    =========================================================================
    Mail (4850번대)
    =========================================================================
    */
    MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MAIL4850", "이메일 발송에 실패했습니다."),

    /*
    =========================================================================
    AI & Agent (4900번대)
    =========================================================================
    */
    AI_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "AI4900", "AI 검증에 실패했습니다."),
    AI_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI4901", "AI 요청 제한을 초과했습니다."),
    AGENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "AI4902", "AI 요청을 찾을 수 없습니다."),

    /*
    =========================================================================
    DTO & Code Generation (5000번대)
    =========================================================================
    */
    DTO_NOT_FOUND(HttpStatus.NOT_FOUND, "DTO5000", "DTO를 찾을 수 없습니다."),
    DTO_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DTO5001", "DTO 생성에 실패했습니다."),
    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "DTO5002", "데이터 모델을 찾을 수 없습니다."),

    /*
    =========================================================================
    Mock Data (5100번대)
    =========================================================================
    */
    MOCK_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "MOCK5100", "Mock 데이터를 찾을 수 없습니다."),
    MOCK_DATA_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MOCK5101", "Mock 데이터 생성에 실패했습니다."),
    MOCK_DATA_INVALID_ROW_COUNT(HttpStatus.BAD_REQUEST, "MOCK5102", "rowCount는 1-10000 사이여야 합니다."),
    MOCK_DATA_EMPTY_SCHEMA(HttpStatus.BAD_REQUEST, "MOCK5103", "스키마에 테이블이 없습니다."),
    VERSION_MOCK_DATA_MISMATCH(HttpStatus.BAD_REQUEST, "MOCK5104", "Mock 데이터가 해당 버전에 속하지 않습니다."),

    /*
    =========================================================================
    SQL Import/Export (5200번대)
    =========================================================================
    */
    SQL_PARSE_ERROR(HttpStatus.BAD_REQUEST, "SQL5200", "SQL 파싱에 실패했습니다."),
    SQL_INVALID_SYNTAX(HttpStatus.BAD_REQUEST, "SQL5201", "유효하지 않은 SQL 문법입니다."),
    SQL_EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SQL5202", "SQL 내보내기에 실패했습니다."),

    /*
    =========================================================================
    WebSocket & Realtime (5300번대)
    =========================================================================
    */
    WEBSOCKET_CONNECTION_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "WS5300", "동시 접속 제한을 초과했습니다."),
    WEBSOCKET_MESSAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "WS5301", "메시지 크기가 너무 큽니다."),
    WEBSOCKET_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "WS5302", "메시지 전송 속도 제한을 초과했습니다."),
    OPERATION_CONFLICT(HttpStatus.CONFLICT, "WS5303", "작업 충돌이 발생했습니다."),
    OPERATION_FAILED(HttpStatus.BAD_REQUEST, "WS5304", "작업 실행에 실패했습니다."),
    WEBSOCKET_UNSUPPORTED_EVENT(HttpStatus.BAD_REQUEST, "WS5305", "지원하지 않는 WebSocket 이벤트 타입입니다."),


    /*
    =========================================================================
    Lock (5400번대)
    =========================================================================
    */
    LOCK_ABSENT(HttpStatus.BAD_REQUEST ,"LOCK5400", "해당 테이블에 대한 락이 없습니다."),
    LOCK_ALREADY_EXIST(HttpStatus.BAD_REQUEST ,"LOCK5401", "이미 테이블에 대한 락이 존재합니다."),
    UNMATCH_WITH_LOCK_OWNER(HttpStatus.UNAUTHORIZED ,"LOCK5402", "락 소유주가 아닙니다."),
    FAIL_TO_UNLOCK(HttpStatus.BAD_REQUEST,"LOCK5403", "락 해제에 실패했습니다."),

    /*
    =========================================================================
    Async Job (5450번대)
    =========================================================================
    */
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "JOB5450", "작업을 찾을 수 없습니다."),

    /*
    =========================================================================
    S3 (5500번대)
    =========================================================================
    */
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_5500", "파일 업로드에 실패했습니다."),
    S3_PRESIGNED_URL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_5501", "다운로드 URL 생성에 실패했습니다."),
    S3_INVALID_URL(HttpStatus.BAD_REQUEST, "S3_5502", "잘못된 S3 URL 형식입니다."),

    /*
    =========================================================================
    SSE (5600번대)
    =========================================================================
    */
    SSEEMITTER_NOT_FOUND(HttpStatus.BAD_REQUEST,"SSE_5600","해당 사용자는 현재 접속중이지 않습니다."),

    /*
    =========================================================================
    Viewer (5700번대)
    =========================================================================
    */
    INVALID_VIEWER_LINK(HttpStatus.BAD_REQUEST, "VIEWER_5700", "유효하지 않은 뷰어링크입니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
