package com.yaldi.domain.team.repository;

import com.yaldi.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    // 소유자로 활성 팀 목록 조회 (삭제되지 않은)
    @Query("SELECT t FROM Team t WHERE t.owner.userKey = :ownerUserKey AND t.deletedAt IS NULL")
    List<Team> findByOwnerUserKey(Integer ownerUserKey);

    //여러 ID로 활성 팀 목록 조회 (삭제되지 않은)
    @Query("SELECT t FROM Team t WHERE t.teamKey IN :teamKeys AND t.deletedAt IS NULL")
    List<Team> findActiveTeamsByIds(List<Integer> teamKeys);

    //활성 팀 조회 (삭제되지 않은)
    @Query("SELECT t FROM Team t WHERE t.teamKey = :teamKey AND t.deletedAt IS NULL")
    Optional<Team> findActiveTeamById(Integer teamKey);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Team t WHERE t.name = :name AND t.deletedAt IS NULL")
    boolean existsByName(String name);
}
