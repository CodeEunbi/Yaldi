package com.yaldi.infra.security.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;

    /** JWT Secret 최소 길이 (256비트 = 32바이트) */
    private static final int MIN_SECRET_LENGTH = 32;

    /**
     * 애플리케이션 시작 시 JWT Secret 검증
     */
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    String.format("JWT secret must be at least %d bytes (256 bits). Current length: %d bytes",
                            MIN_SECRET_LENGTH, keyBytes.length)
            );
        }

        log.info("JWT secret validated successfully. Length: {} bytes", keyBytes.length);
    }
}
