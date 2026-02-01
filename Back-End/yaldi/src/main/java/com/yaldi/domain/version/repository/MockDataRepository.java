package com.yaldi.domain.version.repository;

import com.yaldi.domain.version.entity.MockData;
import com.yaldi.global.asyncjob.entity.AsyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockDataRepository extends JpaRepository<MockData, Long> {

    Optional<MockData> findByAsyncJob(AsyncJob asyncJob);

    List<MockData> findByVersionKeyOrderByCreatedAtDesc(Long versionKey);
}
