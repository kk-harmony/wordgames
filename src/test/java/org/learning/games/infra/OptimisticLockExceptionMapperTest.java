package org.learning.games.infra;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptimisticLockExceptionMapperTest {

	private final OptimisticLockExceptionMapper mapper = new OptimisticLockExceptionMapper();

	@Test
	void mapsTo409Conflict() {
		Response response = mapper.toResponse(new OptimisticLockException());
		assertEquals(409, response.getStatus());
	}
}
