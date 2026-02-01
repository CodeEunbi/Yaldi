package com.yaldi.infra.s3.service;

import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    public String uploadFile(String directory, String fileName, String content) {
        try {
            String key = directory + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("text/plain; charset=utf-8")
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromString(content, StandardCharsets.UTF_8)
            );

            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, key);

            log.info("S3 업로드 성공: {}", s3Url);

            return s3Url;

        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage(), e);
            throw new GeneralException(ErrorStatus.S3_UPLOAD_FAILED);
        }
    }

    public String generatePresignedUrl(String s3Url, Duration expiration) {
        try {
            // S3 URL에서 key 추출
            String key = extractKeyFromS3Url(s3Url);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.info("Presigned URL 생성 성공");

            return presignedUrl;

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패");
            throw new GeneralException(ErrorStatus.S3_PRESIGNED_URL_FAILED);
        }
    }

    private String extractKeyFromS3Url(String s3Url) {
        // URL에서 bucket/region 이후의 경로 추출
        String pattern = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (s3Url.startsWith(pattern)) {
            return s3Url.substring(pattern.length());
        }
        throw new GeneralException(ErrorStatus.S3_INVALID_URL);
    }
}
