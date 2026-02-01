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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 사용자 정보를 처리하는 서비스
 *
 * <p>GitHub, SSAFY 등 OAuth2 표준 방식의 인증을 처리합니다.
 * Google은 OIDC 방식이므로 {@link CustomOidcUserService}에서 처리합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRegistrationService userRegistrationService;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        processOAuth2User(userRequest, oAuth2User);
        return oAuth2User;
    }

    private void processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = Provider.valueOf(registrationId.toUpperCase());
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Provider별 사용자 정보 추출
        String providerId = extractProviderId(provider, attributes);
        String email = extractEmail(provider, attributes, userRequest);
        String name = extractName(provider, attributes);

        log.debug("Processing OAuth2 user - Provider: {}, ProviderId: {}, Email: {}",
                provider, providerId, email);

        AuthType authType;

        // Provider와 ProviderId로 기존 소셜 계정 찾기
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
                        log.info("Restored deleted user via OAuth login: {}", restored.getEmail());
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

        // AuthType과 email을 Request Attribute에 저장하여 SuccessHandler에서 사용
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        request.setAttribute("authType", authType);
        request.setAttribute("userEmail", user.getEmail());
    }

    /**
     * Provider별 사용자 고유 ID 추출
     */
    private String extractProviderId(Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GITHUB -> {
                Object id = attributes.get("id");
                yield id != null ? String.valueOf(id) : null;
            }
            case SSAFY -> {
                Object sub = attributes.get("sub");
                if (sub != null) yield String.valueOf(sub);
                Object id = attributes.get("id");
                yield id != null ? String.valueOf(id) : null;
            }
            case GOOGLE -> {
                // Google은 정상적으로 OIDC (CustomOidcUserService)로 처리되어야 함
                // 여기로 오면 설정 오류일 가능성이 높음 (openid scope 누락 등)
                log.warn("Google authentication is being processed by OAuth2UserService instead of OidcUserService. " +
                        "Check if 'openid' scope is configured in application.yml");
                Object sub = attributes.get("sub");
                yield sub != null ? String.valueOf(sub) : null;
            }
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    /**
     * 이메일 추출 (GitHub의 경우 이메일이 없을 때 재요청 및 fallback 처리)
     */
    private String extractEmail(Provider provider, Map<String, Object> attributes, OAuth2UserRequest userRequest) {
        String email = (String) attributes.get("email");

        // GitHub 전용 이메일 처리 로직
        if (provider == Provider.GITHUB && (email == null || email.trim().isEmpty())) {
            log.warn("GitHub email is null or empty, attempting to fetch from GitHub API");

            // GitHub API를 통해 이메일 재요청
            email = fetchGitHubEmail(userRequest);

            // 그래도 이메일이 없으면 fallback 이메일 생성
            if (email == null || email.trim().isEmpty()) {
                String providerId = extractProviderId(provider, attributes);
                email = providerId + "@github.oauth.yaldi.com";
                log.warn("GitHub email not available, using fallback email: {}", email);
            } else {
                log.info("Successfully fetched GitHub email from API: {}", email);
            }
        }

        return email;
    }

    /**
     * GitHub API를 통해 사용자 이메일 목록 조회
     * primary이면서 verified된 이메일을 우선 반환
     */
    private String fetchGitHubEmail(OAuth2UserRequest userRequest) {
        try {
            String accessToken = userRequest.getAccessToken().getTokenValue();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (response.getBody() != null) {
                // primary이면서 verified된 이메일 찾기
                for (Map<String, Object> emailData : response.getBody()) {
                    Boolean primary = (Boolean) emailData.get("primary");
                    Boolean verified = (Boolean) emailData.get("verified");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailData.get("email");
                    }
                }

                // primary만 있는 경우
                for (Map<String, Object> emailData : response.getBody()) {
                    Boolean primary = (Boolean) emailData.get("primary");
                    if (Boolean.TRUE.equals(primary)) {
                        return (String) emailData.get("email");
                    }
                }

                // verified만 있는 경우
                for (Map<String, Object> emailData : response.getBody()) {
                    Boolean verified = (Boolean) emailData.get("verified");
                    if (Boolean.TRUE.equals(verified)) {
                        return (String) emailData.get("email");
                    }
                }

                // 아무거나 첫 번째 이메일
                if (!response.getBody().isEmpty()) {
                    return (String) response.getBody().get(0).get("email");
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitHub email from API", e);
        }

        return null;
    }

    /**
     * Provider별 이름 추출
     */
    private String extractName(Provider provider, Map<String, Object> attributes) {
        String name = (String) attributes.get("name");

        // GitHub는 name이 없으면 login(username) 사용
        if ((name == null || name.trim().isEmpty()) && provider == Provider.GITHUB) {
            name = (String) attributes.get("login");
        }

        // SSAFY는 name이 없으면 username 사용
        if ((name == null || name.trim().isEmpty()) && provider == Provider.SSAFY) {
            name = (String) attributes.get("username");
        }

        return name;
    }
}
