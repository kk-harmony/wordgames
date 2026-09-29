package org.learning.games.infra;

import java.util.List;
import java.util.Optional;

import org.learning.games.domain.GameSessionRepository;
import org.learning.games.entity.GameSession;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class JpaGameSessionRepository implements GameSessionRepository {

	@Inject
	EntityManager em;

	@Override
	public void persist(GameSession session) {
		em.persist(session);
	}

	@Override
	public Optional<GameSession> findById(Long id) {
		return Optional.ofNullable(em.find(GameSession.class, id));
	}

	@Override
	public Optional<GameSession> findByJoinCode(String joinCode) {
		return em.createQuery(
				"SELECT s FROM GameSession s WHERE s.joinCode = :joinCode",
				GameSession.class)
				.setParameter("joinCode", joinCode)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<GameSession> findByIdWithMembers(Long id) {
		em.flush();
		em.clear();
		List<GameSession> sessions = em.createQuery(
				"SELECT s FROM GameSession s LEFT JOIN FETCH s.members WHERE s.id = :id",
				GameSession.class)
				.setParameter("id", id)
				.getResultList();
		if (sessions.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(sessions.get(0));
	}

	@Override
	public Optional<GameSession> findByJoinCodeWithMembers(String joinCode) {
		em.flush();
		em.clear();
		List<GameSession> sessions = em.createQuery(
				"SELECT s FROM GameSession s LEFT JOIN FETCH s.members WHERE s.joinCode = :joinCode",
				GameSession.class)
				.setParameter("joinCode", joinCode)
				.getResultList();
		if (sessions.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(sessions.get(0));
	}

	@Override
	public Optional<GameSession> findByCurrentGameId(Long gameId) {
		return em.createQuery(
				"SELECT s FROM GameSession s WHERE s.currentGameId = :gameId",
				GameSession.class)
				.setParameter("gameId", gameId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public boolean existsByJoinCode(String joinCode) {
		Long count = em.createQuery(
				"SELECT COUNT(s) FROM GameSession s WHERE s.joinCode = :joinCode",
				Long.class)
				.setParameter("joinCode", joinCode)
				.getSingleResult();
		return count > 0;
	}
}
