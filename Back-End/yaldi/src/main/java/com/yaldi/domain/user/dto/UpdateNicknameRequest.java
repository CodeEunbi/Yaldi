package com.yaldi.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 닉네임 변경 요청 DTO
 *
 * <p>Jackson 설정에 의해 문자열은 자동으로 trim 및 개행문자 제거 처리됩니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateNicknameRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 1, max = 10, message = "닉네임은 1~10자 이내여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_가-힣]+$", message = "닉네임은 영문, 숫자, 한글, 언더스코어만 허용됩니다")
    private String nickname;
}
