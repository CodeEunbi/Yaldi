package com.yaldi.domain.comment.service;

import com.yaldi.domain.comment.dto.event.ReplyCreatedEvent;
import com.yaldi.domain.comment.dto.event.ReplyDeletedEvent;
import com.yaldi.domain.comment.dto.response.CreateReplyResponse;
import com.yaldi.domain.comment.dto.response.ReplyResponse;
import com.yaldi.domain.comment.entity.Comment;
import com.yaldi.domain.comment.entity.Reply;
import com.yaldi.domain.comment.repository.CommentRepository;
import com.yaldi.domain.comment.repository.ReplyRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final CommentRepository commentRepository;
    private final UserTeamRelationRepository userTeamRelationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 댓글에 대한 대댓글 생성
     */
    public CreateReplyResponse createReply(
            Integer userKey,
            Integer teamKey,
            Long commentKey,
            String content
    ) {
        boolean isBelong = userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(userKey, teamKey);
        if (!isBelong) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }

        Comment parentComment = commentRepository.findById(commentKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));

        Reply reply = Reply.builder()
                .commentKey(parentComment.getCommentKey())
                .userKey(userKey)
                .content(content)
                .build();

        Reply savedReply = replyRepository.save(reply);

        log.info("대댓글 생성:: replyKey={}, commentKey={}, 작성자={}, 내용={}",
                savedReply.getReplyKey(), savedReply.getCommentKey(), savedReply.getUserKey(), savedReply.getContent());

        
        publishReplyCreatedEvent(parentComment, savedReply);

        return CreateReplyResponse.from(savedReply);
    }

    /**
     * 대댓글 삭제 (작성자 본인만 가능)
     */
    @Transactional
    public void deleteReply(Integer userKey, Long replyKey) {
        Reply reply = replyRepository.findById(replyKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REPLY_NOT_FOUND));

        if (!reply.getUserKey().equals(userKey)) {
            throw new GeneralException(ErrorStatus.REPLY_FORBIDDEN);
        }

        reply.softDelete();
        log.info("대댓글 삭제:: replyKey={}, 작성자={}", replyKey, userKey);

        
        Comment parentComment = commentRepository.findById(reply.getCommentKey())
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));
        publishReplyDeletedEvent(parentComment, reply);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponse> getRepliesByComment(Long commentKey) {
        List<Reply> replies = replyRepository.findAllByCommentKeyAndDeletedAtIsNull(commentKey);

        if (replies.isEmpty()) {
            throw new GeneralException(ErrorStatus.REPLY_NOT_FOUND);
        }

        return replies.stream()
                .map(ReplyResponse::from)
                .collect(Collectors.toList());
    }


    private void publishReplyCreatedEvent(Comment parentComment, Reply reply) {
        User user = userRepository.findById(reply.getUserKey()).orElse(null);

        ReplyCreatedEvent event = ReplyCreatedEvent.builder()
                .replyKey(reply.getReplyKey())
                .commentKey(reply.getCommentKey())
                .projectKey(parentComment.getProjectKey())
                .userKey(reply.getUserKey())
                .userName(user != null ? user.getNickname() : null)
                .content(reply.getContent())
                .build();

        ErdBroadcastEvent broadcastEvent = ErdBroadcastEvent.builder()
                .projectKey(parentComment.getProjectKey())
                .userKey(reply.getUserKey())
                .event(event)
                .build();

        messagingTemplate.convertAndSend("/topic/project/" + parentComment.getProjectKey(), broadcastEvent);
    }

    private void publishReplyDeletedEvent(Comment parentComment, Reply reply) {
        ReplyDeletedEvent event = ReplyDeletedEvent.builder()
                .replyKey(reply.getReplyKey())
                .commentKey(reply.getCommentKey())
                .projectKey(parentComment.getProjectKey())
                .build();

        ErdBroadcastEvent broadcastEvent = ErdBroadcastEvent.builder()
                .projectKey(parentComment.getProjectKey())
                .userKey(reply.getUserKey())
                .event(event)
                .build();

        messagingTemplate.convertAndSend("/topic/project/" + parentComment.getProjectKey(), broadcastEvent);
    }
}
