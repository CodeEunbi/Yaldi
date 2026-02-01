package com.yaldi.domain.project.controller;

import com.yaldi.domain.project.dto.request.AddProjectMemberRequest;
import com.yaldi.domain.project.dto.request.CreateProjectRequest;
import com.yaldi.domain.project.dto.request.UpdateProjectMemberRoleRequest;
import com.yaldi.domain.project.dto.request.UpdateProjectRequest;
import com.yaldi.domain.project.dto.response.AddProjectMembersResponse;
import com.yaldi.domain.project.dto.response.ProjectMemberHistoryResponse;
import com.yaldi.domain.project.dto.response.ProjectMemberResponse;
import com.yaldi.domain.project.dto.response.ProjectResponse;
import com.yaldi.domain.project.service.ProjectMemberService;
import com.yaldi.domain.project.service.ProjectService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.PageResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="Project", description = "프로젝트 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Validated
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다")
    @PostMapping
    public ApiResponse<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        ProjectResponse response = projectService.createProject(userKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "프로젝트 조회", description = "프로젝트 상세 정보를 조회합니다")
    @GetMapping("/{projectKey}")
    public ApiResponse<ProjectResponse> getProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        ProjectResponse response = projectService.getProject(userKey, projectKey);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "내 프로젝트 목록 조회", description = "현재 사용자가 속한 모든 프로젝트를 조회합니다")
    @GetMapping("/my")
    public ApiResponse<PageResponse<ProjectResponse>> getMyProjects(
            Pageable pageable
    ) {
        // 기본 정렬: lastActivityAt DESC, name ASC
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize() > 0 ? pageable.getPageSize() : 20,
                Sort.by(
                    Sort.Order.desc("lastActivityAt"),
                    Sort.Order.asc("name")
                )
            );
        }

        Integer userKey = SecurityUtil.getCurrentUserKey();
        Page<ProjectResponse> page = projectService.getMyProjects(userKey, pageable);
        return ApiResponse.onSuccess(PageResponse.of(page));
    }

    @Operation(summary = "팀의 프로젝트 목록 조회", description = "특정 팀의 모든 프로젝트를 조회합니다 (페이징, 최근 활동순 → 이름순)")
    @GetMapping("/team/{teamKey}")
    public ApiResponse<PageResponse<ProjectResponse>> getProjectsByTeam(
            @Parameter(description = "팀 ID", required = true)
            @PathVariable @Min(value = 1, message = "팀 ID는 1 이상이어야 합니다") Integer teamKey,
            Pageable pageable
    ) {
        // 기본 정렬: lastActivityAt DESC, name ASC
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize() > 0 ? pageable.getPageSize() : 20,
                Sort.by(
                    Sort.Order.desc("lastActivityAt"),
                    Sort.Order.asc("name")
                )
            );
        }

        Integer userKey = SecurityUtil.getCurrentUserKey();
        Page<ProjectResponse> page = projectService.getProjectsByTeam(userKey, teamKey, pageable);
        return ApiResponse.onSuccess(PageResponse.of(page));
    }

    @Operation(summary = "프로젝트 수정", description = "프로젝트 정보를 수정합니다 (OWNER, ADMIN만 가능)")
    @PatchMapping("/{projectKey}")
    public ApiResponse<ProjectResponse> updateProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        ProjectResponse response = projectService.updateProject(userKey, projectKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 소프트 삭제합니다 (OWNER만 가능). 30일 후 자동으로 완전 삭제됩니다.")
    @DeleteMapping("/{projectKey}")
    public ApiResponse<Void> deleteProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectService.deleteProject(userKey, projectKey);
        return ApiResponse.onSuccess(null);
    }

    @Operation(
        summary = "프로젝트 강제 삭제",
        description = "프로젝트를 즉시 완전 삭제합니다 (복구 불가능, OWNER만 가능). 테스트 또는 긴급 상황에서만 사용하세요."
    )
    @DeleteMapping("/{projectKey}/force")
    public ApiResponse<Void> forceDeleteProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectService.forceDeleteProject(userKey, projectKey);
        return ApiResponse.onSuccess(null);
    }

    // ==================== 프로젝트 멤버 관리 API ====================

    @Operation(summary = "프로젝트 멤버 목록 조회", description = "프로젝트의 모든 멤버를 조회합니다")
    @GetMapping("/{projectKey}/members")
    public ApiResponse<List<ProjectMemberResponse>> getProjectMembers(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        List<ProjectMemberResponse> members = projectMemberService.getProjectMembers(userKey, projectKey);
        return ApiResponse.onSuccess(members);
    }

    @Operation(
        summary = "프로젝트 멤버 추가",
        description = "프로젝트에 새 멤버를 추가합니다 (OWNER, ADMIN만 가능). " +
                     "여러 멤버를 한 번에 추가할 수 있으며, 일부 실패해도 나머지는 추가됩니다. " +
                     "응답에는 성공한 멤버와 실패한 멤버 정보가 모두 포함됩니다."
    )
    @PostMapping("/{projectKey}/members")
    public ApiResponse<AddProjectMembersResponse> addProjectMember(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        AddProjectMembersResponse response = projectMemberService.addProjectMembers(userKey, projectKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "프로젝트 멤버 역할 변경", description = "프로젝트 멤버의 역할을 변경합니다 (OWNER만 가능)")
    @PatchMapping("/{projectKey}/members/{memberKey}")
    public ApiResponse<ProjectMemberResponse> updateProjectMemberRole(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Parameter(description = "멤버 ID", required = true)
            @PathVariable @Min(value = 1, message = "멤버 ID는 1 이상이어야 합니다") Integer memberKey,
            @Valid @RequestBody UpdateProjectMemberRoleRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        ProjectMemberResponse response = projectMemberService.updateProjectMemberRole(
                userKey, projectKey, memberKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "프로젝트 오너 변경", description = "프로젝트 오너를 변경합니다 (OWNER만 가능)")
    @PatchMapping("/{projectKey}/owner/{memberKey}")
    public ApiResponse<?> changeOwner(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Parameter(description = "멤버 ID", required = true)
            @PathVariable @Min(value = 1, message = "멤버 ID는 1 이상이어야 합니다") Integer memberKey) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        return ApiResponse.onSuccess(projectMemberService.changeOwner(userKey,memberKey,projectKey));

    }
    @Operation(summary = "프로젝트 나가기", description = "현재 사용자가 프로젝트에서 나갑니다")
    @PostMapping("/{projectKey}/leave")
    public ApiResponse<Void> leaveProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectMemberService.leaveProject(userKey, projectKey);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "프로젝트 멤버 내보내기", description = "프로젝트에서 멤버를 내보냅니다 (OWNER, ADMIN만 가능)")
    @DeleteMapping("/{projectKey}/members/{memberKey}")
    public ApiResponse<Void> removeProjectMember(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Parameter(description = "멤버 ID", required = true)
            @PathVariable @Min(value = 1, message = "멤버 ID는 1 이상이어야 합니다") Integer memberKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectMemberService.removeProjectMember(userKey, projectKey, memberKey);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "프로젝트 멤버 이력 조회", description = "프로젝트의 멤버 변경 이력을 조회합니다 (페이징, 최신순)")
    @GetMapping("/{projectKey}/member-history")
    public ApiResponse<PageResponse<ProjectMemberHistoryResponse>> getProjectMemberHistory(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProjectMemberHistoryResponse> page = projectMemberService.getProjectMemberHistory(projectKey, pageable);
        return ApiResponse.onSuccess(PageResponse.of(page));
    }
}
