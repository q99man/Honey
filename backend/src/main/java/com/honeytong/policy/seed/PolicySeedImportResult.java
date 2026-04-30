package com.honeytong.policy.seed;

public record PolicySeedImportResult(
        int rowCount,
        int insertedCount,
        int skippedCount
) {
}
