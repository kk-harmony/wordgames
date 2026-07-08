package org.learning.games.infra;

import org.learning.games.domain.exception.DomainException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

	@Override
	public Response toResponse(DomainException exception) {
		CustomErrorWrapper payload = new CustomErrorWrapper(
				exception.getCode(),
				exception.getMessage(),
				null);

		int status = switch (exception.getCode()) {
			case "NOT_FOUND" -> Response.Status.NOT_FOUND.getStatusCode();
			case "FORBIDDEN" -> Response.Status.FORBIDDEN.getStatusCode();
			case "BAD_REQUEST" -> Response.Status.BAD_REQUEST.getStatusCode();
			case "CONFLICT", "IDEMPOTENCY_KEY_REUSED" -> Response.Status.CONFLICT.getStatusCode();
			default -> Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
		};

		return Response.status(status).entity(payload).build();
	}
}
