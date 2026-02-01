package com.yaldi.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지네이션 응답 DTO
 *
 * <p>Spring Data의 Page를 content 대신 data 필드로 반환하고, 메타 정보를 meta 객체에 담습니다.</p>
 */
@Schema(description = "페이지네이션 응답")
public record PageResponse<T>(
    @Schema(description = "데이터 목록")
    List<T> data,

    @Schema(description = "페이지네이션 메타 정보")
    PageMetaInfo meta
) {

    /**
     * 페이지네이션 메타 정보
     */
    @Schema(description = "페이지네이션 메타 정보")
    public record PageMetaInfo(
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "현재 페이지의 요소 개수", example = "20")
        int numberOfElements,

        @Schema(description = "전체 요소 개수", example = "100")
        long totalElements,

        @Schema(description = "전체 페이지 개수", example = "5")
        int totalPages,

        @Schema(description = "첫 페이지 여부", example = "true")
        boolean first,

        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last,

        @Schema(description = "빈 페이지 여부", example = "false")
        boolean empty,

        @Schema(description = "정렬 정보")
        SortInfo sort
    ) {}

    /**
     * 정렬 정보
     */
    @Schema(description = "정렬 정보")
    public record SortInfo(
        @Schema(description = "정렬 여부", example = "true")
        boolean sorted,

        @Schema(description = "정렬되지 않음 여부", example = "false")
        boolean unsorted,

        @Schema(description = "빈 정렬 여부", example = "false")
        boolean empty
    ) {}

    /**
     * Spring Data Page를 PageResponse로 변환
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        SortInfo sortInfo = new SortInfo(
            page.getSort().isSorted(),
            page.getSort().isUnsorted(),
            page.getSort().isEmpty()
        );

        PageMetaInfo meta = new PageMetaInfo(
            page.getNumber(),
            page.getSize(),
            page.getNumberOfElements(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.isEmpty(),
            sortInfo
        );

        return new PageResponse<>(
            page.getContent(),
            meta
        );
    }
}
