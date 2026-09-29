package org.learning.games.domain.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.learning.games.domain.GameLifecycleService;
import org.learning.games.domain.GameService;
import org.learning.games.domain.SecretWordRepository;
import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.entity.Game;
import org.learning.games.entity.SecretWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class GameTurnOrderTest {

	@Inject
	GameLifecycleService lifecycleService;

	@Inject
	GameService gameService;

	@Inject
	SecretWordRepository secretWordRepository;

	@Inject
	EntityManager em;

	@BeforeEach
	void resetData() {
		QuarkusTransaction.requiringNew().run(() -> {
			em.createQuery("DELETE FROM IdempotencyRecord").executeUpdate();
			em.createNativeQuery("UPDATE gamesession SET current_game_id = NULL").executeUpdate();
			em.createQuery("DELETE FROM GameMember").executeUpdate();
			em.createQuery("DELETE FROM Game").executeUpdate();
			em.createQuery("DELETE FROM SessionMember").executeUpdate();
			em.createQuery("DELETE FROM GameSession").executeUpdate();
			em.createQuery("DELETE FROM SecretWord").executeUpdate();
		});
	}

	@Test
	void startPicksAMemberAtRandom_andOnlyThatPlayerCanComplete() {
		Game started = startThreePlayerGame();
		String first = started.currentTurnUserId;
		assertTrue(Set.of("admin", "player2", "player3").contains(first));

		String other = "admin".equals(first) ? "player2" : "admin";
		assertThrows(
				BadRequestException.class,
				() -> QuarkusTransaction.requiringNew().run(() -> gameService.completeTurn(started.id, other)));

		Game after = QuarkusTransaction.requiringNew()
				.call(() -> gameService.completeTurn(started.id, first));
		assertNotEquals(first, after.currentTurnUserId);
		assertTrue(Set.of("admin", "player2", "player3").contains(after.currentTurnUserId));
	}

	@Test
	void firstTurnIsNotAlwaysAdminAcrossManyGames() {
		Set<String> starters = new HashSet<>();
		for (int i = 0; i < 40; i++) {
			starters.add(startThreePlayerGame().currentTurnUserId);
			if (starters.size() > 1) {
				break;
			}
		}
		assertTrue(starters.size() > 1, "expected more than one distinct first-turn player across starts");
		assertTrue(starters.stream().anyMatch(id -> !"admin".equals(id)));
	}

	private Game startThreePlayerGame() {
		return QuarkusTransaction.requiringNew().call(() -> {
			SecretWord secretWord = new SecretWord();
			secretWord.authentic = "apple";
			secretWord.imposed = "orange";
			secretWordRepository.persist(secretWord);

			long gameId = lifecycleService.createGame("Turn Order", null, "admin").id;
			lifecycleService.joinGame(gameId, "player2", null);
			lifecycleService.joinGame(gameId, "player3", null);
			Game started = gameService.startGame(gameId, "admin", secretWord.id);
			assertEquals(3, started.members.size());
			return started;
		});
	}
}
