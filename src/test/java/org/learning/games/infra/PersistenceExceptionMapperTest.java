package org.learning.games.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class PersistenceExceptionMapperTest {

	private final PersistenceExceptionMapper mapper = new PersistenceExceptionMapper();

	@Test
	void mapsTo500WithErrorEnvelope() {
		Response response = mapper.toResponse(new PersistenceException("database failure"));

		assertEquals(500, response.getStatus());
		CustomErrorWrapper body = (CustomErrorWrapper) response.getEntity();
		assertEquals("PERSISTENCE_ERROR", body.type);
		assertEquals("A database error occurred. Please try again.", body.message);
	}
}
