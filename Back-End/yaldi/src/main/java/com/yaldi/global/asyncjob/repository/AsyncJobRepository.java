package com.yaldi.global.asyncjob.repository;

import com.yaldi.global.asyncjob.entity.AsyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsyncJobRepository extends JpaRepository<AsyncJob, String> {
}
