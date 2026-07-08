package org.learning.games.domain.exception;

public class IdempotencyWriteConflictException extends RuntimeException {

	public IdempotencyWriteConflictException(Throwable cause) {
		super(cause);
	}
}
