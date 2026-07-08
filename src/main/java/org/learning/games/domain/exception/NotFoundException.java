package org.learning.games.domain.exception;

public class NotFoundException extends DomainException {

	public NotFoundException(String message) {
		super("NOT_FOUND", message);
	}
}
