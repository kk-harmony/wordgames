package org.learning.games.infra;

import java.util.List;
import java.util.Optional;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class JpaGameMemberRepository implements GameMemberRepository {

	@Inject
	EntityManager em;

	@Override
	public void persist(GameMember member) {
		em.persist(member);
	}

	@Override
	public void delete(GameMember member) {
		em.remove(member);
	}

	@Override
	public Optional<GameMember> findByGameAndUser(Long gameId, String userId) {
		return em.createQuery(
				"SELECT m FROM GameMember m WHERE m.game.id = :gameId AND m.userId = :userId",
				GameMember.class)
				.setParameter("gameId", gameId)
				.setParameter("userId", userId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<GameMember> findByGame(Long gameId) {
		return em.createQuery(
				"SELECT m FROM GameMember m WHERE m.game.id = :gameId ORDER BY m.id",
				GameMember.class)
				.setParameter("gameId", gameId)
				.getResultList();
	}
}
