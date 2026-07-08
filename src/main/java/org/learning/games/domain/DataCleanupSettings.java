package org.learning.games.domain;

public interface DataCleanupSettings {

	boolean isDryRun();

	int getBatchSize();

	int getMaxBatchesPerRun();

	long getInterBatchSleepMs();

	int getIdempotencyRetentionDays();

	int getWaitingRetentionDays();

	int getFinishedRetentionDays();
}
