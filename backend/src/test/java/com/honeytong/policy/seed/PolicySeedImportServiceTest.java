package com.honeytong.policy.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicySeedImportServiceTest {

    @Mock
    private SystemPolicyRepository systemPolicyRepository;

    private PolicySeedImportService importService;

    @BeforeEach
    void setUp() {
        importService = new PolicySeedImportService(systemPolicyRepository);
    }

    @Test
    void importRecords_insertsMissingPolicy() {
        PolicySeedRecord record = new PolicySeedRecord(
                "region",
                "change_cooldown_day",
                "7",
                PolicyValueType.INTEGER,
                "지역 변경 쿨다운 일수"
        );
        when(systemPolicyRepository.existsByPolicyGroupAndPolicyKey("region", "change_cooldown_day"))
                .thenReturn(false);

        PolicySeedImportResult result = importService.importRecords(List.of(record));

        assertThat(result.rowCount()).isEqualTo(1);
        assertThat(result.insertedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();

        ArgumentCaptor<SystemPolicy> policyCaptor = ArgumentCaptor.forClass(SystemPolicy.class);
        verify(systemPolicyRepository).save(policyCaptor.capture());
        assertThat(policyCaptor.getValue().getPolicyGroup()).isEqualTo("region");
        assertThat(policyCaptor.getValue().getPolicyKey()).isEqualTo("change_cooldown_day");
    }

    @Test
    void importRecords_skipsExistingPolicy() {
        PolicySeedRecord record = new PolicySeedRecord(
                "visit",
                "radius_meter",
                "70",
                PolicyValueType.INTEGER,
                "방문 인증 허용 반경 미터"
        );
        when(systemPolicyRepository.existsByPolicyGroupAndPolicyKey("visit", "radius_meter"))
                .thenReturn(true);

        PolicySeedImportResult result = importService.importRecords(List.of(record));

        assertThat(result.insertedCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        verify(systemPolicyRepository, never()).save(any(SystemPolicy.class));
    }
}
