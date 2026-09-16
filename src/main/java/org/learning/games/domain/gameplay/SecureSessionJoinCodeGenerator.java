package org.learning.games.domain.gameplay;

import java.security.SecureRandom;
import java.util.function.Predicate;

import org.learning.games.domain.exception.BadRequestException;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SecureSessionJoinCodeGenerator implements SessionJoinCodeGenerator {

	private static final int MAX_ATTEMPTS = 32;

	private final SecureRandom random = new SecureRandom();

	@Override
	public String nextCode(Predicate<String> alreadyExists) {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String candidate = randomCode();
			if (!alreadyExists.test(candidate)) {
				return candidate;
			}
		}
		throw new BadRequestException("Unable to allocate a unique session ID");
	}

	private String randomCode() {
		StringBuilder builder = new StringBuilder(SessionJoinCode.LENGTH);
		for (int i = 0; i < SessionJoinCode.LENGTH; i++) {
			int index = random.nextInt(SessionJoinCode.ALPHABET.length());
			builder.append(SessionJoinCode.ALPHABET.charAt(index));
		}
		return builder.toString();
	}
}
