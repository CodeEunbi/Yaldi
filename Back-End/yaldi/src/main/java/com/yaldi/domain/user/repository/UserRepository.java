package com.yaldi.domain.user.repository;

import com.yaldi.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * 이메일로 사용자 조회
     * OAuth 로그인 시 사용자 등록/조회에 사용
     */
    Optional<User> findByEmail(String email);

    /**
     * 삭제된 사용자 포함 전체 조회
     * Dev 환경에서만 사용
     */
    @Query(value = "SELECT * FROM users", nativeQuery = true)
    List<User> findAllIncludingDeleted();

    /**
     * 삭제된 사용자 포함 ID로 조회
     * Dev 환경에서 탈퇴한 사용자 복구에 사용
     */
    @Query(value = "SELECT * FROM users WHERE user_key = :userKey", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(Integer userKey);

    /**
     * 삭제된 사용자 포함 이메일로 조회
     * OAuth 로그인 시 탈퇴한 사용자 복구에 사용
     */
    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(String email);

    /**
     * 삭제된 사용자 복구 (deleted_at을 NULL로 설정)
     */
    @Modifying
    @Query(value = "UPDATE users SET deleted_at = NULL WHERE user_key = :userKey", nativeQuery = true)
    void restoreDeletedUser(Integer userKey);

    /**
     * 이메일로 활성 사용자 조회 (삭제되지 않은)
     * Spring Security UserDetailsService에서 사용
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveUserByEmail(String email);

    /**
     * 닉네임 중복 체크
     * 닉네임 변경 및 생성 시 중복 검증에 사용
     */
    boolean existsByNickname(String nickname);

    /**
     * 닉네임으로 활성 사용자 조회
     */
    Optional<User> findByNickname(String nickname);

    /**
     * ID로 활성 사용자 조회 (삭제되지 않은)
     * JWT 인증 필터에서 사용
     */
    @Query("SELECT u FROM User u WHERE u.userKey = :userKey AND u.deletedAt IS NULL")
    Optional<User> findActiveUserById(Integer userKey);

    /**
     * 닉네임 또는 이메일로 활성 사용자 검색 (LIKE)
     * 팀 초대 시 사용자 검색에 사용
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (u.nickname LIKE %:keyword% OR u.email LIKE %:keyword%)")
    List<User> searchActiveUsersByKeyword(String keyword);
}
