package com.honeytong.community.repository;

import com.honeytong.community.entity.CommunityPost;
import com.honeytong.community.entity.CommunityPostStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findByStatusOrderByCreatedAtDesc(CommunityPostStatus status);

    List<CommunityPost> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CommunityPostStatus status);
}
