package org.learning.games.infra;

import org.learning.games.domain.DataCleanupSettings;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DataCleanupConfig implements DataCleanupSettings {

	@ConfigProperty(name = "app.cleanup.enabled", defaultValue = "true")
	boolean enabled;

	@ConfigProperty(name = "app.cleanup.dry-run", defaultValue = "false")
	boolean dryRun;

	@ConfigProperty(name = "app.cleanup.batch-size", defaultValue = "500")
	int batchSize;

	@ConfigProperty(name = "app.cleanup.max-batches-per-run", defaultValue = "100")
	int maxBatchesPerRun;

	@ConfigProperty(name = "app.cleanup.inter-batch-sleep-ms", defaultValue = "50")
	long interBatchSleepMs;

	@ConfigProperty(name = "app.cleanup.idempotency.retention-days", defaultValue = "15")
	int idempotencyRetentionDays;

	@ConfigProperty(name = "app.cleanup.game.waiting-retention-days", defaultValue = "7")
	int waitingRetentionDays;

	@ConfigProperty(name = "app.cleanup.game.finished-retention-days", defaultValue = "30")
	int finishedRetentionDays;

	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isDryRun() {
		return dryRun;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public int getMaxBatchesPerRun() {
		return maxBatchesPerRun;
	}

	@Override
	public long getInterBatchSleepMs() {
		return interBatchSleepMs;
	}

	@Override
	public int getIdempotencyRetentionDays() {
		return idempotencyRetentionDays;
	}

	@Override
	public int getWaitingRetentionDays() {
		return waitingRetentionDays;
	}

	@Override
	public int getFinishedRetentionDays() {
		return finishedRetentionDays;
	}
}
