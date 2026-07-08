package org.learning.games.infra;

import java.time.Instant;
import java.util.List;

import org.learning.games.domain.DataCleanupRepository;
import org.learning.games.domain.model.GameStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class JpaDataCleanupRepository implements DataCleanupRepository {

	@Inject
	EntityManager em;

	@Override
	public int countIdempotencyRecordsOlderThan(Instant cutoff) {
		Long count = em.createQuery(
				"SELECT COUNT(i) FROM IdempotencyRecord i WHERE i.createdAt < :cutoff",
				Long.class)
				.setParameter("cutoff", cutoff)
				.getSingleResult();
		return count.intValue();
	}

	@Override
	public int deleteIdempotencyRecordsOlderThan(Instant cutoff, int batchSize) {
		return em.createNativeQuery("""
				DELETE FROM idempotency_record
				WHERE id IN (
					SELECT id FROM idempotency_record
					WHERE created_at < :cutoff
					ORDER BY id
					LIMIT :batchSize
				)
				""")
				.setParameter("cutoff", cutoff)
				.setParameter("batchSize", batchSize)
				.executeUpdate();
	}

	@Override
	public int countEligibleGames(GameStatus status, Instant cutoff) {
		Long count = em.createQuery(
				"SELECT COUNT(g) FROM Game g WHERE g.status = :status AND g.createdAt < :cutoff",
				Long.class)
				.setParameter("status", status)
				.setParameter("cutoff", cutoff)
				.getSingleResult();
		return count.intValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> findEligibleGameIds(GameStatus status, Instant cutoff, int batchSize) {
		return em.createQuery(
				"SELECT g.id FROM Game g WHERE g.status = :status AND g.createdAt < :cutoff ORDER BY g.id",
				Long.class)
				.setParameter("status", status)
				.setParameter("cutoff", cutoff)
				.setMaxResults(batchSize)
				.getResultList();
	}

	@Override
	public int countIdempotencyByGameIds(List<Long> gameIds) {
		if (gameIds.isEmpty()) {
			return 0;
		}
		Long count = em.createQuery(
				"SELECT COUNT(i) FROM IdempotencyRecord i WHERE i.gameId IN :gameIds",
				Long.class)
				.setParameter("gameIds", gameIds)
				.getSingleResult();
		return count.intValue();
	}

	@Override
	public int countGameMembersByGameIds(List<Long> gameIds) {
		if (gameIds.isEmpty()) {
			return 0;
		}
		Long count = em.createQuery(
				"SELECT COUNT(m) FROM GameMember m WHERE m.game.id IN :gameIds",
				Long.class)
				.setParameter("gameIds", gameIds)
				.getSingleResult();
		return count.intValue();
	}

	@Override
	public int deleteIdempotencyByGameIds(List<Long> gameIds) {
		if (gameIds.isEmpty()) {
			return 0;
		}
		return em.createNativeQuery("DELETE FROM idempotency_record WHERE game_id IN (:gameIds)")
				.setParameter("gameIds", gameIds)
				.executeUpdate();
	}

	@Override
	public int deleteGameMembersByGameIds(List<Long> gameIds) {
		if (gameIds.isEmpty()) {
			return 0;
		}
		return em.createNativeQuery("DELETE FROM gamemember WHERE game_id IN (:gameIds)")
				.setParameter("gameIds", gameIds)
				.executeUpdate();
	}

	@Override
	public int deleteGamesByIds(List<Long> gameIds) {
		if (gameIds.isEmpty()) {
			return 0;
		}
		return em.createNativeQuery("DELETE FROM game WHERE id IN (:gameIds)")
				.setParameter("gameIds", gameIds)
				.executeUpdate();
	}
}
