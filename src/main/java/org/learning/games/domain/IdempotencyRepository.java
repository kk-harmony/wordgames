package org.learning.games.domain;

import java.util.Optional;

import org.learning.games.domain.model.StoredIdempotency;

public interface IdempotencyRepository {

	Optional<StoredIdempotency> findByUserAndKey(String userId, String idempotencyKey);
}
