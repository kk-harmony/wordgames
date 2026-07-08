package org.learning.games.infra;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class AdvisoryLockService {

	@Inject
	EntityManager em;

	public boolean tryLock(long lockKey) {
		Object result = em.createNativeQuery("SELECT pg_try_advisory_lock(:lockKey)")
				.setParameter("lockKey", lockKey)
				.getSingleResult();
		return Boolean.TRUE.equals(result);
	}

	public void unlock(long lockKey) {
		em.createNativeQuery("SELECT pg_advisory_unlock(:lockKey)")
				.setParameter("lockKey", lockKey)
				.getSingleResult();
	}
}
