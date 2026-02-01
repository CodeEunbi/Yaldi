package com.yaldi.domain.user.service;

import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 닉네임 생성 서비스
 *
 * <p>OAuth2 로그인 시 중복되지 않는 고유한 닉네임을 생성합니다.</p>
 *
 * @author Yaldi Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNicknameService {

    private final UserRepository userRepository;

    /** 최대 닉네임 길이 */
    private static final int MAX_NICKNAME_LENGTH = 10;

    /** 숫자 접미사를 붙일 때 기본 닉네임 최대 길이 */
    private static final int NICKNAME_BASE_LENGTH_WITH_SUFFIX = 7;

    /** 닉네임 생성 최대 시도 횟수 */
    private static final int MAX_GENERATION_ATTEMPTS = 10000;

    /** 기본 닉네임 */
    private static final String DEFAULT_NICKNAME = "User";

    /**
     * 중복되지 않는 고유한 닉네임을 생성합니다.
     *
     * <p>기본 이름으로 시작하여, 중복이 있으면 숫자를 붙여 고유성을 보장합니다.
     * 닉네임은 최대 10자로 제한됩니다.</p>
     *
     * @param baseName 기본 이름 (OAuth2 제공자로부터 받은 이름)
     * @return 고유한 닉네임
     * @throws GeneralException 최대 시도 횟수를 초과한 경우
     */
    public String generateUniqueNickname(String baseName) {
        // 닉네임 정제 (공백, 개행 등 제거)
        String sanitized = StringUtils.sanitizeNickname(baseName);

        if (sanitized == null || sanitized.isEmpty()) {
            sanitized = DEFAULT_NICKNAME;
        }

        // 최대 길이 제한
        String nickname = sanitized.substring(0, Math.min(sanitized.length(), MAX_NICKNAME_LENGTH));

        // 중복 체크
        if (!userRepository.existsByNickname(nickname)) {
            return nickname;
        }

        // 중복일 경우 숫자 붙이기 (최대 시도 횟수 제한)
        int suffix = 1;
        while (suffix < MAX_GENERATION_ATTEMPTS) {
            String numberedNickname = nickname.substring(0, Math.min(nickname.length(), NICKNAME_BASE_LENGTH_WITH_SUFFIX)) + suffix;
            if (!userRepository.existsByNickname(numberedNickname)) {
                return numberedNickname;
            }
            suffix++;
        }

        log.error("Failed to generate unique nickname after {} attempts for base name: {}", MAX_GENERATION_ATTEMPTS, baseName);
        throw new GeneralException(ErrorStatus.INTERNAL_SERVER_ERROR);
    }
}
