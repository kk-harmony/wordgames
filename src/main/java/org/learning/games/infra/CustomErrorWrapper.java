package org.learning.games.infra;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "Standard API error envelope")
public class CustomErrorWrapper {
	@Schema(description = "Machine-readable error code", example = "BAD_REQUEST")
	public String type;

	@Schema(description = "Human-readable error message")
	public String message;

	@Schema(description = "Field-level validation errors, when applicable")
	public List<FieldError> details;

	public CustomErrorWrapper(String type, String message, List<FieldError> details) {
		this.type = type;
		this.message = message;
		this.details = details;
	}

	@Schema(name = "FieldError")
	public static class FieldError {
		@Schema(description = "Request field path")
		public String field;

		@Schema(description = "Validation message for the field")
		public String error;

		public FieldError(String field, String error) {
			this.field = field;
			this.error = error;
		}
	}
}
