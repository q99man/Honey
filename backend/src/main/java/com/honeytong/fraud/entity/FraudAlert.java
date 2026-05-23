package com.honeytong.fraud.entity;

import com.honeytong.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private FraudAlertType alertType;

    @Column(name = "risk_score", nullable = false)
    private double riskScore;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FraudAlert() {
    }

    public FraudAlert(User user, FraudAlertType alertType, double riskScore, String description, String ipAddress, Long targetId) {
        this.user = user;
        this.alertType = alertType;
        this.riskScore = riskScore;
        this.description = description;
        this.ipAddress = ipAddress;
        this.targetId = targetId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public FraudAlertType getAlertType() {
        return alertType;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getDescription() {
        return description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getTargetId() {
        return targetId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
