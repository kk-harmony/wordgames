package org.learning.games.infra;

import java.util.List;
import java.util.Optional;

import org.learning.games.domain.SessionMemberRepository;
import org.learning.games.entity.SessionMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class JpaSessionMemberRepository implements SessionMemberRepository {

	@Inject
	EntityManager em;

	@Override
	public void persist(SessionMember member) {
		em.persist(member);
	}

	@Override
	public void delete(SessionMember member) {
		em.remove(member);
	}

	@Override
	public Optional<SessionMember> findBySessionAndUser(Long sessionId, String userId) {
		return em.createQuery(
				"SELECT m FROM SessionMember m WHERE m.session.id = :sessionId AND m.userId = :userId",
				SessionMember.class)
				.setParameter("sessionId", sessionId)
				.setParameter("userId", userId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<SessionMember> findBySession(Long sessionId) {
		return em.createQuery(
				"SELECT m FROM SessionMember m WHERE m.session.id = :sessionId ORDER BY m.id",
				SessionMember.class)
				.setParameter("sessionId", sessionId)
				.getResultList();
	}
}
