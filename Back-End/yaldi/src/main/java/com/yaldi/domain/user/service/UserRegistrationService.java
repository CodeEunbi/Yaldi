package com.yaldi.domain.user.service;

import com.yaldi.domain.user.dto.OAuth2RegistrationResult;
import com.yaldi.domain.user.entity.AuthType;
import com.yaldi.domain.user.entity.Provider;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.entity.UserSocialAccount;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.domain.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 등록 서비스
 *
 * <p>OAuth2 로그인 시 신규 사용자를 등록하고 소셜 계정 정보를 저장합니다.</p>
 *
 * @author Yaldi Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final UserNicknameService nicknameService;

    /**
     * OAuth2 사용자를 등록하거나 기존 사용자에 소셜 계정을 추가합니다.
     *
     * <h3>처리 로직</h3>
     * <ol>
     *   <li><strong>케이스 1:</strong> 이메일이 DB에 없음 → User + UserSocialAccount 생성 (SIGNUP)</li>
     *   <li><strong>케이스 2:</strong> 동일 이메일 + 다른 Provider → 기존 User에 UserSocialAccount 추가 (LOGIN)</li>
     *   <li><strong>케이스 3:</strong> 탈퇴한 사용자 → 사용자 복구 (REJOIN)</li>
     * </ol>
     *
     * <h3>예시</h3>
     * <ul>
     *   <li>user@example.com으로 Google 로그인 → User 생성 (SIGNUP)</li>
     *   <li>user@example.com으로 GITHUB 로그인 → 기존 User에 GITHUB 계정 연결 (LOGIN)</li>
     *   <li>탈퇴한 user@example.com으로 로그인 → 계정 복구 (REJOIN)</li>
     * </ul>
     *
     * @param email 사용자 이메일
     * @param name 사용자 이름
     * @param provider OAuth2 제공자 (GOOGLE, GITHUB, SSAFY 등)
     * @param providerId 제공자의 사용자 고유 ID
     * @return OAuth2 등록 결과 (User 엔티티 + AuthType)
     */
    @Transactional
    public OAuth2RegistrationResult registerNewOAuth2User(String email, String name, Provider provider, String providerId) {
        AuthType authType;

        // 1. 이메일로 기존 사용자 찾기 (삭제된 사용자 포함)
        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    // 활성 사용자가 존재하는 경우
                    return existingUser;
                })
                .orElse(null);

        if (user != null) {
            // 케이스 2: 기존 사용자 - 다른 provider로 로그인
            authType = AuthType.LOGIN;
            log.info("Existing user found: userKey={}, email={}. Adding new provider: {}",
                    user.getUserKey(), user.getEmail(), provider);
        } else {
            // 활성 사용자가 없으면, 삭제된 사용자 확인 및 복구
            User deletedUser = userRepository.findByEmailIncludingDeleted(email)
                    .orElse(null);

            if (deletedUser != null) {
                // 케이스 3: 탈퇴한 사용자 복구 (재가입)
                userRepository.restoreDeletedUser(deletedUser.getUserKey());
                user = userRepository.findById(deletedUser.getUserKey())
                        .orElseThrow(() -> new IllegalStateException("User restore failed"));
                authType = AuthType.REJOIN;
                log.info("Restored deleted user via OAuth: {} ({})", user.getNickname(), user.getEmail());
            } else {
                // 케이스 1: 신규 사용자 - User 생성
                user = User.builder()
                        .email(email)
                        .nickname(nicknameService.generateUniqueNickname(name))
                        .build();
                user = userRepository.save(user);
                authType = AuthType.SIGNUP;
                log.info("New user created: userKey={}, email={}", user.getUserKey(), user.getEmail());
            }
        }

        // 2. 소셜 계정 연동 정보 저장
        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .user(user)
                .provider(provider)
                .oauthUserId(providerId)
                .build();
        socialAccountRepository.save(socialAccount);

        log.info("Social account linked: provider={}, providerId={}, authType={}", provider, providerId, authType);

        return new OAuth2RegistrationResult(user, authType);
    }
}
