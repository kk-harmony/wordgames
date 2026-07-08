package org.learning.games.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AdvisoryLockServiceTest {

	private static final long TEST_LOCK_KEY = 42424242L;

	@Inject
	AdvisoryLockService advisoryLockService;

	@Test
	void onlyOneSessionCanHoldAdvisoryLock() throws InterruptedException {
		AtomicInteger acquiredCount = new AtomicInteger();
		AtomicBoolean holderHasLock = new AtomicBoolean();
		CountDownLatch holderReady = new CountDownLatch(1);
		CountDownLatch releaseHolder = new CountDownLatch(1);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			executor.submit(() -> QuarkusTransaction.requiringNew().run(() -> {
				if (advisoryLockService.tryLock(TEST_LOCK_KEY)) {
					acquiredCount.incrementAndGet();
					holderHasLock.set(true);
					holderReady.countDown();
					try {
						releaseHolder.await(5, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						advisoryLockService.unlock(TEST_LOCK_KEY);
					}
				}
			}));

			assertTrue(holderReady.await(5, TimeUnit.SECONDS));

			AtomicBoolean contenderAcquired = new AtomicBoolean();
			executor.submit(() -> QuarkusTransaction.requiringNew().run(() ->
					contenderAcquired.set(advisoryLockService.tryLock(TEST_LOCK_KEY)))).get(5, TimeUnit.SECONDS);

			assertTrue(holderHasLock.get());
			assertFalse(contenderAcquired.get());
			assertEquals(1, acquiredCount.get());

			releaseHolder.countDown();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			executor.shutdownNow();
		}
	}
}
