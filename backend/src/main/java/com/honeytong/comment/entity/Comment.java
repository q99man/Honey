package com.honeytong.comment.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import com.honeytong.place.entity.Place;
import com.honeytong.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "comments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comments_user_place",
                columnNames = {"user_id", "place_id"}
        )
)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(nullable = false, length = 300)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Comment() {
    }

    public Comment(User user, Place place, String content) {
        this.user = user;
        this.place = place;
        this.content = content;
        this.status = CommentStatus.VISIBLE;
        this.reportCount = 0;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Place getPlace() {
        return place;
    }

    public String getContent() {
        return content;
    }

    public CommentStatus getStatus() {
        return status;
    }

    public int getReportCount() {
        return reportCount;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isVisible() {
        return status == CommentStatus.VISIBLE && deletedAt == null;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void restore(String content) {
        this.content = content;
        this.status = CommentStatus.VISIBLE;
        this.deletedAt = null;
    }

    public void blind() {
        this.status = CommentStatus.BLINDED;
    }

    public void delete() {
        this.status = CommentStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
