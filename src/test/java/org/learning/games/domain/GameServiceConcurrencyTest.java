package org.learning.games.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.DomainException;
import org.learning.games.entity.SecretWord;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

@QuarkusTest
class GameServiceConcurrencyTest {

	@Inject
	GameLifecycleService lifecycleService;

	@Inject
	GameService gameService;

	@Inject
	SecretWordRepository secretWordRepository;

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ParallelTurnComplete {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setupGameInProgress() {
			QuarkusTransaction.requiringNew().run(() -> {
				SecretWord secretWord = new SecretWord();
				secretWord.authentic = "lime";
				secretWord.imposed = "lemon";
				secretWordRepository.persist(secretWord);

				gameId = lifecycleService.createGame("Parallel Turn", null, "admin").id;
				lifecycleService.joinGame(gameId, "player2", null);
				lifecycleService.joinGame(gameId, "player3", null);
				gameService.startGame(gameId, "admin", secretWord.id);
			});
		}

		@Test
		@Order(2)
		@TestSecurity(user = "admin")
		void parallelCompleteTurn_oneSucceedsOneOptimisticLock() throws Exception {
			CountDownLatch ready = new CountDownLatch(2);
			CountDownLatch start = new CountDownLatch(1);
			AtomicInteger successes = new AtomicInteger();
			AtomicInteger conflicts = new AtomicInteger();
			AtomicInteger otherFailures = new AtomicInteger();

			Runnable worker = () -> {
				ready.countDown();
				try {
					start.await();
					QuarkusTransaction.requiringNew().run(() -> gameService.completeTurn(gameId, "admin"));
					successes.incrementAndGet();
				} catch (Exception ex) {
					if (isOptimisticLock(ex) || isConcurrentTurnConflict(ex)) {
						conflicts.incrementAndGet();
					} else {
						otherFailures.incrementAndGet();
					}
				}
			};

			ExecutorService pool = Executors.newFixedThreadPool(2);
			pool.submit(worker);
			pool.submit(worker);
			ready.await();
			start.countDown();
			pool.shutdown();

			assertEquals(true, pool.awaitTermination(30, TimeUnit.SECONDS));
			assertEquals(0, otherFailures.get(), "unexpected errors during parallel turn completion");
			assertEquals(1, successes.get());
			assertEquals(1, conflicts.get());
		}
	}

	private static boolean isConcurrentTurnConflict(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof BadRequestException badRequest
					&& "Turn already completed".equals(badRequest.getMessage())) {
				return true;
			}
			if (current instanceof DomainException domain
					&& "BAD_REQUEST".equals(domain.getCode())
					&& "Turn already completed".equals(domain.getMessage())) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private static boolean isOptimisticLock(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof OptimisticLockException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
