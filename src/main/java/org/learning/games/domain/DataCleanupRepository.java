package org.learning.games.domain;

import java.time.Instant;
import java.util.List;

import org.learning.games.domain.model.GameStatus;

public interface DataCleanupRepository {

	int countIdempotencyRecordsOlderThan(Instant cutoff);

	int deleteIdempotencyRecordsOlderThan(Instant cutoff, int batchSize);

	int countEligibleGames(GameStatus status, Instant cutoff);

	List<Long> findEligibleGameIds(GameStatus status, Instant cutoff, int batchSize);

	int countIdempotencyByGameIds(List<Long> gameIds);

	int countGameMembersByGameIds(List<Long> gameIds);

	int deleteIdempotencyByGameIds(List<Long> gameIds);

	int deleteGameMembersByGameIds(List<Long> gameIds);

	int deleteGamesByIds(List<Long> gameIds);
}
