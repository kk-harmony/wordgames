package org.learning.games.infra;

import org.learning.games.domain.SecretWordRepository;
import org.learning.games.entity.SecretWord;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class JpaSecretWordRepository implements SecretWordRepository {

	@Inject
	EntityManager em;

	@Override
	public void persist(SecretWord secretWord) {
		em.persist(secretWord);
	}

	@Override
	public Optional<SecretWord> findById(Long id) {
		return Optional.ofNullable(em.find(SecretWord.class, id));
	}

	@Override
	public void delete(Long id) {
		findById(id).ifPresent(sw -> em.remove(sw));
	}

	@Override
	public List<SecretWord> findAll() {
		return em.createQuery("SELECT s FROM SecretWord s ORDER BY s.id", SecretWord.class)
				.getResultList();
	}

	@Override
	public List<SecretWord> findAllExcludingIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return findAll();
		}
		return em.createQuery(
				"SELECT s FROM SecretWord s WHERE s.id NOT IN :ids ORDER BY s.id",
				SecretWord.class)
				.setParameter("ids", ids)
				.getResultList();
	}
}
