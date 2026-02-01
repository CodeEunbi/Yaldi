package com.yaldi.domain.comment.repository;

import com.yaldi.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 사용자의 댓글 목록 조회
     */
    List<Comment> findByUserKey(Integer userKey);

    /**
     * 테이블의 댓글 목록 조회
     */
    List<Comment> findByTableKey(Long tableKey);

    /**
     * 테이블의 해결되지 않은 댓글 목록 조회
     */
    List<Comment> findByTableKeyAndIsResolvedFalse(Long tableKey);

    /**
     * 삭제된 댓글 포함 전체 조회
     */
    @Query(value = "SELECT * FROM comments", nativeQuery = true)
    List<Comment> findAllIncludingDeleted();

    List<Comment> findAllByProjectKeyAndDeletedAtIsNullAndIsResolvedFalse(Long projectKey);
    /**
     * 삭제된 댓글 포함 ID로 조회
     */
    @Query(value = "SELECT * FROM comments WHERE comment_key = :commentKey", nativeQuery = true)
    Optional<Comment> findByIdIncludingDeleted(Long commentKey);

    /**
     * 활성 댓글 조회 (삭제되지 않은)
     */
    @Query("SELECT c FROM Comment c WHERE c.commentKey = :commentKey AND c.deletedAt IS NULL")
    Optional<Comment> findActiveCommentById(Long commentKey);
}
