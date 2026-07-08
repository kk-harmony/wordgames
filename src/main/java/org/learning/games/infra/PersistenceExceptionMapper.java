package org.learning.games.infra;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

	@Override
	public Response toResponse(PersistenceException exception) {
		CustomErrorWrapper payload = new CustomErrorWrapper(
				"PERSISTENCE_ERROR",
				"A database error occurred. Please try again.",
				null);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(payload).build();
	}
}
