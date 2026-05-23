package com.honeytong.comment.repository;

import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.entity.CommentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByUserIdAndPlaceId(Long userId, Long placeId);

    long countByUserIdAndStatusAndDeletedAtIsNull(Long userId, CommentStatus status);

    List<Comment> findTop50ByOrderByCreatedAtDesc();

    List<Comment> findByPlaceIdAndStatusOrderByCreatedAtDesc(Long placeId, CommentStatus status);

    List<Comment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CommentStatus status);

    long countByPlaceIdAndStatusAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(Long placeId, CommentStatus status, java.time.LocalDateTime since);
}
