package org.learning.games.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.learning.games.domain.model.IdempotentResult;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class IdempotencyServiceConcurrencyTest {

	@Inject
	IdempotencyService idempotencyService;

	@Test
	void parallelStoreWithSameKey_bothReturnEquivalentResult() throws Exception {
		String key = "parallel-key-" + UUID.randomUUID();
		String responseBody = "{\"status\":\"IN_PROGRESS\"}";
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		List<IdempotentResult> results = new CopyOnWriteArrayList<>();
		AtomicInteger errors = new AtomicInteger();

		Runnable worker = () -> {
			ready.countDown();
			try {
				start.await();
				IdempotentResult result = QuarkusTransaction.requiringNew().call(() ->
						idempotencyService.store(
								"admin",
								key,
								IdempotencyOperation.COMPLETE_TURN,
								1L,
								"turn",
								200,
								responseBody));
				results.add(result);
			} catch (Exception ex) {
				errors.incrementAndGet();
			}
		};

		ExecutorService pool = Executors.newFixedThreadPool(2);
		pool.submit(worker);
		pool.submit(worker);
		ready.await();
		start.countDown();
		pool.shutdown();

		assertEquals(true, pool.awaitTermination(30, TimeUnit.SECONDS));
		assertEquals(0, errors.get(), "unexpected errors during parallel idempotency store");
		assertEquals(2, results.size());
		assertEquals(responseBody, results.get(0).responseBody());
		assertEquals(responseBody, results.get(1).responseBody());
		assertEquals(200, results.get(0).status());
		assertEquals(200, results.get(1).status());
	}
}
