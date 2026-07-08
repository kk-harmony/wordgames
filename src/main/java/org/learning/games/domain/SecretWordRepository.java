package org.learning.games.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.learning.games.entity.SecretWord;

public interface SecretWordRepository {
	
	void persist(SecretWord secretWord);
	Optional<SecretWord> findById(Long id);
	void delete(Long id);
	List<SecretWord> findAll();
	List<SecretWord> findAllExcludingIds(Collection<Long> ids);
}
