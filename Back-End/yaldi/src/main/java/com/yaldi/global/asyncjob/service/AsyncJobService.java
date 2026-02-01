package com.yaldi.global.asyncjob.service;

import com.yaldi.global.asyncjob.entity.AsyncJob;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.asyncjob.repository.AsyncJobRepository;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.global.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비동기 작업 공통 서비스
 * 모든 Kafka 기반 작업에서 재사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncJobService {

    private final AsyncJobRepository asyncJobRepository;

    @Transactional
    public AsyncJob createJob(String jobType, Integer userKey, Long referenceKey) {
        String jobId = IdGenerator.generateJobId();

        AsyncJob job = AsyncJob.builder()
                .jobId(jobId)
                .jobType(jobType)
                .userKey(userKey)
                .referenceKey(referenceKey)
                .status(AsyncJobStatus.PENDING)
                .build();

        AsyncJob savedJob = asyncJobRepository.save(job);

        log.info("비동기 작업 생성 - JobId: {}, Type: {}, UserKey: {}",jobId, jobType, userKey);

        return savedJob;
    }

    @Transactional
    public AsyncJob updateStatus(String jobId, AsyncJobStatus status) {
        AsyncJob job = asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.JOB_NOT_FOUND));

        job.updateStatus(status);

        log.info("작업 상태 업데이트 - JobId: {}, Status: {} → {}",jobId, job.getStatus(), status);
        return job;
    }

    @Transactional
    public AsyncJob failJob(String jobId, String errorMessage) {
        AsyncJob job = asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.JOB_NOT_FOUND));

        job.fail(errorMessage);

        log.error("작업 실패 - JobId: {}, Error: {}", jobId, errorMessage);
        return job;
    }

    @Transactional(readOnly = true)
    public AsyncJob getJob(String jobId) {
        return asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.JOB_NOT_FOUND));
    }
}
