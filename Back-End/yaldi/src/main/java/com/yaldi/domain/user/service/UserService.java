package com.yaldi.domain.user.service;

import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.entity.UserTeamActionType;
import com.yaldi.domain.team.entity.UserTeamHistory;
import com.yaldi.domain.team.entity.UserTeamRelation;
import com.yaldi.domain.team.repository.TeamRepository;
import com.yaldi.domain.team.repository.UserTeamHistoryRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.domain.team.service.TeamService;
import com.yaldi.domain.user.dto.OwnedTeamInfoResponse;
import com.yaldi.domain.user.dto.SocialAccountListResponse;
import com.yaldi.domain.user.dto.TeamMemberInfo;
import com.yaldi.domain.user.dto.UpdateNicknameRequest;
import com.yaldi.domain.user.dto.UserResponse;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.entity.UserSocialAccount;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.domain.user.repository.UserSocialAccountRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final RefreshTokenService refreshTokenService;
    private final TeamRepository teamRepository;
    private final UserTeamRelationRepository userTeamRelationRepository;
    private final UserTeamHistoryRepository userTeamHistoryRepository;
    private final TeamService teamService;

    /**
     * 사용자 정보 조회
     *
     * @param userKey 사용자 키
     * @return 사용자 정보
     * @throws GeneralException USER_NOT_FOUND 사용자를 찾을 수 없는 경우
     * @throws GeneralException USER_DELETED 탈퇴한 사용자인 경우
     */
    public UserResponse getUserInfo(Integer userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 탈퇴한 사용자 확인
        if (user.getDeletedAt() != null) {
            throw new GeneralException(ErrorStatus.USER_DELETED);
        }

        return UserResponse.from(user);
    }

    /**
     * 닉네임 변경
     *
     * @param userKey 사용자 키
     * @param request 닉네임 변경 요청
     * @return 변경된 사용자 정보
     * @throws GeneralException USER_NOT_FOUND 사용자를 찾을 수 없는 경우
     * @throws GeneralException USER_DELETED 탈퇴한 사용자인 경우
     * @throws DataIntegrityViolationException 중복된 닉네임인 경우
     */
    @Transactional
    public UserResponse updateNickname(Integer userKey, UpdateNicknameRequest request) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 탈퇴한 사용자 확인
        if (user.getDeletedAt() != null) {
            throw new GeneralException(ErrorStatus.USER_DELETED);
        }

        // 현재 닉네임과 동일한 경우 변경하지 않고 그대로 반환
        if (user.getNickname().equals(request.getNickname())) {
            log.info("Nickname unchanged: userKey={}, nickname={}", userKey, request.getNickname());
            return UserResponse.from(user);
        }

        // 다른 사용자가 사용 중인 닉네임인지 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new DataIntegrityViolationException("이미 사용 중인 닉네임입니다: " + request.getNickname());
        }

        user.updateNickname(request.getNickname());
        User updatedUser = userRepository.save(user);

        log.info("User nickname updated: userKey={}, newNickname={}", userKey, request.getNickname());

        return UserResponse.from(updatedUser);
    }

    /**
     * 사용자의 소셜 계정 목록 조회
     *
     * <p>user_social_accounts 테이블에서 user_key로 직접 조회합니다.</p>
     *
     * @param userKey 사용자 키
     * @return 소셜 계정 목록 응답 (data + totalElements)
     */
    public SocialAccountListResponse getUserSocialAccounts(Integer userKey) {
        List<UserSocialAccount> socialAccounts = socialAccountRepository.findByUserKey(userKey);
        return SocialAccountListResponse.from(socialAccounts);
    }

    /**
     * 사용자 삭제 (Soft Delete)
     *
     * <p>사용자 탈퇴 시 다음 작업을 수행합니다:</p>
     * <ul>
     *   <li> 오너인 팀이 있는지 확인, </li>
     *   <li> 오너가 아닌 일반 멤버인 팀의 경우 관계 삭제 </li>
     *   <li>사용자 데이터 Soft Delete (deleted_at 설정)</li>
     *   <li>Redis에 저장된 Refresh Token 삭제 (즉시 로그아웃)</li>
     * </ul>
     *
     * @param userKey 사용자 키
     * @throws GeneralException USER_NOT_FOUND 사용자를 찾을 수 없는 경우
     * @throws GeneralException USER_DELETED 이미 탈퇴한 사용자인 경우
     * @throws GeneralException USER_HAS_OWNED_TEAMS 오너인 팀이 있는 경우
     */
    @Transactional
    public void deleteUser(Integer userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 이미 탈퇴한 사용자 확인
        if (user.getDeletedAt() != null) throw new GeneralException(ErrorStatus.USER_DELETED);

        // 1. 오너인 팀 검증 (먼저 전체 확인, 삭제는 나중에)
        List<Team> ownedTeams = teamRepository.findByOwnerUserKey(userKey);
        List<Team> teamsToDelete = new java.util.ArrayList<>();
        List<OwnedTeamInfoResponse> teamsWithMultipleMembersInfo = new java.util.ArrayList<>();

        for (Team team : ownedTeams) {
            long memberCount = userTeamRelationRepository.countByTeam_TeamKey(team.getTeamKey());

            if (memberCount == 1) {
                // 혼자 있는 팀 : 삭제 예정 목록에 추가
                teamsToDelete.add(team);
            } else {
                // 2명 이상 : 오너 이양 필요 목록에 추가
                // 팀 멤버 정보 조회 (본인 제외)
                List<UserTeamRelation> relations = userTeamRelationRepository.findByTeam_TeamKey(team.getTeamKey());
                List<Integer> memberUserKeys = relations.stream()
                        .map(relation -> relation.getUser().getUserKey())
                        .filter(key -> !key.equals(userKey))  // 본인(기존 오너) 제외
                        .collect(Collectors.toList());

                List<User> members = userRepository.findAllById(memberUserKeys);
                List<TeamMemberInfo> memberInfos = members.stream()
                        .map(member -> new TeamMemberInfo(member.getUserKey(), member.getNickname()))
                        .collect(Collectors.toList());

                teamsWithMultipleMembersInfo.add(new OwnedTeamInfoResponse(
                        team.getTeamKey(),
                        team.getName(),
                        memberCount,
                        memberInfos
                ));
            }
        }

        // 오너 이양이 필요한 팀이 있으면 팀 정보와 함께 에러를 던짐
        if (!teamsWithMultipleMembersInfo.isEmpty()) {
            String teamNames = teamsWithMultipleMembersInfo.stream()
                    .map(OwnedTeamInfoResponse::name)
                    .collect(Collectors.joining(", "));
            log.warn("오너 권한을 넘겨줘야하는 팀 존재: userKey={}, teams=[{}]", userKey, teamNames);
            throw new GeneralException(ErrorStatus.USER_HAS_OWNED_TEAMS, teamsWithMultipleMembersInfo);
        }

        // 혼자 있는 팀 삭제
        for (Team team : teamsToDelete) {
            log.info("팀원이 1명(본인)인 팀 자동 삭제: teamKey={}, name={}", team.getTeamKey(), team.getName());
            teamService.deleteTeam(userKey, team.getTeamKey());
        }

        // 2. 일반 멤버인 팀의 관계 삭제 및 탈퇴 이력 저장
        List<UserTeamRelation> userTeamRelations = userTeamRelationRepository.findByUser_UserKey(userKey);
        for (UserTeamRelation relation : userTeamRelations) {
            // 사용자 탈퇴로 인한 팀 이탈 이력 저장
            UserTeamHistory withdrawalHistory = UserTeamHistory.builder()
                    .team(relation.getTeam())
                    .actor(user)
                    .target(user)
                    .email(user.getEmail())
                    .actionType(UserTeamActionType.MEMBER_WITHDRAWAL)
                    .reason("사용자 서비스 탈퇴")
                    .build();
            userTeamHistoryRepository.save(withdrawalHistory);
        }
        userTeamRelationRepository.deleteByUser_UserKey(userKey);

        // 3. 사용자 데이터 Soft Delete
        user.softDelete();
        userRepository.save(user);

        // 4. Redis에서 Refresh Token 삭제 (즉시 로그아웃)
        refreshTokenService.deleteRefreshToken(userKey);

        log.info("User soft deleted and refresh token removed: userKey={}", userKey);
    }
}
