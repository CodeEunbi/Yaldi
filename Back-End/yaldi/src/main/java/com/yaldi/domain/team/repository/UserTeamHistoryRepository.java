package com.yaldi.domain.team.repository;

import com.yaldi.domain.team.entity.UserTeamActionType;
import com.yaldi.domain.team.entity.UserTeamHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTeamHistoryRepository extends JpaRepository<UserTeamHistory, Long> {

    // 특정 팀과 이메일로 특정 액션 타입의 히스토리 조회 (초대 관련)
    Optional<UserTeamHistory> findByTeam_TeamKeyAndEmailAndActionType(
            Integer teamKey,
            String email,
            UserTeamActionType actionType
    );

    // 특정 팀과 이메일로 가장 최신 히스토리 조회 (초대 상태 확인용)
    Optional<UserTeamHistory> findFirstByTeam_TeamKeyAndEmailOrderByCreatedAtDesc(
            Integer teamKey,
            String email
    );

    //특정 팀과 액션 타입으로 히스토리 조회 (초대 상태 확인용)
    List<UserTeamHistory> findByTeam_TeamKeyAndActionType(Integer teamKey, UserTeamActionType actionType);

    // 특정 팀과 액션 타입으로 히스토리 조회 (actor, target fetch join으로 N+1 방지)
    @Query("SELECT h FROM UserTeamHistory h " +
            "JOIN FETCH h.actor " +
            "JOIN FETCH h.target " +
            "WHERE h.team.teamKey = :teamKey AND h.actionType = :actionType")
    List<UserTeamHistory> findByTeam_TeamKeyAndActionTypeWithUsers(
            @Param("teamKey") Integer teamKey,
            @Param("actionType") UserTeamActionType actionType
    );

    List<UserTeamHistory> findByTeam_TeamKeyAndEmailOrderByCreatedAtAsc(Integer teamKey, String email);
}
