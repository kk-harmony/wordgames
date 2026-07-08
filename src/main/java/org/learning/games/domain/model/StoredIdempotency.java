package org.learning.games.domain.model;

public record StoredIdempotency(
		String operation,
		String requestHash,
		int responseStatus,
		String responseBody) {
}
