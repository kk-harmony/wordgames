package org.learning.games.domain;

import java.util.List;

import org.learning.games.domain.exception.ForbiddenException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.entity.SecretWord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SecretWordService {

	@Inject
	SecretWordRepository secretWordRepository;

	@Inject
	GameRepository gameRepository;

	@Inject
	GameRandom gameRandom;

	@Transactional
	public SecretWord create(String authentic, String imposed) {
		SecretWord secretWord = new SecretWord();
		secretWord.authentic = authentic;
		secretWord.imposed = imposed;
		secretWordRepository.persist(secretWord);
		return secretWord;
	}

	@Transactional
	public SecretWord getByIdForUser(Long id, String userId) {
		SecretWord secretWord = secretWordRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("SecretWord " + id + " not found"));

		if (!canAccessSecretWord(secretWord.id, userId)) {
			throw new ForbiddenException("Not authorized to view this secret word");
		}

		return secretWord;
	}

	@Transactional
	public SecretWord randomForUser(String userId) {
		List<Long> usedIds = gameRepository.findSecretWordIdsUsedByAdmin(userId);
		List<SecretWord> pool = secretWordRepository.findAllExcludingIds(usedIds);
		if (pool.isEmpty()) {
			pool = secretWordRepository.findAll();
		}
		if (pool.isEmpty()) {
			throw new NotFoundException("No secret words available");
		}
		return pool.get(gameRandom.nextInt(pool.size()));
	}

	private boolean canAccessSecretWord(Long secretWordId, String userId) {
		if (gameRepository.isSecretWordUsedByAdmin(secretWordId, userId)) {
			return true;
		}
		return isInSelectionPool(secretWordId, userId);
	}

	private boolean isInSelectionPool(Long secretWordId, String userId) {
		if (!gameRepository.hasWaitingGameAsAdmin(userId)) {
			return false;
		}
		List<Long> usedIds = gameRepository.findSecretWordIdsUsedByAdmin(userId);
		List<SecretWord> pool = secretWordRepository.findAllExcludingIds(usedIds);
		if (pool.isEmpty()) {
			pool = secretWordRepository.findAll();
		}
		return pool.stream().anyMatch(word -> word.id.equals(secretWordId));
	}
}
