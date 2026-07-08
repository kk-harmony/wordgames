package org.learning.games.domain.model;

public record CleanupResult(
		int idempotencyRecordsDeleted,
		int gamesDeleted,
		int gameMembersDeleted,
		int gameIdempotencyRecordsDeleted,
		boolean dryRun) {

	public CleanupResult add(CleanupResult other) {
		return new CleanupResult(
				idempotencyRecordsDeleted + other.idempotencyRecordsDeleted,
				gamesDeleted + other.gamesDeleted,
				gameMembersDeleted + other.gameMembersDeleted,
				gameIdempotencyRecordsDeleted + other.gameIdempotencyRecordsDeleted,
				dryRun);
	}

	public int totalRowsAffected() {
		return idempotencyRecordsDeleted
				+ gamesDeleted
				+ gameMembersDeleted
				+ gameIdempotencyRecordsDeleted;
	}
}
