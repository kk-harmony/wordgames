package org.learning.games.domain.exception;

public class ForbiddenException extends DomainException {

	public ForbiddenException(String message) {
		super("FORBIDDEN", message);
	}
}
