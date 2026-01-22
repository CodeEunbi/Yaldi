package ssafy.yaldi.global.response.status;

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

    /*
    =========================================================================
    Member (4000번대)
    =========================================================================
    */
    MEMBER_NOT_EXIST(HttpStatus.NOT_FOUND, "MEMBER4000", "입력하신 회원이 존재하지 않습니다."),
    MEMBER_DUPLICATE_BY_EMAIL(HttpStatus.BAD_REQUEST, "MEMBER4001", "이메일이 중복됩니다."),
    MEMBER_NOT_REGISTERED_BY_GOOGLE(HttpStatus.BAD_REQUEST, "MEMBER4002", "신규 유저입니다. 회원가입이 필요합니다."),
    MEMBER_DUPLICATE_BY_NICKNAME(HttpStatus.BAD_REQUEST, "MEMBER4003", "닉네임이 중복됩니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
