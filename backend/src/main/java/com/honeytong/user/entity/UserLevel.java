package com.honeytong.user.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "user_level")
public class UserLevel extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int exp;

    @Column(name = "total_exp", nullable = false)
    private int totalExp;

    @Column(length = 50)
    private String title;

    @Column(name = "avatar_stage", length = 50)
    private String avatarStage;

    @Column(name = "rank_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal rankScore;

    protected UserLevel() {
    }

    public UserLevel(User user) {
        this.user = user;
        this.level = 1;
        this.exp = 0;
        this.totalExp = 0;
        this.rankScore = BigDecimal.ZERO;
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public String getTitle() {
        return title;
    }

    public String getAvatarStage() {
        return avatarStage;
    }
}
