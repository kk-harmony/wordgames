package org.learning.games.domain.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.learning.games.domain.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class SessionJoinCodeGeneratorTest {

	@Test
	void generatesValidUniqueLookingCodes() {
		SessionJoinCodeGenerator generator = new SecureSessionJoinCodeGenerator();
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 50; i++) {
			String code = generator.nextCode(existing -> false);
			assertEquals(SessionJoinCode.LENGTH, code.length());
			assertEquals(code, SessionJoinCode.parse(code).value());
			codes.add(code);
		}
		assertEquals(50, codes.size());
	}

	@Test
	void retriesWhenCodeAlreadyExists() {
		AtomicInteger calls = new AtomicInteger();
		Predicate<String> exists = candidate -> calls.getAndIncrement() < 2;

		SessionJoinCodeGenerator generator = new SecureSessionJoinCodeGenerator();
		String code = generator.nextCode(exists);
		assertEquals(SessionJoinCode.LENGTH, code.length());
		assertTrue(calls.get() >= 3);
	}

	@Test
	void failsAfterTooManyCollisions() {
		SessionJoinCodeGenerator generator = new SecureSessionJoinCodeGenerator();
		assertThrows(BadRequestException.class, () -> generator.nextCode(existing -> true));
	}
}
