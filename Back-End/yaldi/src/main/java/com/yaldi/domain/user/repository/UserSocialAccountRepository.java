package com.yaldi.domain.user.repository;

import com.yaldi.domain.user.entity.Provider;
import com.yaldi.domain.user.entity.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Integer> {

    /**
     * Provider와 Provider User ID로 소셜 계정 조회
     * OAuth 로그인 시 사용
     */
    Optional<UserSocialAccount> findByProviderAndOauthUserId(Provider provider, String providerUserId);

    /**
     * userKey로 소셜 계정 목록 조회
     * 사용자의 연결된 소셜 계정 목록 조회 시 사용
     */
    @Query("SELECT sa FROM UserSocialAccount sa WHERE sa.user.userKey = :userKey")
    List<UserSocialAccount> findByUserKey(Integer userKey);
}
