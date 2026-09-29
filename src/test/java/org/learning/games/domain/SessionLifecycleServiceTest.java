package org.learning.games.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.ForbiddenException;
import org.learning.games.domain.gameplay.GameResolutionService;
import org.learning.games.domain.gameplay.SessionJoinCode;
import org.learning.games.domain.gameplay.SessionRules;
import org.learning.games.domain.model.GameOutcome;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.domain.model.MemberRole;
import org.learning.games.domain.model.SessionStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameSession;
import org.learning.games.entity.SecretWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class SessionLifecycleServiceTest {

	@Inject
	SessionLifecycleService sessions;

	@Inject
	GameResolutionService resolutionService;

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
	void createSessionAddsCreatorAsAdminWithJoinCode() {
		GameSession session = QuarkusTransaction.requiringNew()
				.call(() -> sessions.createSession("Lobby", "Host", "admin"));

		assertNotNull(session.id);
		assertEquals(SessionJoinCode.LENGTH, session.joinCode.length());
		assertEquals(session.joinCode, SessionJoinCode.parse(session.joinCode).value());
		assertEquals("admin", session.adminUserId);
		assertEquals(SessionStatus.OPEN, session.status);
		assertEquals(0, session.gamesStartedCount);
		assertEquals(1, session.members.size());
		assertEquals(MemberRole.ADMIN, session.members.get(0).role);
		assertEquals("Host", session.members.get(0).displayName);
	}

	@Test
	void joinSessionBetweenGames() {
		String code = QuarkusTransaction.requiringNew()
				.call(() -> sessions.createSession("Lobby", null, "admin").joinCode);

		GameSession joined = QuarkusTransaction.requiringNew()
				.call(() -> sessions.joinSession(code, "player2", "P2"));

		assertEquals(2, joined.members.size());
		assertTrue(joined.members.stream().anyMatch(m -> "player2".equals(m.userId)));
	}

	@Test
	void rejectDuplicateJoin() {
		String code = QuarkusTransaction.requiringNew()
				.call(() -> sessions.createSession("Lobby", null, "admin").joinCode);
		QuarkusTransaction.requiringNew().run(() -> sessions.joinSession(code, "player2", null));

		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.joinSession(code, "player2", null)));
	}

	@Test
	void rejectJoinWhileGameInProgress() {
		String code = createSessionWithThreePlayers();
		long secretWordId = persistSecretWord();
		QuarkusTransaction.requiringNew()
				.run(() -> sessions.startGame(code, "admin", secretWordId));

		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.joinSession(code, "late", null)));
	}

	@Test
	void leaveSessionBetweenGames() {
		String code = createSessionWithThreePlayers();
		QuarkusTransaction.requiringNew().run(() -> sessions.leaveSession(code, "player2"));

		GameSession session = QuarkusTransaction.requiringNew()
				.call(() -> sessions.getSessionForMember(code, "admin"));
		assertEquals(2, session.members.size());
	}

	@Test
	void adminCannotLeave() {
		String code = QuarkusTransaction.requiringNew()
				.call(() -> sessions.createSession("Lobby", null, "admin").joinCode);

		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew().run(() -> sessions.leaveSession(code, "admin")));
	}

	@Test
	void startGameSnapshotsRosterAndIncrementsCount() {
		String code = createSessionWithThreePlayers();
		long secretWordId = persistSecretWord();

		Game game = QuarkusTransaction.requiringNew()
				.call(() -> sessions.startGame(code, "admin", secretWordId));

		assertEquals(GameStatus.IN_PROGRESS, game.status);
		assertEquals(3, game.members.size());
		assertNotNull(game.impostorUserId);

		GameSession session = QuarkusTransaction.requiringNew()
				.call(() -> sessions.getSessionForMember(code, "admin"));
		assertEquals(1, session.gamesStartedCount);
		assertEquals(game.id, session.currentGameId);
	}

	@Test
	void startGameRequiresThreePlayersAndAdmin() {
		String code = QuarkusTransaction.requiringNew()
				.call(() -> sessions.createSession("Lobby", null, "admin").joinCode);
		QuarkusTransaction.requiringNew().run(() -> sessions.joinSession(code, "player2", null));
		long secretWordId = persistSecretWord();

		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.startGame(code, "admin", secretWordId)));

		QuarkusTransaction.requiringNew().run(() -> sessions.joinSession(code, "player3", null));

		assertThrows(ForbiddenException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.startGame(code, "player2", secretWordId)));
	}

	@Test
	void lateJoinerIncludedInNextGameOnly() {
		String code = createSessionWithThreePlayers();
		long secretWordId = persistSecretWord();
		Game first = QuarkusTransaction.requiringNew()
				.call(() -> sessions.startGame(code, "admin", secretWordId));
		assertEquals(3, first.members.size());

		finishCurrentGame(first.id);

		QuarkusTransaction.requiringNew().run(() -> sessions.joinSession(code, "late", "Late"));
		long secondWordId = persistSecretWord("pear", "peach");
		Game second = QuarkusTransaction.requiringNew()
				.call(() -> sessions.startGame(code, "admin", secondWordId));

		assertEquals(4, second.members.size());
		assertTrue(second.members.stream().anyMatch(m -> "late".equals(m.userId)));
	}

	@Test
	void finishClearsCurrentGameId() {
		String code = createSessionWithThreePlayers();
		long secretWordId = persistSecretWord();
		Game game = QuarkusTransaction.requiringNew()
				.call(() -> sessions.startGame(code, "admin", secretWordId));

		finishCurrentGame(game.id);

		GameSession session = QuarkusTransaction.requiringNew()
				.call(() -> sessions.getSessionForMember(code, "admin"));
		assertNull(session.currentGameId);
	}

	@Test
	void rejectTwentyFirstGame() {
		String code = createSessionWithThreePlayers();
		QuarkusTransaction.requiringNew().run(() -> {
			GameSession session = em.createQuery(
					"SELECT s FROM GameSession s WHERE s.joinCode = :code", GameSession.class)
					.setParameter("code", code)
					.getSingleResult();
			session.gamesStartedCount = SessionRules.MAX_GAMES;
		});
		long secretWordId = persistSecretWord();

		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.startGame(code, "admin", secretWordId)));
	}

	@Test
	void rejectInvalidJoinCodeFormat() {
		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.joinSession("bad", "player2", null)));
	}

	@Test
	void kickMemberBetweenGames() {
		String code = createSessionWithThreePlayers();
		GameSession kicked = QuarkusTransaction.requiringNew()
				.call(() -> sessions.kickMember(code, "admin", "player2"));
		assertEquals(2, kicked.members.size());
		assertTrue(kicked.members.stream().noneMatch(m -> "player2".equals(m.userId)));
	}

	@Test
	void rejectKickOfAdmin() {
		String code = createSessionWithThreePlayers();
		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.kickMember(code, "admin", "admin")));
	}

	@Test
	void rejectKickWhileGameInProgress() {
		String code = createSessionWithThreePlayers();
		long secretWordId = persistSecretWord();
		QuarkusTransaction.requiringNew()
				.run(() -> sessions.startGame(code, "admin", secretWordId));

		assertThrows(BadRequestException.class,
				() -> QuarkusTransaction.requiringNew()
						.run(() -> sessions.kickMember(code, "admin", "player2")));
	}

	private String createSessionWithThreePlayers() {
		String code = QuarkusTransaction.requiringNew()
				.call(() -> sessions.createSession("Lobby", null, "admin").joinCode);
		QuarkusTransaction.requiringNew().run(() -> sessions.joinSession(code, "player2", null));
		QuarkusTransaction.requiringNew().run(() -> sessions.joinSession(code, "player3", null));
		return code;
	}

	private long persistSecretWord() {
		return persistSecretWord("apple", "orange");
	}

	private long persistSecretWord(String authentic, String imposed) {
		return QuarkusTransaction.requiringNew().call(() -> {
			SecretWord secretWord = new SecretWord();
			secretWord.authentic = authentic;
			secretWord.imposed = imposed;
			em.persist(secretWord);
			return secretWord.id;
		});
	}

	private void finishCurrentGame(long gameId) {
		QuarkusTransaction.requiringNew().run(() -> {
			Game game = em.find(Game.class, gameId);
			resolutionService.finishGame(game, GameOutcome.IMPOSTOR_IDENTIFIED);
		});
	}
}
