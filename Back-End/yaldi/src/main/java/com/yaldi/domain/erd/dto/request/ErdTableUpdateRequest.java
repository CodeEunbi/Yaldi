package com.yaldi.domain.erd.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ERD 테이블 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdTableUpdateRequest {

    @NotBlank(message = "논리명은 필수입니다")
    @Size(max = 100, message = "논리명은 100자 이하여야 합니다")
    private String logicalName;

    @NotBlank(message = "물리명은 필수입니다")
    @Size(max = 64, message = "물리명은 64자 이하여야 합니다")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "물리명은 영문, 숫자, 언더스코어만 사용 가능하며 숫자로 시작할 수 없습니다")
    private String physicalName;

    @DecimalMin(value = "0.0", message = "X 좌표는 0 이상이어야 합니다")
    private BigDecimal xPosition;

    @DecimalMin(value = "0.0", message = "Y 좌표는 0 이상이어야 합니다")
    private BigDecimal yPosition;

    @Pattern(regexp = "^[0-9A-Fa-f]{6}$", message = "잘못된 색상 형식입니다")
    private String colorHex;
}
