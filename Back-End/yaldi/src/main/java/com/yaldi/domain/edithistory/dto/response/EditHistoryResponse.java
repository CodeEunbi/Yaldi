package com.yaldi.domain.edithistory.dto.response;

import com.yaldi.domain.edithistory.entity.EditHistory;
import com.yaldi.domain.edithistory.entity.EditHistoryActionType;
import com.yaldi.domain.edithistory.entity.EditHistoryTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * EditHistory 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditHistoryResponse {

    private Long editHistoryKey;
    private Integer userKey;
    private Long projectKey;
    private Long targetKey;
    private EditHistoryTargetType targetType;
    private EditHistoryActionType actionType;
    private Map<String, Object> delta;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private OffsetDateTime createdAt;

    public static EditHistoryResponse from(EditHistory editHistory) {
        return EditHistoryResponse.builder()
                .editHistoryKey(editHistory.getEditHistoryKey())
                .userKey(editHistory.getUserKey())
                .projectKey(editHistory.getProjectKey())
                .targetKey(editHistory.getTargetKey())
                .targetType(editHistory.getTargetType())
                .actionType(editHistory.getActionType())
                .delta(editHistory.getDelta())
                .beforeState(editHistory.getBeforeState())
                .afterState(editHistory.getAfterState())
                .createdAt(editHistory.getCreatedAt())
                .build();
    }
}
