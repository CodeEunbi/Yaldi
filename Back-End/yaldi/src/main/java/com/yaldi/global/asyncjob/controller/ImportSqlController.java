package com.yaldi.global.asyncjob.controller;

import com.yaldi.global.asyncjob.dto.ImportSqlRequest;
import com.yaldi.global.asyncjob.entity.AsyncJob;
import com.yaldi.global.asyncjob.service.AsyncJobService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.kafka.dto.ImportSqlEvent;
import com.yaldi.infra.kafka.service.KafkaProducerService;
import com.yaldi.infra.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ImportSqlController {

    private final AsyncJobService asyncJobService;
    private final KafkaProducerService kafkaProducerService;

    @PostMapping("/{projectKey}/import/sql")
    public ApiResponse<?> importSql(
            @PathVariable Long projectKey,
            @RequestBody ImportSqlRequest request
    ) {

        AsyncJob job = asyncJobService.createJob("IMPORT_VALIDATE", 1, projectKey);

        // Kafka 발행
        kafkaProducerService.sendMessage(
                "async-job-topic",
                job.getJobId(),
                new ImportSqlEvent(
                        job.getJobId(),
                        projectKey,
                        SecurityUtil.getCurrentUserKey(),
                        request.getSqlContent()
                )
        );

        log.info("[Kafka 발행 완료] JobId={}", job.getJobId());

        return ApiResponse.onSuccess(
                Map.of(
                        "jobId", job.getJobId(),
                        "status", "PENDING"
                )
        );
    }
}
