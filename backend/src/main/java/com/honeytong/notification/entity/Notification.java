package com.honeytong.notification.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import com.honeytong.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {
    }

    public Notification(User user, NotificationType type, String title, String content, Long targetId) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetId = targetId;
        this.isRead = false;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getTargetId() {
        return targetId;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }
}
