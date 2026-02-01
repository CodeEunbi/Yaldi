package com.yaldi.infra.security.oauth2;

import com.yaldi.domain.user.dto.OAuth2RegistrationResult;
import com.yaldi.domain.user.entity.AuthType;
import com.yaldi.domain.user.entity.Provider;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.entity.UserSocialAccount;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.domain.user.repository.UserSocialAccountRepository;
import com.yaldi.domain.user.service.UserRegistrationService;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * OpenID Connect (OIDC) 사용자 정보를 처리하는 서비스
 *
 * <p>Google OAuth2에서 'openid' scope 사용 시 OIDC 방식으로 처리됩니다.
 * ID Token에서 사용자 정보를 추출하여 DB에 저장하거나 업데이트합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final UserRegistrationService userRegistrationService;

    /**
     * OIDC 사용자 정보를 로드하고 DB에 저장합니다.
     */
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 부모 클래스의 loadUser()를 호출하여 OidcUser 생성
        // 이 과정에서 ID Token 검증 및 UserInfo Endpoint 호출이 수행됨
        OidcUser oidcUser = super.loadUser(userRequest);

        // 2. OIDC 사용자 정보 처리 (DB 저장/업데이트)
        processOidcUser(userRequest, oidcUser);

        // 3. OidcUser 반환 (Spring Security가 Authentication의 Principal로 사용)
        return oidcUser;
    }

    /**
     * OIDC 사용자 정보를 처리하여 DB에 저장하거나 업데이트합니다.
     */
    private void processOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        // 1. Provider 정보 추출
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        // 2. OIDC Claims에서 사용자 정보 추출
        // 'sub'는 OIDC 표준 클레임으로 사용자의 고유 식별자
        String providerId = oidcUser.getAttribute("sub");
        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");

        log.debug("Processing OIDC user - Provider: {}, ProviderId: {}, Email: {}",
                provider, providerId, email);

        AuthType authType;

        // 3. Provider와 ProviderId로 기존 소셜 계정 찾기
        Optional<UserSocialAccount> socialAccountOptional = socialAccountRepository
                .findByProviderAndOauthUserId(provider, providerId);

        User user;
        if (socialAccountOptional.isPresent()) {
            // 기존 사용자 (삭제된 경우 자동 복구)
            Integer userKey = socialAccountOptional.get().getUserKey();
            user = userRepository.findById(userKey)
                    .orElseGet(() -> {
                        // 없으면 삭제된 사용자니까 복구하고 다시 조회
                        userRepository.restoreDeletedUser(userKey);
                        User restored = userRepository.findById(userKey)
                                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
                        log.info("Restored deleted user via OIDC login: {}", restored.getEmail());
                        return restored;
                    });

            authType = AuthType.LOGIN;
            log.info("Existing user logged in: {}", user.getEmail());
        } else {
            // 신규 사용자 등록
            OAuth2RegistrationResult result = userRegistrationService.registerNewOAuth2User(email, name, provider, providerId);
            user = result.getUser();
            authType = result.getAuthType();
            log.info("New user registered: {}, authType: {}", user.getEmail(), authType);
        }

        // AuthType을 Request Attribute에 저장하여 SuccessHandler에서 사용
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        request.setAttribute("authType", authType);
        request.setAttribute("userEmail", user.getEmail());
    }
}
