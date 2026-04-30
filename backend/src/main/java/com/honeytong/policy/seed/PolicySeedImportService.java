package com.honeytong.policy.seed;

import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicySeedImportService {

    private final SystemPolicyRepository systemPolicyRepository;

    public PolicySeedImportService(SystemPolicyRepository systemPolicyRepository) {
        this.systemPolicyRepository = systemPolicyRepository;
    }

    @Transactional
    public PolicySeedImportResult importRecords(List<PolicySeedRecord> records) {
        int insertedCount = 0;
        int skippedCount = 0;

        for (PolicySeedRecord record : records) {
            if (systemPolicyRepository.existsByPolicyGroupAndPolicyKey(record.policyGroup(), record.policyKey())) {
                skippedCount++;
                continue;
            }
            systemPolicyRepository.save(new SystemPolicy(
                    record.policyGroup(),
                    record.policyKey(),
                    record.policyValue(),
                    record.valueType(),
                    record.description()
            ));
            insertedCount++;
        }

        return new PolicySeedImportResult(records.size(), insertedCount, skippedCount);
    }
}
