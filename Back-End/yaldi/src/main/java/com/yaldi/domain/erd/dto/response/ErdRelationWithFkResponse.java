package com.yaldi.domain.erd.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdRelationWithFkResponse {
    private ErdRelationResponse erdRelationResponse;
    private List<ErdColumnResponse> columns;
}
