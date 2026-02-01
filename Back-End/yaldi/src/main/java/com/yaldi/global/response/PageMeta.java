package com.yaldi.global.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 메타 정보
 *
 * <p>리스트 응답에 포함되는 메타 정보를 담는 공통 클래스입니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMeta {

    private Integer totalElements;

    //현재 페이지 번호 (0부터 시작)
    private Integer currentPage;

    // 페에지 크기
    private Integer size;
    private Boolean hasNext;

    public static PageMeta of(Integer totalElements) {
        return PageMeta.builder()
                .totalElements(totalElements)
                .build();
    }

    public static PageMeta of(Page<?> page) {
        return PageMeta.builder()
                .totalElements((int) page.getTotalElements())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .hasNext(page.hasNext())
                .build();
    }
}
