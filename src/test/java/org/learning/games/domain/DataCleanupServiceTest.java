package org.learning.games.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.learning.games.domain.model.CleanupResult;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.domain.model.MemberRole;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;
import org.learning.games.entity.IdempotencyRecord;
import org.learning.games.entity.SecretWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
@TestProfile(DataCleanupServiceTest.CleanupTestProfile.class)
class DataCleanupServiceTest {

	@Inject
	DataCleanupService cleanupService;

	@Inject
	EntityManager em;

	@BeforeEach
	void resetData() {
		QuarkusTransaction.requiringNew().run(() -> {
			em.createQuery("DELETE FROM IdempotencyRecord").executeUpdate();
			em.createQuery("DELETE FROM GameMember").executeUpdate();
			em.createQuery("DELETE FROM Game").executeUpdate();
			em.createQuery("DELETE FROM SecretWord").executeUpdate();
		});
	}

	@Test
	void deletesOldIdempotencyRecordsAndEligibleGames() {
		Instant oldWaiting = Instant.now().minus(10, ChronoUnit.DAYS);
		Instant oldFinished = Instant.now().minus(35, ChronoUnit.DAYS);
		Instant oldActive = Instant.now().minus(20, ChronoUnit.DAYS);
		SecretWord secretWord = persistSecretWord();
		long waitingGameId = persistOldGame(GameStatus.WAITING, oldWaiting, null);
		long finishedGameId = persistOldGame(GameStatus.FINISHED, oldFinished, secretWord.id);
		long inProgressGameId = persistOldGame(GameStatus.IN_PROGRESS, oldActive, secretWord.id);
		long votingGameId = persistOldGame(GameStatus.VOTING, oldActive, secretWord.id);
		persistMember(waitingGameId, "waiting-user");
		persistMember(finishedGameId, "finished-user");
		persistMember(inProgressGameId, "progress-user");
		persistMember(votingGameId, "voting-user");
		persistIdempotency(waitingGameId, "waiting-key", oldWaiting);
		persistIdempotency(finishedGameId, "finished-key", oldFinished);
		persistIdempotency(inProgressGameId, "progress-key", oldActive);
		persistGlobalIdempotency("global-old-key", 99L, oldActive);

		CleanupResult result = QuarkusTransaction.requiringNew().call(cleanupService::run);

		assertFalse(result.dryRun());
		assertEquals(3, result.idempotencyRecordsDeleted());
		assertEquals(2, result.gamesDeleted());
		assertEquals(2, result.gameMembersDeleted());
		assertEquals(1, result.gameIdempotencyRecordsDeleted());

		assertGameExists(inProgressGameId);
		assertGameExists(votingGameId);
		assertGameMissing(waitingGameId);
		assertGameMissing(finishedGameId);
		assertSecretWordExists(secretWord.id);
		assertEquals(0L, countIdempotencyRecords());
	}

	@Test
	void deletesGameRelatedRowsInFkSafeOrder() {
		Instant old = Instant.now().minus(20, ChronoUnit.DAYS);
		long gameId = persistOldGame(GameStatus.WAITING, old, null);
		persistMember(gameId, "member-a");
		persistMember(gameId, "member-b");
		persistIdempotency(gameId, "game-idem-1", old);
		persistIdempotency(gameId, "game-idem-2", old);

		QuarkusTransaction.requiringNew().run(cleanupService::run);

		assertGameMissing(gameId);
		assertEquals(0L, countRows(GameMember.class));
		assertEquals(0L, countRows(IdempotencyRecord.class));
	}

	private SecretWord persistSecretWord() {
		return QuarkusTransaction.requiringNew().call(() -> {
			SecretWord secretWord = new SecretWord();
			secretWord.authentic = "apple";
			secretWord.imposed = "apricot";
			em.persist(secretWord);
			return secretWord;
		});
	}

	private long persistOldGame(GameStatus status, Instant createdAt, Long secretWordId) {
		return QuarkusTransaction.requiringNew().call(() -> {
			Game game = new Game();
			game.name = "cleanup-test-" + status;
			game.adminUserId = "admin-" + status;
			game.status = status;
			game.createdAt = createdAt;
			if (secretWordId != null) {
				game.secretWord = em.getReference(SecretWord.class, secretWordId);
			}
			em.persist(game);
			return game.id;
		});
	}

	private void persistMember(long gameId, String userId) {
		QuarkusTransaction.requiringNew().run(() -> {
			GameMember member = new GameMember();
			member.game = em.getReference(Game.class, gameId);
			member.userId = userId;
			member.role = MemberRole.MEMBER;
			em.persist(member);
		});
	}

	private void persistIdempotency(long gameId, String key, Instant createdAt) {
		QuarkusTransaction.requiringNew().run(() -> {
			IdempotencyRecord record = new IdempotencyRecord();
			record.idempotencyKey = key;
			record.userId = "user-" + key;
			record.operation = "TEST";
			record.gameId = gameId;
			record.requestHash = "hash";
			record.responseStatus = 200;
			record.responseBody = "{}";
			record.createdAt = createdAt;
			em.persist(record);
		});
	}

	private void persistGlobalIdempotency(String key, long gameId, Instant createdAt) {
		persistIdempotency(gameId, key, createdAt);
	}

	private void assertGameExists(long gameId) {
		QuarkusTransaction.requiringNew().run(() ->
				assertNotNull(em.find(Game.class, gameId)));
	}

	private void assertGameMissing(long gameId) {
		QuarkusTransaction.requiringNew().run(() ->
				assertEquals(null, em.find(Game.class, gameId)));
	}

	private void assertSecretWordExists(long secretWordId) {
		QuarkusTransaction.requiringNew().run(() ->
				assertNotNull(em.find(SecretWord.class, secretWordId)));
	}

	private long countIdempotencyRecords() {
		return QuarkusTransaction.requiringNew().call(() ->
				em.createQuery("SELECT COUNT(i) FROM IdempotencyRecord i", Long.class).getSingleResult());
	}

	private long countRows(Class<?> entityType) {
		return QuarkusTransaction.requiringNew().call(() ->
				em.createQuery("SELECT COUNT(e) FROM " + entityType.getSimpleName() + " e", Long.class)
						.getSingleResult());
	}

	public static class CleanupTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
		@Override
		public String getConfigProfile() {
			return "test";
		}

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
					"app.cleanup.dry-run", "false",
					"app.cleanup.idempotency.retention-days", "15",
					"app.cleanup.game.waiting-retention-days", "7",
					"app.cleanup.game.finished-retention-days", "30",
					"app.cleanup.batch-size", "500",
					"app.cleanup.max-batches-per-run", "100",
					"app.cleanup.inter-batch-sleep-ms", "0");
		}
	}
}
