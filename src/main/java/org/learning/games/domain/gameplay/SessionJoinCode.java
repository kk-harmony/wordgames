package org.learning.games.domain.gameplay;

import org.learning.games.domain.exception.BadRequestException;

/**
 * Public session join code: fixed-length, uppercase letters+digits without ambiguous glyphs.
 */
public final class SessionJoinCode {

	/** Ambiguous glyphs excluded: 0/O, 1/I/L. */
	public static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	public static final int LENGTH = 5;

	private final String value;

	private SessionJoinCode(String value) {
		this.value = value;
	}

	public static SessionJoinCode parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new BadRequestException("Session ID is required");
		}
		String normalized = raw.trim().toUpperCase();
		if (normalized.length() != LENGTH) {
			throw new BadRequestException("Session ID must be " + LENGTH + " characters");
		}
		for (int i = 0; i < normalized.length(); i++) {
			if (ALPHABET.indexOf(normalized.charAt(i)) < 0) {
				throw new BadRequestException("Session ID contains invalid characters");
			}
		}
		return new SessionJoinCode(normalized);
	}

	public String value() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SessionJoinCode code && value.equals(code.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
