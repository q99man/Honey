package com.honeytong.policy.entity;

import com.honeytong.common.entity.BaseTimeEntity;
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

@Entity
@Table(
        name = "system_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_system_policies_group_key",
                columnNames = {"policy_group", "policy_key"}
        )
)
public class SystemPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_group", nullable = false, length = 50)
    private String policyGroup;

    @Column(name = "policy_key", nullable = false, length = 100)
    private String policyKey;

    @Column(name = "policy_value", nullable = false, length = 255)
    private String policyValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private PolicyValueType valueType;

    @Column(length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    protected SystemPolicy() {
    }

    public SystemPolicy(
            String policyGroup,
            String policyKey,
            String policyValue,
            PolicyValueType valueType,
            String description
    ) {
        this.policyGroup = policyGroup;
        this.policyKey = policyKey;
        this.policyValue = policyValue;
        this.valueType = valueType;
        this.description = description;
        this.active = true;
    }

    public String getPolicyValue() {
        return policyValue;
    }

    public PolicyValueType getValueType() {
        return valueType;
    }

    public boolean isActive() {
        return active;
    }
}
