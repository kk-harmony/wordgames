package org.learning.games.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
@TestProfile(DataCleanupServiceDryRunTest.DryRunCleanupTestProfile.class)
class DataCleanupServiceDryRunTest {

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
		});
	}

	@Test
	void dryRunReportsCountsWithoutDeleting() {
		Instant old = Instant.now().minus(20, ChronoUnit.DAYS);
		long waitingGameId = persistOldGame(GameStatus.WAITING, old);
		persistMember(waitingGameId, "dry-run-user");
		persistIdempotency(waitingGameId, "dry-run-key", old);
		persistIdempotency(42L, "dry-run-global", old);

		CleanupResult result = QuarkusTransaction.requiringNew().call(cleanupService::run);

		assertTrue(result.dryRun());
		assertEquals(2, result.idempotencyRecordsDeleted());
		assertEquals(1, result.gamesDeleted());
		assertEquals(1, result.gameMembersDeleted());
		assertEquals(1, result.gameIdempotencyRecordsDeleted());
		assertEquals(2L, countIdempotencyRecords());
		assertNotNullGame(waitingGameId);
	}

	private long persistOldGame(GameStatus status, Instant createdAt) {
		return QuarkusTransaction.requiringNew().call(() -> {
			Game game = new Game();
			game.name = "dry-run-" + status;
			game.adminUserId = "admin-" + status;
			game.status = status;
			game.createdAt = createdAt;
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

	private long countIdempotencyRecords() {
		return QuarkusTransaction.requiringNew().call(() ->
				em.createQuery("SELECT COUNT(i) FROM IdempotencyRecord i", Long.class).getSingleResult());
	}

	private void assertNotNullGame(long gameId) {
		QuarkusTransaction.requiringNew().run(() ->
				org.junit.jupiter.api.Assertions.assertNotNull(em.find(Game.class, gameId)));
	}

	public static class DryRunCleanupTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
		@Override
		public String getConfigProfile() {
			return "test";
		}

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
					"app.cleanup.dry-run", "true",
					"app.cleanup.idempotency.retention-days", "15",
					"app.cleanup.game.waiting-retention-days", "7",
					"app.cleanup.game.finished-retention-days", "30",
					"app.cleanup.batch-size", "500",
					"app.cleanup.max-batches-per-run", "100",
					"app.cleanup.inter-batch-sleep-ms", "0");
		}
	}
}
