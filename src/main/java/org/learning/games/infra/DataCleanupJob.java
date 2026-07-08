package org.learning.games.infra;

import org.learning.games.infra.DataCleanupConfig;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataCleanupJob {

	@Inject
	DataCleanupConfig config;

	@Inject
	LockedDataCleanupRunner lockedDataCleanupRunner;

	@Scheduled(cron = "{app.cleanup.cron}", concurrentExecution = ConcurrentExecution.SKIP)
	void runScheduledCleanup() {
		if (!config.isEnabled()) {
			return;
		}
		lockedDataCleanupRunner.run();
	}
}
