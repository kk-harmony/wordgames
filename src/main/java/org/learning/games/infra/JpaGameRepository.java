package org.learning.games.infra;

import java.util.List;
import java.util.Optional;

import org.learning.games.domain.GameRepository;
import org.learning.games.entity.Game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class JpaGameRepository implements GameRepository {

	@Inject
	EntityManager em;

	@Override
	public void persist(Game game) {
		em.persist(game);
	}

	@Override
	public Optional<Game> findById(Long id) {
		return Optional.ofNullable(em.find(Game.class, id));
	}

	@Override
	public Optional<Game> findByIdWithMembers(Long id) {
		em.flush();
		em.clear();
		return em.createQuery(
				"SELECT g FROM Game g LEFT JOIN FETCH g.members WHERE g.id = :id",
				Game.class)
				.setParameter("id", id)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<Long> findSecretWordIdsUsedByAdmin(String adminUserId) {
		return em.createQuery(
				"SELECT DISTINCT g.secretWord.id FROM Game g "
						+ "WHERE g.adminUserId = :adminUserId AND g.secretWord IS NOT NULL",
				Long.class)
				.setParameter("adminUserId", adminUserId)
				.getResultList();
	}

	@Override
	public boolean hasWaitingGameAsAdmin(String adminUserId) {
		return em.createQuery(
				"SELECT COUNT(g) FROM Game g WHERE g.adminUserId = :adminUserId AND g.status = :status",
				Long.class)
				.setParameter("adminUserId", adminUserId)
				.setParameter("status", org.learning.games.domain.model.GameStatus.WAITING)
				.getSingleResult() > 0;
	}

	@Override
	public boolean isSecretWordUsedByAdmin(Long secretWordId, String adminUserId) {
		return em.createQuery(
				"SELECT COUNT(g) FROM Game g WHERE g.adminUserId = :adminUserId AND g.secretWord.id = :secretWordId",
				Long.class)
				.setParameter("adminUserId", adminUserId)
				.setParameter("secretWordId", secretWordId)
				.getSingleResult() > 0;
	}
}
