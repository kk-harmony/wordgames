package org.learning.games.domain;

import java.util.Optional;

import org.learning.games.domain.model.StoredIdempotency;

public interface IdempotencyReplayReader {

	Optional<StoredIdempotency> findRecord(String userId, String idempotencyKey);
}
