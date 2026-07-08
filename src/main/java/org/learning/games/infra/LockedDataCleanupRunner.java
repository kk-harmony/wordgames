package org.learning.games.infra;

import java.util.logging.Logger;

import org.learning.games.domain.CleanupMetrics;
import org.learning.games.domain.DataCleanupService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LockedDataCleanupRunner {

	private static final Logger LOG = Logger.getLogger(LockedDataCleanupRunner.class.getName());
	private static final long CLEANUP_ADVISORY_LOCK_KEY = 867530901L;

	@Inject
	AdvisoryLockService advisoryLockService;

	@Inject
	DataCleanupService cleanupService;

	@Inject
	CleanupMetrics cleanupMetrics;

	@Transactional
	public void run() {
		if (!advisoryLockService.tryLock(CLEANUP_ADVISORY_LOCK_KEY)) {
			cleanupMetrics.recordSkipped("lock_not_acquired");
			LOG.info("cleanup.skipped reason=lock_not_acquired");
			return;
		}

		try {
			cleanupService.run();
		} finally {
			advisoryLockService.unlock(CLEANUP_ADVISORY_LOCK_KEY);
		}
	}
}
