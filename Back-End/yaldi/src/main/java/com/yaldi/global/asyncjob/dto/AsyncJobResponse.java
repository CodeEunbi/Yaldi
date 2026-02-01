package com.yaldi.global.asyncjob.dto;

import com.yaldi.global.asyncjob.entity.AsyncJob;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class AsyncJobResponse {
    private String jobId;
    private String jobType;
    private AsyncJobStatus status;
    private String errorMessage;
    private OffsetDateTime completedAt;

    public static AsyncJobResponse from(AsyncJob job) {
        return AsyncJobResponse.builder()
                .jobId(job.getJobId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
