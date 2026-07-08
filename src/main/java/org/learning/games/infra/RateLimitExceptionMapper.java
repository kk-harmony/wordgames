package org.learning.games.infra;

import io.quarkiverse.bucket4j.runtime.RateLimitException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RateLimitExceptionMapper implements ExceptionMapper<RateLimitException> {

	@Override
	public Response toResponse(RateLimitException exception) {
		CustomErrorWrapper payload = new CustomErrorWrapper(
				"RATE_LIMIT_EXCEEDED",
				"Too many requests. Please try again later.",
				null);

		Response.ResponseBuilder builder = Response.status(429).entity(payload);
		long retryAfterSeconds = (exception.getWaitTimeInMilliSeconds() + 999) / 1000;
		if (retryAfterSeconds > 0) {
			builder.header("Retry-After", retryAfterSeconds);
		}
		return builder.build();
	}

}
