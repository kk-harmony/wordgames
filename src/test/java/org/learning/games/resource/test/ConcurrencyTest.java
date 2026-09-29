package org.learning.games.resource.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.learning.games.domain.GameLifecycleService;
import org.learning.games.domain.GameService;
import org.learning.games.domain.SecretWordRepository;
import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.entity.Game;
import org.learning.games.entity.SecretWord;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ConcurrencyTest {

	/**
	 * Domain-rule concurrency: duplicate turn without idempotency key is rejected as BAD_REQUEST.
	 * Optimistic-lock conflicts (409 CONFLICT) are covered by {@link org.learning.games.domain.GameServiceConcurrencyTest}.
	 */

	@Inject
	GameLifecycleService lifecycleService;

	@Inject
	GameService gameService;

	@Inject
	SecretWordRepository secretWordRepository;

	@Test
	void secondTurnCompleteWithoutKeyReturnsBadRequest() {
		long gameId = QuarkusTransaction.requiringNew().call(() -> {
			SecretWord secretWord = new SecretWord();
			secretWord.authentic = "lime";
			secretWord.imposed = "lemon";
			secretWordRepository.persist(secretWord);

			long id = lifecycleService.createGame("Concurrency Game", null, "admin").id;
			lifecycleService.joinGame(id, "player2", null);
			lifecycleService.joinGame(id, "player3", null);
			Game started = gameService.startGame(id, "admin", secretWord.id);
			return started.id;
		});

		String turnUser = QuarkusTransaction.requiringNew()
				.call(() -> gameService.getGameForMember(gameId, "admin").currentTurnUserId);

		QuarkusTransaction.requiringNew().run(() -> gameService.completeTurn(gameId, turnUser));

		BadRequestException ex = assertThrows(
				BadRequestException.class,
				() -> QuarkusTransaction.requiringNew().run(() -> gameService.completeTurn(gameId, turnUser)));
		assertEquals("BAD_REQUEST", ex.getCode());
	}
}
