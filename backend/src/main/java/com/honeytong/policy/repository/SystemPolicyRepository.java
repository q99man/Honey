package com.honeytong.policy.repository;

import com.honeytong.policy.entity.SystemPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPolicyRepository extends JpaRepository<SystemPolicy, Long> {

    Optional<SystemPolicy> findByPolicyGroupAndPolicyKeyAndActiveTrue(String policyGroup, String policyKey);

    Optional<SystemPolicy> findByPolicyGroupAndPolicyKey(String policyGroup, String policyKey);

    boolean existsByPolicyGroupAndPolicyKey(String policyGroup, String policyKey);

    List<SystemPolicy> findAllByOrderByPolicyGroupAscPolicyKeyAsc();
}
