package org.learning.games.infra;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

	@Override
	public Response toResponse(ConstraintViolationException exception) {
		var errors = exception.getConstraintViolations().stream()
				.map(violation -> new CustomErrorWrapper.FieldError(
						violation.getPropertyPath().toString(),
						violation.getMessage()))
				.collect(Collectors.toList());

		CustomErrorWrapper payload = new CustomErrorWrapper(
				"VALIDATION_ERROR",
				"The provided data is invalid.",
				errors);

		return Response.status(Response.Status.BAD_REQUEST).entity(payload).build();
	}
}
