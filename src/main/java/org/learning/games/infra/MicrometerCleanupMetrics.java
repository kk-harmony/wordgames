package org.learning.games.infra;

import org.learning.games.domain.CleanupMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MicrometerCleanupMetrics implements CleanupMetrics {

	@Inject
	MeterRegistry meterRegistry;

	@Override
	public RunTimer startRunTimer() {
		Timer.Sample sample = Timer.start(meterRegistry);
		return () -> sample.stop(meterRegistry.timer("cleanup.run.duration"));
	}

	@Override
	public void recordDeletedRows(String table, int count) {
		if (count > 0) {
			meterRegistry.counter("cleanup.rows.deleted", "table", table).increment(count);
		}
	}

	@Override
	public void recordSkipped(String reason) {
		meterRegistry.counter("cleanup.skipped", "reason", reason).increment();
	}
}
