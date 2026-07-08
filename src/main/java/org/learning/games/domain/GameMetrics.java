package org.learning.games.domain;

public interface GameMetrics {

	void recordGameStarted();

	void recordMemberKicked();

	void recordVoteCast();

	void recordGameFinished();
}
