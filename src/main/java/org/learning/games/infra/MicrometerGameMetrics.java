package org.learning.games.infra;

import org.learning.games.domain.GameMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MicrometerGameMetrics implements GameMetrics {

	@Inject
	MeterRegistry meterRegistry;

	@Override
	public void recordGameStarted() {
		meterRegistry.counter("games.started").increment();
	}

	@Override
	public void recordMemberKicked() {
		meterRegistry.counter("members.kicked").increment();
	}

	@Override
	public void recordVoteCast() {
		meterRegistry.counter("votes.cast").increment();
	}

	@Override
	public void recordGameFinished() {
		meterRegistry.counter("games.finished").increment();
	}
}
