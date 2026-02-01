package com.yaldi.domain.comment.service;

import com.yaldi.domain.comment.dto.response.CommentResponse;
import com.yaldi.domain.comment.entity.Comment;
import com.yaldi.domain.comment.repository.CommentRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserTeamRelationRepository userTeamRelationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Comment createCommentWithTable(
            Integer userKey,
            Integer teamKey,
            Long projectKey,
            Long tableKey,
            String content,
            String colorHex
    ) {
        boolean isBelong = userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(userKey, teamKey);
        if (!isBelong) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }

        Comment comment = Comment.builder()
                .userKey(userKey)
                .tableKey(tableKey)
                .projectKey(projectKey)
                .content(content)
                .colorHex(colorHex)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("댓글 생성: comment={}, project={}, table={}", saved.getCommentKey(), projectKey, tableKey);
        return saved;
    }

    @Transactional
    public Comment createCommentWithoutTable(
            Integer userKey,
            Integer teamKey,
            Long projectKey,
            String content,
            String colorHex,
            BigDecimal xPosition,
            BigDecimal yPosition
    ) {
        boolean isBelong = userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(userKey, teamKey);
        if (!isBelong) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }

        Comment comment = Comment.builder()
                .userKey(userKey)
                .projectKey(projectKey)
                .content(content)
                .colorHex(colorHex)
                .xPosition(xPosition)
                .yPosition(yPosition)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("댓글 생성: comment={}, project={}, x={}, y={}", saved.getCommentKey(), projectKey, xPosition, yPosition);
        return saved;
    }

    @Transactional
    public Comment deleteComment(Integer userKey, Long commentKey) {
        Comment comment = commentRepository.findById(commentKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));

        if (!comment.getUserKey().equals(userKey)) {
            throw new GeneralException(ErrorStatus.COMMENT_FORBIDDEN);
        }

        comment.softDelete();
        log.info("댓글 삭제: comment={}, user={}", commentKey, userKey);

        return comment;
    }

    @Transactional
    public Comment resolveComment(Long commentKey) {
        Comment comment = commentRepository.findById(commentKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));

        comment.resolve();
        return comment;
    }

    @Transactional
    public Comment unResolveComment(Long commentKey) {
        Comment comment = commentRepository.findById(commentKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));

        comment.unresolve();
        return comment;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByProject(Long projectKey) {
        List<Comment> comments = commentRepository.findAllByProjectKeyAndDeletedAtIsNullAndIsResolvedFalse(projectKey);

        if (comments.isEmpty()) {
            throw new GeneralException(ErrorStatus.COMMENT_NOT_FOUND);
        }

        return comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }
}
