package com.yaldi.domain.team.repository;

import com.yaldi.domain.team.entity.UserTeamRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserTeamRelationRepository extends JpaRepository<UserTeamRelation, Integer> {

    List<UserTeamRelation> findByUser_UserKey(Integer userKey);

    List<UserTeamRelation> findByTeam_TeamKey(Integer teamKey);

    //팀 멤버 조회
    @Query("SELECT r FROM UserTeamRelation r JOIN FETCH r.user WHERE r.team.teamKey = :teamKey")
    List<UserTeamRelation> findByTeam_TeamKeyWithUser(Integer teamKey);

    // 특정 팀의 멤버 userKey 목록만 조회 (성능 최적화)
    @Query("SELECT r.user.userKey FROM UserTeamRelation r WHERE r.team.teamKey = :teamKey")
    Set<Integer> findUserKeysByTeam_TeamKey(Integer teamKey);

    Optional<UserTeamRelation> findByUser_UserKeyAndTeam_TeamKey(Integer userKey, Integer teamKey);

    boolean existsByUser_UserKeyAndTeam_TeamKey(Integer userKey, Integer teamKey);

    //특정 팀의 관계 삭제
    void deleteByTeam_TeamKey(Integer teamKey);

    //특정 유저의 모든 팀 관계 삭제
    void deleteByUser_UserKey(Integer userKey);

    long countByTeam_TeamKey(Integer teamKey);
}
