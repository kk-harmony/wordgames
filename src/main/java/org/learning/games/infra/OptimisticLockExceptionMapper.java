package org.learning.games.infra;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

	@Override
	public Response toResponse(OptimisticLockException exception) {
		CustomErrorWrapper payload = new CustomErrorWrapper(
				"CONFLICT",
				"Game state was modified by another request. Please retry.",
				null);
		return Response.status(Response.Status.CONFLICT).entity(payload).build();
	}
}
