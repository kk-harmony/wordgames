package org.learning.games.resource.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.learning.games.domain.GameService;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;

import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Completes turns in whatever order {@code currentTurnUserId} dictates so HTTP
 * flow tests stay valid after random first-turn selection.
 */
final class TurnCompletionSupport {

	private TurnCompletionSupport() {
	}

	static void completeAllTurns(GameService gameService, long gameId, String viewerUserId) {
		for (int i = 0; i < 12; i++) {
			Game game = QuarkusTransaction.requiringNew()
					.call(() -> gameService.getGameForMember(gameId, viewerUserId));
			if (game.status != GameStatus.IN_PROGRESS) {
				return;
			}
			assertNotNull(game.currentTurnUserId, "expected a current turn while IN_PROGRESS");
			String turnUser = game.currentTurnUserId;
			QuarkusTransaction.requiringNew().run(() -> gameService.completeTurn(gameId, turnUser));
		}
		Game finalGame = QuarkusTransaction.requiringNew()
				.call(() -> gameService.getGameForMember(gameId, viewerUserId));
		assertEquals(GameStatus.VOTING, finalGame.status, "expected voting after all turns");
	}

	/** Advance the game until {@code userId} has the turn (or voting begins). */
	static void advanceUntilTurn(GameService gameService, long gameId, String userId) {
		for (int i = 0; i < 12; i++) {
			Game game = QuarkusTransaction.requiringNew()
					.call(() -> gameService.getGameForMember(gameId, userId));
			if (game.status != GameStatus.IN_PROGRESS) {
				return;
			}
			if (userId.equals(game.currentTurnUserId)) {
				return;
			}
			String turnUser = game.currentTurnUserId;
			QuarkusTransaction.requiringNew().run(() -> gameService.completeTurn(gameId, turnUser));
		}
	}
}
