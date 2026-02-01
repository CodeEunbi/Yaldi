package com.yaldi.domain.comment.repository;

import com.yaldi.domain.comment.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Long> {

    /**
     * 댓글의 답글 목록 조회
     */
    List<Reply> findByCommentKey(Long commentKey);

    /**
     * 사용자의 답글 목록 조회
     */
    List<Reply> findByUserKey(Integer userKey);

    List<Reply> findAllByCommentKeyAndDeletedAtIsNull(Long commentKey);
    /**
     * 삭제된 답글 포함 전체 조회
     */
    @Query(value = "SELECT * FROM replies", nativeQuery = true)
    List<Reply> findAllIncludingDeleted();

    /**
     * 삭제된 답글 포함 ID로 조회
     */
    @Query(value = "SELECT * FROM replies WHERE reply_key = :replyKey", nativeQuery = true)
    Optional<Reply> findByIdIncludingDeleted(Long replyKey);

    /**
     * 활성 답글 조회 (삭제되지 않은)
     */
    @Query("SELECT r FROM Reply r WHERE r.replyKey = :replyKey AND r.deletedAt IS NULL")
    Optional<Reply> findActiveReplyById(Long replyKey);

    /**
     * 댓글의 답글 수 조회
     */
    long countByCommentKey(Long commentKey);
}
