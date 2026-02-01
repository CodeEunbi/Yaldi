package com.yaldi.domain.team.controller;

import com.yaldi.domain.team.dto.request.CreateTeamRequest;
import com.yaldi.domain.team.dto.request.InviteTeamMemberRequest;
import com.yaldi.domain.team.dto.request.UpdateTeamNameRequest;
import com.yaldi.domain.team.dto.request.UpdateTeamOwnerRequest;
import com.yaldi.domain.team.dto.response.*;
import com.yaldi.domain.team.service.TeamService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Team", description = "팀 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "팀 목록 조회", description = "사용자(me)가 속한 팀 목록을 조회합니다")
    @GetMapping
    public ApiResponse<List<TeamResponse>> getMyTeams() {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        List<TeamResponse> teams = teamService.getUserTeams(userKey);
        return ApiResponse.onSuccess(teams);
    }

    @Operation(summary = "오너인 팀 목록 조회", description = "사용자(me)가 오너인 팀 목록을 조회합니다")
    @GetMapping("/owned")
    public ApiResponse<List<TeamResponse>> getOwnedTeams() {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        List<TeamResponse> ownedTeams = teamService.getOwnedTeams(userKey);

        return ApiResponse.onSuccess(ownedTeams);
    }

    @Operation(summary = "팀 생성", description = "새로운 팀을 생성합니다. 생성자(팀오너)는 자동으로 팀 멤버로 추가됩니다")
    @PostMapping
    public ApiResponse<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        TeamResponse team = teamService.createTeam(userKey, request);

        return ApiResponse.onSuccess(team);
    }

    @Operation(summary = "팀 상세 정보 조회", description = "특정 팀의 상세 정보를 조회합니다")
    @GetMapping("/{teamKey}")
    public ApiResponse<TeamResponse> getTeamDetail(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        TeamResponse team = teamService.getTeamDetail(userKey, teamKey);

        return ApiResponse.onSuccess(team);
    }

    @Operation(summary = "팀 정보 수정", description = "팀의 정보를 수정합니다(팀명)")
    @PatchMapping("/{teamKey}")
    public ApiResponse<TeamResponse> updateTeamName(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey,
            @Valid @RequestBody UpdateTeamNameRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        TeamResponse team = teamService.updateTeamName(userKey, teamKey, request);

        return ApiResponse.onSuccess(team);
    }

    @Operation(summary = "팀 삭제", description = "팀을 삭제합니다(soft delete). 팀 오너만 삭제할 수 있습니다")
    @DeleteMapping("/{teamKey}")
    public ApiResponse<String> deleteTeam(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        teamService.deleteTeam(userKey, teamKey);

        return ApiResponse.onSuccess("팀 삭제가 완료되었습니다");
    }

    @Operation(summary = "팀 멤버 조회", description = "팀 멤버를 조회합니다")
    @GetMapping("/{teamKey}/members")
    public ApiResponse<List<TeamMemberResponse>> getTeamMembers(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        List<TeamMemberResponse> teamMembers = teamService.getTeamMembers(userKey, teamKey);

        return ApiResponse.onSuccess(teamMembers);
    }


    @Operation(summary = "팀 멤버 방출", description = "팀 멤버를 방출합니다(오너 권한)")
    @DeleteMapping("/{teamKey}/members/{targetUserKey}")
    public ApiResponse<String> expelTeamMember(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey,
            @Parameter(description = "방출할 유저 Key", example = "2")
            @PathVariable Integer targetUserKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        teamService.expelTeamMember(userKey, teamKey, targetUserKey);

        return ApiResponse.onSuccess("팀 멤버 방출이 완료되었습니다");
    }

    @Operation(summary = "팀 자진 탈퇴(나가기)", description = "팀에서 탈퇴합니다(자진)")
    @DeleteMapping("/{teamKey}/members/me")
    public ApiResponse<String> leaveTeam(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey
    ){
        Integer userKey = SecurityUtil.getCurrentUserKey();
        teamService.leaveTeam(userKey, teamKey);

        return ApiResponse.onSuccess("팀 나가기가 완료되었습니다");
    }

    @Operation(summary = "팀 오너 양도", description = "팀 오너 권한을 다른 멤버에게 양도합니다 (오너 권한 필요)")
    @PatchMapping("/{teamKey}/owner")
    public ApiResponse<TeamResponse> transferOwnership(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey,
            @Valid @RequestBody UpdateTeamOwnerRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        TeamResponse team = teamService.transferOwnership(userKey, teamKey, request.newOwnerUserKey());

        return ApiResponse.onSuccess(team);
    }

    @Operation(summary = "팀 초대용 사용자 검색", description = "닉네임 또는 이메일로 사용자를 검색합니다. 각 사용자의 초대 가능 여부가 함께 반환됩니다")
    @GetMapping("/{teamKey}/invite/search")
    public ApiResponse<List<UserSearchResult>> searchUsersForInvitation(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey,
            @Parameter(description = "검색 키워드 (닉네임 또는 이메일)", example = "yal")
            @RequestParam String keyword
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        List<UserSearchResult> results = teamService.searchUsersForInvitation(userKey, teamKey, keyword);

        return ApiResponse.onSuccess(results);
    }

    @Operation(summary = "팀 멤버 초대", description = "이메일 또는 닉네임으로 팀에 멤버를 초대합니다. 초대 이메일이 발송됩니다")
    @PostMapping("/{teamKey}/invite")
    public ApiResponse<TeamInvitationResponse> inviteTeamMember(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey,
            @Valid @RequestBody InviteTeamMemberRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        TeamInvitationResponse invitation = teamService.inviteTeamMember(userKey, teamKey, request);

        return ApiResponse.onSuccess(invitation);
    }

    @Operation(summary = "팀 초대 수락", description = "받은 팀 초대를 수락하고 팀 멤버로 추가됩니다")
    @PostMapping("/invitations/{invitationKey}/accept")
    public ApiResponse<InvitationActionResponse> acceptInvitation(
            @Parameter(description = "초대 Key", example = "1")
            @PathVariable Long invitationKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        InvitationActionResponse response = teamService.acceptInvitation(userKey, invitationKey);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "팀 초대 거절", description = "받은 팀 초대를 거절합니다")
    @PostMapping("/invitations/{invitationKey}/reject")
    public ApiResponse<InvitationActionResponse> rejectInvitation(
            @Parameter(description = "초대 Key", example = "1")
            @PathVariable Long invitationKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        InvitationActionResponse response = teamService.rejectInvitation(userKey, invitationKey);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "팀 초대 취소", description = "보낸 팀 초대를 취소합니다. 초대한 사람 또는 팀 오너만 가능합니다")
    @DeleteMapping("/invitations/{invitationKey}")
    public ApiResponse<String> cancelInvitation(
            @Parameter(description = "초대 Key", example = "1")
            @PathVariable Long invitationKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        teamService.cancelInvitation(userKey, invitationKey);

        return ApiResponse.onSuccess("팀 초대가 취소되었습니다");
    }

    @Operation(summary = "팀 초대 상태인 유저 목록 조회", description = "팀 초대를 보냈지만 아직 수락하지 않은 대기 중인 초대 목록을 조회합니다. 팀 오너만 조회 가능합니다")
    @GetMapping("/{teamKey}/invitations/pending")
    public ApiResponse<PendingInvitationListResponse> getPendingInvitations(
            @Parameter(description = "팀 Key", example = "1")
            @PathVariable Integer teamKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        PendingInvitationListResponse response = teamService.getPendingInvitations(userKey, teamKey);

        return ApiResponse.onSuccess(response);
    }
}
