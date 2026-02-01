# 인증 패턴 가이드

## REST API vs WebSocket 인증 방식 차이

### REST API Controller

```java
// ❌ 사용하지 않음
public ApiResponse<?> getMyInfo(Authentication authentication) {
    Integer userKey = (Integer) authentication.getPrincipal();
    // ...
}

// ✅ 권장 방식
public ApiResponse<?> getMyInfo() {
    Integer userKey = SecurityUtil.getCurrentUserKey();
    // ...
}
```

**이유:**
- JWT 필터에서 `SecurityContext`에 인증 정보 저장
- 파라미터 없이 어디서든 접근 가능
- 일관된 에러 처리

---

### WebSocket Controller

```java
// ✅ WebSocket에서는 Principal 사용
@MessageMapping("/erd/table/lock")
public void handleTableLock(@Payload TableLockEvent event, Principal principal) {
    String userEmail = principal.getName();  // email 반환
    User user = userRepository.findByEmail(userEmail).orElse(null);
    // ...
}
```

**이유:**
- WebSocket은 handshake 시점에 한 번만 인증
- `principal.getName()` → **email** 반환 (REST API와 다름)
- Spring WebSocket 표준 방식

---

## 핵심 차이점

| 구분 | REST API | WebSocket |
|------|----------|-----------|
| **인증 시점** | 매 요청마다 (JWT 필터) | Handshake 시 1회 |
| **Principal.getName()** | userKey (String) | userEmail (String) |
| **권장 방식** | `SecurityUtil.getCurrentUserKey()` | `Principal` 파라미터 |
| **User 조회** | userKey로 직접 조회 | email → DB 조회 |

---

## 적용 예시

### REST API
```java
@GetMapping("/api/v1/users/me")
public ApiResponse<?> getMyInfo() {
    Integer userKey = SecurityUtil.getCurrentUserKey();
    return ApiResponse.onSuccess(userService.getUserInfo(userKey));
}
```

### WebSocket
```java
@MessageMapping("/erd/cursor")
public void handleCursorMove(@Payload CursorPosEvent event, Principal principal) {
    String userEmail = principal.getName();
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    // ...
}
```

---

## 참고

- REST API: `JwtAuthenticationFilter.java:226-239`
- WebSocket: `WebSocketConnectionInterceptor.java:62-116`
