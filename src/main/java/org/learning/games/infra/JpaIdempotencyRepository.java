package org.learning.games.infra;

import java.util.Optional;

import org.learning.games.domain.IdempotencyRepository;
import org.learning.games.domain.model.StoredIdempotency;
import org.learning.games.entity.IdempotencyRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class JpaIdempotencyRepository implements IdempotencyRepository {

	@Inject
	EntityManager em;

	@Override
	public Optional<StoredIdempotency> findByUserAndKey(String userId, String idempotencyKey) {
		return em.createQuery(
				"SELECT r FROM IdempotencyRecord r WHERE r.userId = :userId AND r.idempotencyKey = :key",
				IdempotencyRecord.class)
				.setParameter("userId", userId)
				.setParameter("key", idempotencyKey)
				.getResultStream()
				.findFirst()
				.map(this::toStored);
	}

	public void persist(IdempotencyRecord record) {
		em.persist(record);
		em.flush();
	}

	private StoredIdempotency toStored(IdempotencyRecord record) {
		return new StoredIdempotency(
				record.operation,
				record.requestHash,
				record.responseStatus,
				record.responseBody);
	}
}
