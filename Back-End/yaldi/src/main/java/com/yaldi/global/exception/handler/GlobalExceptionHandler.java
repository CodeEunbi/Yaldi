package com.yaldi.global.exception.handler;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     * - 서비스 레이어에서 명시적으로 throw한 GeneralException
     */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(GeneralException ex) {
        // result를 항상 배열로 통일 (null인 경우 빈 배열)
        Object result = ex.getData();
        if (result == null) {
            result = List.of();
        } else if (!(result instanceof List)) {
            // 단일 값인 경우 리스트로 래핑
            result = List.of(result);
        }

        return ResponseEntity
                .status(ex.getErrorStatus().getHttpStatus())
                .body(ApiResponse.onFailure(
                        ex.getErrorStatus(),
                        result
                ));
    }

    /**
     * DTO Validation 예외 처리
     * - @Valid 어노테이션으로 검증 실패 (@NotNull, @Min, @Size 등)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex
    ) {
        List<String> errorMessages = getValidationErrorMessages(ex);
        return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(
                        ErrorStatus.BAD_REQUEST,
                        errorMessages
                ));
    }

    /**
     * Path Variable/Query Parameter Validation 예외 처리
     * - @Validated 클래스 레벨 검증 실패 (@PathVariable, @RequestParam 등)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        List<String> errorMessages = getValidationErrorMessages(ex);

        return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(
                        ErrorStatus.BAD_REQUEST,
                        errorMessages
                ));
    }

    /**
     * JSON 파싱 예외 처리
     * - JSON 형식 오류, 타입 불일치 (String을 Integer로 받는 경우 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex
    ) {
        log.warn("JSON 파싱 오류: {}", ex.getMessage());

        List<String> errorMessages = List.of("잘못된 요청 형식입니다.");

        // 타입 불일치 에러 상세 메시지 추출
        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            String fieldName = invalidFormatEx.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .findFirst()
                    .orElse("unknown");

            String targetType = invalidFormatEx.getTargetType().getSimpleName();
            Object value = invalidFormatEx.getValue();

            String errorMessage = String.format("%s: 올바른 %s 형식이 아닙니다 (입력값: %s)",
                    fieldName, targetType, value);
            errorMessages = List.of(errorMessage);
        }

        return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(
                        ErrorStatus.BAD_REQUEST,
                        errorMessages
                ));
    }

    /**
     * 404 Not Found 예외 처리
     * - 존재하지 않는 API 엔드포인트 호출
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex
    ) {
        log.warn("404 Not Found: {}", ex.getRequestURL());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.onFailure(
                        ErrorStatus.NOT_FOUND,
                        List.of("요청한 리소스를 찾을 수 없습니다: " + ex.getRequestURL())
                ));
    }

    /**
     * 404 Not Found 예외 처리 (Static Resource)
     * - 존재하지 않는 정적 리소스 요청
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("404 Not Found: {}", request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.onFailure(
                        ErrorStatus.NOT_FOUND,
                        List.of("요청한 리소스를 찾을 수 없습니다: " + request.getRequestURI())
                ));
    }

    /**
     * DB 무결성 제약조건 위반 예외 처리
     * - Foreign Key 제약 위반, Unique 제약 위반, Not Null 제약 위반
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Data Integrity Violation at {}: {}", request.getRequestURI(), ex.getMessage());

        String errorMessage = "데이터 무결성 제약 조건 위반이 발생했습니다.";

        // 구체적인 에러 메시지 추출
        if (ex.getMostSpecificCause() != null) {
            String detailMessage = ex.getMostSpecificCause().getMessage();
            if (detailMessage != null) {
                if (detailMessage.contains("foreign key constraint")) {
                    errorMessage = "참조 무결성 제약 조건 위반: 관련된 데이터가 존재합니다.";
                } else if (detailMessage.contains("unique constraint")) {
                    errorMessage = "중복된 데이터가 존재합니다.";
                } else if (detailMessage.contains("not-null constraint")) {
                    errorMessage = "필수 값이 누락되었습니다.";
                }
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.onFailure(
                        ErrorStatus.CONFLICT,
                        List.of(errorMessage)
                ));
    }

    /**
     * HTTP Method 미지원 예외 처리
     * - 지원하지 않는 HTTP Method 사용 (GET 엔드포인트에 POST 요청 등)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex
    ) {
        log.warn("Method Not Supported: {} for {}", ex.getMethod(), ex.getSupportedHttpMethods());
        String errorMessage = String.format("지원하지 않는 HTTP Method입니다: %s (지원: %s)",
                ex.getMethod(), ex.getSupportedHttpMethods());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.onFailure(
                        ErrorStatus.METHOD_NOT_ALLOWED,
                        List.of(errorMessage)
                ));
    }

    /**
     * 필수 요청 파라미터 누락 예외 처리
     * - @RequestParam(required=true) 파라미터가 누락된 경우
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex
    ) {
        log.warn("Missing Request Parameter: {}", ex.getParameterName());
        String errorMessage = String.format("필수 파라미터가 누락되었습니다: %s (%s)",
                ex.getParameterName(), ex.getParameterType());

        return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(
                        ErrorStatus.BAD_REQUEST,
                        List.of(errorMessage)
                ));
    }

    /**
     * 메서드 인자 타입 불일치 예외 처리
     * - @PathVariable, @RequestParam 타입 변환 실패 (String을 Integer로 변환 실패 등)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex
    ) {
        log.warn("Type Mismatch: parameter '{}' with value '{}'", ex.getName(), ex.getValue());
        String errorMessage = String.format("%s: 올바른 형식이 아닙니다 (입력값: %s)",
                ex.getName(), ex.getValue());

        return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.onFailure(
                        ErrorStatus.BAD_REQUEST,
                        List.of(errorMessage)
                ));
    }

    /**
     * 파일 업로드 크기 초과 예외 처리
     * - MultipartFile 업로드 시 최대 크기 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex
    ) {
        log.warn("Upload Size Exceeded: {}", ex.getMessage());
        String errorMessage = "업로드 파일 크기가 제한을 초과했습니다.";

        if (ex.getMaxUploadSize() > 0) {
            long maxSizeMB = ex.getMaxUploadSize() / (1024 * 1024);
            errorMessage = String.format("업로드 파일 크기가 제한을 초과했습니다 (최대: %dMB)", maxSizeMB);
        }

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.onFailure(
                        ErrorStatus.PAYLOAD_TOO_LARGE,
                        List.of(errorMessage)
                ));
    }

    /**
     * 인증 실패 예외 처리
     * - Spring Security 인증 실패 (JWT 토큰 없음, 만료 등)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex
    ) {
        log.warn("Authentication Failed: {}", ex.getMessage());

        return ResponseEntity
                .status(ErrorStatus.UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.onFailure(
                        ErrorStatus.UNAUTHORIZED,
                        List.of("인증에 실패했습니다.")
                ));
    }

    /**
     * 권한 없음 예외 처리
     * - Spring Security 인가 실패 (접근 권한 없음)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex
    ) {
        log.warn("Access Denied: {}", ex.getMessage());

        return ResponseEntity
                .status(ErrorStatus.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.onFailure(
                        ErrorStatus.FORBIDDEN,
                        List.of("접근 권한이 없습니다.")
                ));
    }

    /**
     * 기타 모든 예외 처리 (Fallback)
     * - 위에서 처리되지 않은 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected Exception at {}: ", request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(
                        ErrorStatus.INTERNAL_SERVER_ERROR,
                        List.of("서버 내부 오류가 발생했습니다.")
                ));
    }

    /**
     * DTO Validation 에러 메시지 추출
     * - @Valid 검증 실패 시 FieldError 목록을 "필드명: 메시지" 형식으로 변환
     */
    private List<String> getValidationErrorMessages(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> {
                    String field = fieldError.getField();       // 실패한 필드 이름
                    String message = fieldError.getDefaultMessage(); // 검증 실패 메시지
                    return field + ": " + message;
                })
                .toList();
    }

    /**
     * PathVariable/RequestParam Validation 에러 메시지 추출
     * - @Validated 클래스 레벨 검증 실패 시 ConstraintViolation 목록을 "메소드명.파라미터명: 메시지" 형식으로 변환
     */
    private List<String> getValidationErrorMessages(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString(); // 위반된 필드 (예: getProjectsByTeam.teamKey)
                    String message = violation.getMessage();              // 검증 실패 메시지
                    return field + ": " + message;
                })
                .toList();
    }
}
