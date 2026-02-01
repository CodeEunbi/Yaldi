package com.yaldi.domain.consultation.repository;

import com.yaldi.domain.consultation.entity.ConsultationMessage;
import com.yaldi.domain.consultation.entity.ConsultationMessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, Long> {

    List<ConsultationMessage> findByProjectKeyOrderByCreatedAtAsc(Long projectKey);

    List<ConsultationMessage> findTop20ByProjectKeyOrderByCreatedAtDesc(Long projectKey);

    List<ConsultationMessage> findByProjectKeyAndRoleOrderByCreatedAtAsc(
            Long projectKey,
            ConsultationMessageRole role
    );

    long countByProjectKey(Long projectKey);

    void deleteByProjectKey(Long projectKey);
}
