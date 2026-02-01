package com.yaldi.domain.viewer.dto;

/**
 * 뷰어 링크 검증 정보
 * - linkId로부터 조회한 프로젝트 정보를 담는 DTO
 */
public record ViewerLinkInfo(Long projectKey) {
}
