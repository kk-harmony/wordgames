package org.learning.games.infra;

import java.util.Optional;

import org.learning.games.domain.IdempotencyReplayReader;
import org.learning.games.domain.IdempotencyRepository;
import org.learning.games.domain.model.StoredIdempotency;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class JpaIdempotencyReplayReader implements IdempotencyReplayReader {

	@Inject
	IdempotencyRepository repository;

	@Override
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public Optional<StoredIdempotency> findRecord(String userId, String idempotencyKey) {
		return repository.findByUserAndKey(userId, idempotencyKey);
	}
}
