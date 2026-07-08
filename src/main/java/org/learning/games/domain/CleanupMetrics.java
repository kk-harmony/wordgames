package org.learning.games.domain;

public interface CleanupMetrics {

	interface RunTimer extends AutoCloseable {

		@Override
		void close();
	}

	RunTimer startRunTimer();

	void recordDeletedRows(String table, int count);

	void recordSkipped(String reason);
}
